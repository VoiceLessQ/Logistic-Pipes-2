package com.Morph.logisticspipes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import com.Morph.logisticspipes.client.pipe.LPPipeModel;
import com.Morph.logisticspipes.client.pipe.LPPipeModel.Quad;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlock;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;
import com.Morph.logisticspipes.pipes.basic.PipeType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;

import static net.minecraft.core.Direction.*;

/**
 * LP1-accurate pipe renderer using PipeModel_moved.obj.
 *
 * Corner selection matches LogisticsNewRenderPipe.java (LP1):
 *   count=0  → Corner_M   (full corner, no arm at this corner)
 *   count=1  → Spacer[N]  (one arm; uses spacer number from LP1 PipeTurnCorner enum)
 *   count=2  → Corner_I   (two arms; single-bevel piece for the corner)
 *   count=3  → Corner_I3  (all three arms; full inner corner)
 *
 * Type indicator uses LP1's LPUVScale(12/16, 12/16): UV mapped to [0.125, 0.875]
 * so the icon texture is centered without stretching to the full face.
 */
public class LPPipeRenderer implements BlockEntityRenderer<LogisticsPipeBlockEntity> {

    private static final ResourceLocation PIPE_TEX =
            ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/pipe/pipemodel.png");

    /**
     * Per-type face-plate <em>background</em> — the LP1 {@code new_texture/<type>.png}
     * equivalent. Coloured per type (yellow request, green provider, etc.).
     */
    private static final Map<PipeType, ResourceLocation> PLATE_TEX = new EnumMap<>(Map.of(
            PipeType.BASIC,    ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/block/pipe_basic.png"),
            PipeType.PROVIDER, ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/block/pipe_provider.png"),
            PipeType.REQUEST,  ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/block/pipe_request.png"),
            PipeType.SUPPLIER, ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/block/pipe_supplier.png"),
            PipeType.CHASSIS,  ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/block/chassis_mk1.png")
    ));

    /**
     * Per-type face-plate <em>overlay</em> — the LP1 {@code overlay_gen/<type>/un-powered-pipe.png}.
     * Transparent-background grid pattern with black/coloured lines that go ON TOP of
     * {@link #PLATE_TEX}, giving the iconic LP1 framed-face look. Verified RGBA with
     * alpha values {0, 255} so the plate colour shows through the transparent cells.
     */
    private static final Map<PipeType, ResourceLocation> INDICATOR_TEX = new EnumMap<>(Map.of(
            PipeType.BASIC,    ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/pipe/indicator_basic.png"),
            PipeType.PROVIDER, ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/pipe/indicator_provider.png"),
            PipeType.REQUEST,  ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/pipe/indicator_request.png"),
            PipeType.SUPPLIER, ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/pipe/indicator_supplier.png"),
            PipeType.CHASSIS,  ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/pipe/indicator_chassis.png")
    ));

    private static volatile LPPipeModel model = null;

    public LPPipeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(LogisticsPipeBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        LPPipeModel m = getModel();
        if (m == null) return;

        BlockState state = be.getBlockState();
        PipeType type = state.getValue(LogisticsPipeBlock.PIPE_TYPE);
        boolean[] conn = new boolean[6];
        for (Direction dir : Direction.values())
            conn[dir.ordinal()] = state.getValue(LogisticsPipeBlock.CONNECTED.get(dir));

        Matrix4f mat = pose.last().pose();
        PoseStack.Pose entry = pose.last();
        VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(PIPE_TEX));

        // --- Edges (12): skip edges that touch any connected direction ---
        for (int i = 0; i < EDGE_NAMES.length; i++) {
            if (!conn[EDGE_DIRS[i][0].ordinal()] && !conn[EDGE_DIRS[i][1].ordinal()])
                render(m.get(EDGE_NAMES[i]), body, mat, entry, light, overlay);
        }

        // --- Corners (8): LP1-accurate M / Spacer / Corner_I / Corner_I3 selection ---
        for (CornerDef c : CORNERS) {
            int count = 0;
            for (Direction d : c.dirs) if (conn[d.ordinal()]) count++;

            if (count == 0) {
                render(m.get("Corner_M_" + c.name), body, mat, entry, light, overlay);
            } else if (count == 1) {
                for (Direction d : c.dirs) {
                    if (conn[d.ordinal()]) {
                        render(m.get("Spacer" + c.spacerFor(d)), body, mat, entry, light, overlay);
                        break;
                    }
                }
            } else if (count == 2) {
                render(m.get("Corner_I_" + c.name), body, mat, entry, light, overlay);
            } else {
                render(m.get("Corner_I3_" + c.name), body, mat, entry, light, overlay);
            }
        }

        // --- Sides: render arm body for each connected direction ---
        for (Direction dir : Direction.values()) {
            if (conn[dir.ordinal()])
                render(m.get("Side_" + dirChar(dir)), body, mat, entry, light, overlay);
        }

        // --- Supports: rendered for purely straight pipes (LP1: only when 2 opposite connections, no branches) ---
        // DOWN+UP straight
        if (conn[DOWN.ordinal()] && conn[UP.ordinal()]
                && !conn[NORTH.ordinal()] && !conn[SOUTH.ordinal()] && !conn[EAST.ordinal()] && !conn[WEST.ordinal()]) {
            for (String g : new String[]{ "Support_E_S", "Support_W_S", "Support_N_S", "Support_S_S" })
                render(m.get(g), body, mat, entry, light, overlay);
        }
        // NORTH+SOUTH straight
        if (conn[NORTH.ordinal()] && conn[SOUTH.ordinal()]
                && !conn[DOWN.ordinal()] && !conn[UP.ordinal()] && !conn[EAST.ordinal()] && !conn[WEST.ordinal()]) {
            for (String g : new String[]{ "Support_E_U", "Support_W_U", "Support_U_S", "Support_D_S" })
                render(m.get(g), body, mat, entry, light, overlay);
        }
        // EAST+WEST straight
        if (conn[EAST.ordinal()] && conn[WEST.ordinal()]
                && !conn[DOWN.ordinal()] && !conn[UP.ordinal()] && !conn[NORTH.ordinal()] && !conn[SOUTH.ordinal()]) {
            for (String g : new String[]{ "Support_U_U", "Support_D_U", "Support_N_U", "Support_S_U" })
                render(m.get(g), body, mat, entry, light, overlay);
        }

        // --- Mounts: bracket pieces on faces adjacent to solid blocks (LP1 logic) ---
        {
            List<MountDef> cands = new ArrayList<>(Arrays.asList(ALL_MOUNTS));
            cands.removeIf(md -> conn[md.dir().ordinal()] || conn[md.side().ordinal()]);
            boolean[] solid = new boolean[6];
            BlockPos bePos = be.getBlockPos();
            for (Direction dir : Direction.values()) {
                if (!conn[dir.ordinal()]) {
                    BlockPos adj = bePos.relative(dir);
                    BlockState adj2 = be.getLevel().getBlockState(adj);
                    solid[dir.ordinal()] = adj2.isFaceSturdy(be.getLevel(), adj, dir.getOpposite());
                }
            }
            cands.removeIf(md -> !solid[md.dir().ordinal()]);
            if (!cands.isEmpty()) {
                if (solid[DOWN.ordinal()]) {
                    mountFindOpponent(cands, DOWN);
                } else if (solid[UP.ordinal()]) {
                    mountFindOpponent(cands, UP);
                } else {
                    cands.removeIf(md -> md.dir() == DOWN || md.dir() == UP);
                    if (cands.size() > 2) mountRemoveIfHasOpponent(cands);
                    if (cands.size() > 2) mountRemoveIfHasConnected(cands);
                    if (cands.size() > 2 && !cands.isEmpty()) mountFindOpponent(cands, cands.get(0).dir());
                }
                for (MountDef md : cands)
                    render(m.get("Mount_" + dirChar(md.dir()) + "_" + dirChar(md.side())), body, mat, entry, light, overlay);
            }
        }

        // --- Type face-plate on unconnected faces (LP1's two-layer plate) ---
        // LPUVScale(12/16, 12/16): center(uv) = 0.125 + uv * 0.75  (matches LP1).
        //
        // Vanilla's BufferSource keeps ONE active builder at a time — getBuffer(B)
        // ends builder A. So all plates render with PLATE_TEX first, then we switch
        // to INDICATOR_TEX for the overlay pass. Interleaving the two per-direction
        // crashes with "Not building!" on the next addVertex.
        ResourceLocation plateTex = PLATE_TEX.getOrDefault(type, PLATE_TEX.get(PipeType.BASIC));
        ResourceLocation indTex   = INDICATOR_TEX.getOrDefault(type, INDICATOR_TEX.get(PipeType.BASIC));

        // LAYER 1 — coloured plate (yellow request, green provider, …)
        VertexConsumer plateBuf = buffers.getBuffer(RenderType.entityCutoutNoCull(plateTex));
        for (Direction dir : Direction.values()) {
            if (!conn[dir.ordinal()])
                renderCentered(m.get("Texture_Plate_" + dirChar(dir)), plateBuf, mat, entry, light, overlay);
        }

        // LAYER 2 — black grid lines on top (transparent cells let the plate show through)
        VertexConsumer indBuf = buffers.getBuffer(RenderType.entityCutoutNoCull(indTex));
        for (Direction dir : Direction.values()) {
            if (!conn[dir.ordinal()])
                renderCentered(m.get("Texture_Plate_" + dirChar(dir)), indBuf, mat, entry, light, overlay);
        }
    }

    // -------------------------------------------------------------------------
    // Render helpers
    // -------------------------------------------------------------------------

    private static void render(List<Quad> quads, VertexConsumer buf,
                                Matrix4f mat, PoseStack.Pose entry, int light, int overlay) {
        for (Quad q : quads)
            emitQuad(buf, mat, entry,
                    q.x0(), q.y0(), q.z0(), q.u0(), q.v0(),
                    q.x1(), q.y1(), q.z1(), q.u1(), q.v1(),
                    q.x2(), q.y2(), q.z2(), q.u2(), q.v2(),
                    q.x3(), q.y3(), q.z3(), q.u3(), q.v3(),
                    q.nx(), q.ny(), q.nz(), light, overlay);
    }

    // Render with LP1's LPUVScale(12/16,12/16): UV mapped to [0.125, 0.875] + normal offset
    private static void renderCentered(List<Quad> quads, VertexConsumer buf,
                                        Matrix4f mat, PoseStack.Pose entry, int light, int overlay) {
        for (Quad q : quads) {
            float dx = q.nx() * 0.002f, dy = q.ny() * 0.002f, dz = q.nz() * 0.002f;
            emitQuad(buf, mat, entry,
                    q.x0()+dx, q.y0()+dy, q.z0()+dz, center(q.u0()), center(q.v0()),
                    q.x1()+dx, q.y1()+dy, q.z1()+dz, center(q.u1()), center(q.v1()),
                    q.x2()+dx, q.y2()+dy, q.z2()+dz, center(q.u2()), center(q.v2()),
                    q.x3()+dx, q.y3()+dy, q.z3()+dz, center(q.u3()), center(q.v3()),
                    q.nx(), q.ny(), q.nz(), light, overlay);
        }
    }

    private static float center(float uv) { return 0.125f + uv * 0.75f; }

    private static void emitQuad(VertexConsumer buf, Matrix4f mat, PoseStack.Pose entry,
                                   float x0, float y0, float z0, float u0, float v0,
                                   float x1, float y1, float z1, float u1, float v1,
                                   float x2, float y2, float z2, float u2, float v2,
                                   float x3, float y3, float z3, float u3, float v3,
                                   float nx, float ny, float nz, int light, int overlay) {
        buf.addVertex(mat, x0, y0, z0).setColor(255,255,255,255).setUv(u0,v0).setOverlay(overlay).setLight(light).setNormal(entry,nx,ny,nz);
        buf.addVertex(mat, x1, y1, z1).setColor(255,255,255,255).setUv(u1,v1).setOverlay(overlay).setLight(light).setNormal(entry,nx,ny,nz);
        buf.addVertex(mat, x2, y2, z2).setColor(255,255,255,255).setUv(u2,v2).setOverlay(overlay).setLight(light).setNormal(entry,nx,ny,nz);
        buf.addVertex(mat, x3, y3, z3).setColor(255,255,255,255).setUv(u3,v3).setOverlay(overlay).setLight(light).setNormal(entry,nx,ny,nz);
    }

    // -------------------------------------------------------------------------
    // Model loading
    // -------------------------------------------------------------------------

    private static LPPipeModel getModel() {
        if (model == null) {
            synchronized (LPPipeRenderer.class) {
                if (model == null) {
                    try {
                        ResourceLocation objLoc = ResourceLocation.fromNamespaceAndPath("logisticspipes", "pipe/pipemodel_moved.obj");
                        try (InputStream is = Minecraft.getInstance().getResourceManager().open(objLoc)) {
                            model = LPPipeModel.parse(is);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return model;
    }

    // -------------------------------------------------------------------------
    // Direction helpers
    // -------------------------------------------------------------------------

    private static String dirChar(Direction dir) {
        return switch (dir) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST  -> "E";
            case WEST  -> "W";
            case UP    -> "U";
            case DOWN  -> "D";
        };
    }

    // -------------------------------------------------------------------------
    // Edge data (12 edges, each touching 2 directions)
    // -------------------------------------------------------------------------

    private static final String[] EDGE_NAMES = {
            "Edge_M_U_N", "Edge_M_U_S", "Edge_M_U_E", "Edge_M_U_W",
            "Edge_M_D_N", "Edge_M_D_S", "Edge_M_D_E", "Edge_M_D_W",
            "Edge_M_S_NW", "Edge_M_S_NE", "Edge_M_S_SE", "Edge_M_S_SW"
    };

    private static final Direction[][] EDGE_DIRS = {
            { UP,   NORTH }, { UP,   SOUTH }, { UP,   EAST  }, { UP,   WEST  },
            { DOWN, NORTH }, { DOWN, SOUTH }, { DOWN, EAST  }, { DOWN, WEST  },
            { NORTH, WEST }, { NORTH, EAST }, { SOUTH, EAST }, { SOUTH, WEST }
    };

    // -------------------------------------------------------------------------
    // Corner data (8 corners, each touching 3 directions)
    //
    // Spacer numbers match LP1's PipeTurnCorner enum (direction → spacer#):
    //   UP_NW: UP=23, N=1,  W=14   UP_NE: UP=22, N=2,  E=9
    //   UP_SW: UP=24, S=6,  W=13   UP_SE: UP=21, S=5,  E=10
    //   DN_NW: DN=20, N=4,  W=15   DN_NE: DN=17, N=3,  E=12
    //   DN_SW: DN=19, S=7,  W=16   DN_SE: DN=18, S=8,  E=11
    // -------------------------------------------------------------------------

    record CornerDef(String name, Direction[] dirs, int[] spacers) {
        int spacerFor(Direction d) {
            for (int i = 0; i < dirs.length; i++)
                if (dirs[i] == d) return spacers[i];
            return 1;
        }
    }

    // -------------------------------------------------------------------------
    // Mount data (24 mounts: dir + perpendicular side)
    // -------------------------------------------------------------------------

    record MountDef(Direction dir, Direction side) {}

    private static final MountDef[] ALL_MOUNTS = {
        new MountDef(UP,    NORTH), new MountDef(UP,    SOUTH), new MountDef(UP,    EAST),  new MountDef(UP,    WEST),
        new MountDef(DOWN,  NORTH), new MountDef(DOWN,  SOUTH), new MountDef(DOWN,  EAST),  new MountDef(DOWN,  WEST),
        new MountDef(NORTH, UP),    new MountDef(NORTH, DOWN),  new MountDef(NORTH, EAST),  new MountDef(NORTH, WEST),
        new MountDef(SOUTH, UP),    new MountDef(SOUTH, DOWN),  new MountDef(SOUTH, EAST),  new MountDef(SOUTH, WEST),
        new MountDef(EAST,  UP),    new MountDef(EAST,  DOWN),  new MountDef(EAST,  NORTH), new MountDef(EAST,  SOUTH),
        new MountDef(WEST,  UP),    new MountDef(WEST,  DOWN),  new MountDef(WEST,  NORTH), new MountDef(WEST,  SOUTH),
    };

    private static void mountFindOpponent(List<MountDef> cands, Direction dir) {
        cands.removeIf(md -> md.dir() != dir);
        if (cands.size() <= 2) return;
        boolean[] sides = new boolean[6];
        for (MountDef md : cands) sides[md.side().ordinal()] = true;
        Direction k1 = null, k2 = null;
        if (sides[NORTH.ordinal()] && sides[SOUTH.ordinal()]) { k1 = NORTH; k2 = SOUTH; }
        else if (sides[EAST.ordinal()]  && sides[WEST.ordinal()])  { k1 = EAST;  k2 = WEST;  }
        else if (sides[UP.ordinal()]    && sides[DOWN.ordinal()])   { k1 = UP;    k2 = DOWN;  }
        if (k1 != null) { Direction fk1 = k1, fk2 = k2; cands.removeIf(md -> md.side() != fk1 && md.side() != fk2); }
    }

    private static void mountReduceToOne(List<MountDef> cands, Direction dir) {
        boolean found = false;
        Iterator<MountDef> it = cands.iterator();
        while (it.hasNext()) { MountDef md = it.next(); if (md.dir() != dir) continue; if (found) it.remove(); else found = true; }
    }

    private static void mountReduceToOnePref(List<MountDef> cands, Direction dir, Direction pref) {
        boolean hasPref = cands.stream().anyMatch(md -> md.dir() == dir && md.side() == pref);
        if (!hasPref) mountReduceToOne(cands, dir);
        else cands.removeIf(md -> md.dir() == dir && md.side() != pref);
    }

    private static void mountRemoveIfHasOpponent(List<MountDef> cands) {
        boolean[] dirs = new boolean[6];
        for (MountDef md : cands) dirs[md.dir().ordinal()] = true;
        if (dirs[NORTH.ordinal()] && dirs[SOUTH.ordinal()]) {
            cands.removeIf(md -> md.dir() == EAST || md.dir() == WEST);
            mountReduceToOne(cands, NORTH); mountReduceToOne(cands, SOUTH);
        } else if (dirs[EAST.ordinal()] && dirs[WEST.ordinal()]) {
            cands.removeIf(md -> md.dir() == NORTH || md.dir() == SOUTH);
            mountReduceToOne(cands, EAST); mountReduceToOne(cands, WEST);
        }
    }

    private static void mountRemoveIfHasConnected(List<MountDef> cands) {
        boolean[] dirs = new boolean[6];
        for (MountDef md : cands) dirs[md.dir().ordinal()] = true;
        for (Direction dir : new Direction[]{ NORTH, SOUTH, EAST, WEST }) {
            Direction rot = dir.getClockWise();
            if (dirs[dir.ordinal()] && dirs[rot.ordinal()]) {
                mountReduceToOnePref(cands, dir, dir.getCounterClockWise());
                mountReduceToOnePref(cands, rot, rot.getClockWise());
            }
        }
    }

    private static final CornerDef[] CORNERS = {
        new CornerDef("U_NW", new Direction[]{ UP,   NORTH, WEST  }, new int[]{ 23,  1, 14 }),
        new CornerDef("U_NE", new Direction[]{ UP,   NORTH, EAST  }, new int[]{ 22,  2,  9 }),
        new CornerDef("U_SW", new Direction[]{ UP,   SOUTH, WEST  }, new int[]{ 24,  6, 13 }),
        new CornerDef("U_SE", new Direction[]{ UP,   SOUTH, EAST  }, new int[]{ 21,  5, 10 }),
        new CornerDef("D_NW", new Direction[]{ DOWN, NORTH, WEST  }, new int[]{ 20,  4, 15 }),
        new CornerDef("D_NE", new Direction[]{ DOWN, NORTH, EAST  }, new int[]{ 17,  3, 12 }),
        new CornerDef("D_SW", new Direction[]{ DOWN, SOUTH, WEST  }, new int[]{ 19,  7, 16 }),
        new CornerDef("D_SE", new Direction[]{ DOWN, SOUTH, EAST  }, new int[]{ 18,  8, 11 }),
    };
}
