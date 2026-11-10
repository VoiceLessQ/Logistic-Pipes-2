package logisticspipes.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.impl.LPRenderStateImpl;
import logisticspipes.renderer.newpipe.LogisticsNewPipeItemBoxRenderer;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsRenderPipe implements BlockEntityRenderer<LogisticsTileGenericPipe, LogisticsRenderPipe.State> {

	private static final int LIQUID_STAGES = 40;
	private static final int MAX_ITEMS_TO_RENDER = 10;
	/** Atlas geometry of pipes and LP blocks; models carry back-faced copies where the inside is visible. */
	public static final RenderType ATLAS_TYPE = RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS);
	private static final BlockState SIGN_BOARD = Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH);
	public static LogisticsNewRenderPipe secondRenderer = new LogisticsNewRenderPipe();
	public static LogisticsNewPipeItemBoxRenderer boxRenderer = new LogisticsNewPipeItemBoxRenderer();
	public static ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();

	private final ItemModelResolver itemModelResolver;

	public static class State extends BlockEntityRenderState {

		public LogisticsTileGenericPipe tile;
		public float partialTicks;
	}

	public LogisticsRenderPipe(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	/**
	 * Queues LP model geometry: {@code draw} runs when the feature renderer drains the queue, with
	 * the shared {@link LPRenderStateImpl} bound to that pass's buffer and the pose captured now.
	 */
	public static void submitGeometry(SubmitNodeCollector collector, PoseStack poseStack, RenderType type, int light, int overlay, Runnable draw) {
		if (!(SimpleServiceLocator.cclProxy.getRenderState() instanceof LPRenderStateImpl)) return;
		collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
			LPRenderStateImpl rs = (LPRenderStateImpl) SimpleServiceLocator.cclProxy.getRenderState();
			rs.bind(buffer, pose.pose(), pose.normal(), light, overlay);
			rs.reset();
			draw.run();
			rs.draw();
		});
	}

	public static TextureAtlasSprite getBlockSprite(Identifier id) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
	}

	/** Multiblock pipes draw outside their own block; the distance check in shouldRender still applies. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(LogisticsTileGenericPipe tile, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
		state.tile = tile;
		state.partialTicks = partialTicks;
	}

	@Override
	public void submit(State state, @Nonnull PoseStack poseStack, @Nonnull SubmitNodeCollector collector, @Nonnull CameraRenderState camera) {
		LogisticsTileGenericPipe tileentity = state.tile;
		if (tileentity == null || tileentity.pipe == null) return;
		int packedLight = state.lightCoords;
		int packedOverlay = OverlayTexture.NO_OVERLAY;

		// placeholder cube when the OBJ pipe models failed to load, so broken geometry stays visible
		if (LogisticsNewRenderPipe.sideNormal.isEmpty()) {
			collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS),
					(pose, buffer) -> drawPlaceholderCube(tileentity, pose, buffer, packedLight, packedOverlay));
		}

		poseStack.pushPose();
		try {
			renderInternal(tileentity, 0, 0, 0, state.partialTicks, poseStack, collector, packedLight, packedOverlay);
		} finally {
			poseStack.popPose();
		}
	}

	private void drawPlaceholderCube(LogisticsTileGenericPipe tileentity, PoseStack.Pose pose, VertexConsumer vc, int packedLight, int packedOverlay) {
		// Core cube 6/16..10/16 centered in the block.
		float a = 0.375f, b = 0.625f;
		int pipeHash = tileentity.pipe.getClass().getName().hashCode();
		int r = 64 + ((pipeHash >>> 16) & 0x7F);
		int g = 64 + ((pipeHash >>> 8) & 0x7F);
		int bl = 64 + (pipeHash & 0x7F);
		emitBox(vc, pose, a, a, a, b, b, b, r, g, bl, 0, 1, 0, 1, packedLight, packedOverlay);

		if (tileentity.renderState != null && tileentity.renderState.pipeConnectionMatrix != null) {
			for (Direction dir : Direction.values()) {
				if (!tileentity.renderState.pipeConnectionMatrix.isConnected(dir)) continue;
				float x0 = a, y0 = a, z0 = a, x1 = b, y1 = b, z1 = b;
				switch (dir) {
					case DOWN:  y0 = 0f; y1 = a; break;
					case UP:    y0 = b; y1 = 1f; break;
					case NORTH: z0 = 0f; z1 = a; break;
					case SOUTH: z0 = b; z1 = 1f; break;
					case WEST:  x0 = 0f; x1 = a; break;
					case EAST:  x0 = b; x1 = 1f; break;
				}
				emitBox(vc, pose, x0, y0, z0, x1, y1, z1, r, g, bl, 0, 1, 0, 1, packedLight, packedOverlay);
			}
		}
	}

	private static void emitBox(VertexConsumer vc, PoseStack.Pose m,
			float x0, float y0, float z0, float x1, float y1, float z1,
			int r, int g, int b, float u0, float u1, float v0, float v1, int packedLight, int packedOverlay) {
		quad(vc, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, r, g, b, u0, u1, v0, v1, packedLight, packedOverlay);
		quad(vc, m, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, r, g, b, u0, u1, v0, v1, packedLight, packedOverlay);
		quad(vc, m, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, r, g, b, u0, u1, v0, v1, packedLight, packedOverlay);
		quad(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, r, g, b, u0, u1, v0, v1, packedLight, packedOverlay);
		quad(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, r, g, b, u0, u1, v0, v1, packedLight, packedOverlay);
		quad(vc, m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, r, g, b, u0, u1, v0, v1, packedLight, packedOverlay);
	}

	private static void quad(VertexConsumer vc, PoseStack.Pose m,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4,
			float nx, float ny, float nz, int r, int g, int b,
			float u0, float u1, float v0, float v1, int packedLight, int packedOverlay) {
		vc.addVertex(m, x1, y1, z1).setColor(r, g, b, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(m, nx, ny, nz);
		vc.addVertex(m, x2, y2, z2).setColor(r, g, b, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(m, nx, ny, nz);
		vc.addVertex(m, x3, y3, z3).setColor(r, g, b, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(m, nx, ny, nz);
		vc.addVertex(m, x4, y4, z4).setColor(r, g, b, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(m, nx, ny, nz);
	}

	public static logisticspipes.proxy.object3d.interfaces.TextureTransformation requestTableIcon = null;

	/**
	 * Draws the Request Table's full block body. Port of the dead 1.12
	 * LogisticsNewPipeWorldRenderer request-table branch onto the
	 * {@link LogisticsSolidBlockRenderer#renderSolid} draw path: the shared solid-block OBJ
	 * body plus cover plates, with plates omitted on connected sides and the body rotated
	 * to the table's facing. Texture is LP1's {@code blocks/requesttable/requesttexture}
	 * (stitched into the block atlas by the {@code blocks/} directory source).
	 */
	private void renderRequestTableBlock(PipeBlockRequestTable table, LogisticsTileGenericPipe pipeTile,
			PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
		if (!(SimpleServiceLocator.cclProxy.getRenderState() instanceof logisticspipes.proxy.object3d.impl.LPRenderStateImpl)) return;
		if (logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.block == null
				|| logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.block.isEmpty()) return;

		if (requestTableIcon == null) {
			TextureAtlasSprite sprite = getBlockSprite(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "blocks/requesttable/requesttexture"));
			requestTableIcon = SimpleServiceLocator.cclProxy.createIconTransformer(sprite);
		}
		if (requestTableIcon == null) return;
		submitGeometry(collector, poseStack, ATLAS_TYPE, packedLight, packedOverlay, () -> renderRequestTableModels(table, pipeTile));
	}

	private void renderRequestTableModels(PipeBlockRequestTable table, LogisticsTileGenericPipe pipeTile) {

		logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.BlockRotation rotation =
				logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.BlockRotation.getRotation(table.getRotation());
		if (rotation == null) rotation = logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.BlockRotation.ZERO;

		logisticspipes.proxy.object3d.interfaces.IModel3D body =
				logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.block.get(rotation);
		if (body != null) {
			body.render(requestTableIcon);
		}

		for (logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.CoverSides side :
				logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.CoverSides.values()) {
			// LP1 skipped the plates on sides with pipe connections so adjacent pipes visually
			// enter the table.
			if (pipeTile.renderState != null && pipeTile.renderState.pipeConnectionMatrix.isConnected(side.getDir(rotation))) {
				continue;
			}
			java.util.Map<logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.BlockRotation, logisticspipes.proxy.object3d.interfaces.IModel3D> outer =
					logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.texturePlate_Outer.get(side);
			java.util.Map<logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.BlockRotation, logisticspipes.proxy.object3d.interfaces.IModel3D> inner =
					logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer.texturePlate_Inner.get(side);
			if (outer != null && outer.get(rotation) != null) {
				outer.get(rotation).render(requestTableIcon);
			}
			if (inner != null && inner.get(rotation) != null) {
				inner.get(rotation).render(requestTableIcon);
			}
		}
	}

	private void renderInternal(LogisticsTileGenericPipe tileentity, double x, double y, double z, float partialTicks,
			PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
		// In 1.20.1 the BER PoseStack is pre-translated, so we always pass (0,0,0).
		// Use tileentity==null as the sole in-hand signal instead.
		boolean inHand = (tileentity == null);
		if (!inHand && tileentity.pipe == null) {
			return;
		}

		// 1.20.1: depth + rescale-normal + colour state are managed by the bound RenderType,
		// so the old GlStateManager._enableDepthTest/_depthFunc/_depthMask block is gone.
		// destroyStage overlay is handled by the outer BER pipeline (crumbling buffer) and
		// no longer needs a per-pipe matrix push here.

		poseStack.pushPose();
		try {
			if (!inHand && SimpleServiceLocator.cclProxy.isActivated()) {
				if (tileentity.pipe instanceof CoreRoutedPipe) {
					renderPipeSigns((CoreRoutedPipe) tileentity.pipe, x, y, z, partialTicks, poseStack, collector, packedLight, packedOverlay);
				}
			}

			double distance = !inHand ? new DoubleCoordinates((BlockEntity) tileentity).distanceTo(new DoubleCoordinates(Minecraft.getInstance().player)) : 0;

			if (SimpleServiceLocator.cclProxy.isActivated()) {
				// The Request Table is an isPipeBlock() pipe: LogisticsNewRenderPipe skips it and
				// the 1.12 ISimpleBlockRenderingHandler that drew its block body was never ported,
				// so without this branch the table is invisible.
				if (!inHand && tileentity.pipe instanceof PipeBlockRequestTable) {
					renderRequestTableBlock((PipeBlockRequestTable) tileentity.pipe, tileentity, poseStack, collector, packedLight, packedOverlay);
				}
				LogisticsRenderPipe.secondRenderer.renderTileEntityAt(tileentity, distance, poseStack, collector, packedLight, packedOverlay);
			}

			if (!inHand && !tileentity.isOpaque()) {
				// renderSolids uses only Blaze3D / PoseStack — no CCL dependency.
				if (tileentity.pipe.transport != null) {
					renderSolids(tileentity.pipe, x, y, z, partialTicks, poseStack, collector, packedLight, packedOverlay);
				}
			}
		} finally {
			poseStack.popPose();
		}
		// MCMP special renderer removed — MCMultiPart has no 1.20.1 port (former dummy was a no-op).
	}

	private void renderSolids(CoreUnroutedPipe pipe, double x, double y, double z, float partialTickTime,
			PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
		poseStack.pushPose();

		float light = 1.0F; // full-bright; actual lighting applied via packedLight parameter

		int count = 0;
		for (LPTravelingItem item : pipe.transport.items) {
			CoreUnroutedPipe lPipe = pipe;
			double lX = x;
			double lY = y;
			double lZ = z;
			float lItemYaw = item.getYaw();
			if (count >= LogisticsRenderPipe.MAX_ITEMS_TO_RENDER) {
				break;
			}

			if (item.getItemIdentifierStack() == null) {
				continue;
			}
			if (!item.getContainer().getBlockPos().equals(lPipe.container.getBlockPos())) {
				continue;
			}

			if (item.getPosition() > lPipe.transport.getPipeLength() || item.getPosition() < 0) {
				continue;
			}

			float fPos = item.getPosition() + item.getSpeed() * partialTickTime;
			if (fPos > lPipe.transport.getPipeLength() && item.output != null) {
				CoreUnroutedPipe nPipe = lPipe.transport.getNextPipe(item.output);
				if (nPipe != null) {
					fPos -= lPipe.transport.getPipeLength();
					lX -= lPipe.getX() - nPipe.getX();
					lY -= lPipe.getY() - nPipe.getY();
					lZ -= lPipe.getZ() - nPipe.getZ();
					lItemYaw += lPipe.transport.getYawDiff(item);
					lPipe = nPipe;
					item = item.renderCopy();
					item.input = item.output;
					item.output = null;
				} else {
					continue;
				}
			}

			DoubleCoordinates pos = lPipe.getItemRenderPos(fPos, item);
			if (pos == null) {
				continue;
			}
			double boxScale = lPipe.getBoxRenderScale(fPos, item);
			double itemYaw = (lPipe.getItemRenderYaw(fPos, item) - lPipe.getItemRenderYaw(0, item) + lItemYaw) % 360;
			double itemPitch = lPipe.getItemRenderPitch(fPos, item);
			double itemYawForPitch = lPipe.getItemRenderYaw(fPos, item);

			ItemStack stack = item.getItemIdentifierStack().makeNormalStack();
			doRenderItem(stack, pipe.container.getWorld(), lX + pos.getXCoord(), lY + pos.getYCoord(), lZ + pos.getZCoord(), light, 0.75F, boxScale, itemYaw, itemPitch, itemYawForPitch, partialTickTime, poseStack, collector, packedLight, packedOverlay);
			count++;
		}

		count = 0;
		double dist = 0.135;
		DoubleCoordinates pos = new DoubleCoordinates(0.5, 0.5, 0.5);
		CoordinateUtils.add(pos, Direction.SOUTH, dist);
		CoordinateUtils.add(pos, Direction.EAST, dist);
		CoordinateUtils.add(pos, Direction.UP, dist);
		for (Pair<ItemIdentifierStack, Pair<Integer, Integer>> item : pipe.transport._itemBuffer) {
			if (item == null || item.getValue1() == null) {
				continue;
			}
			ItemStack stack = item.getValue1().makeNormalStack();
			doRenderItem(stack, pipe.container.getWorld(), x + pos.getXCoord(), y + pos.getYCoord(), z + pos.getZCoord(), light, 0.25F, 0, 0, 0, 0, partialTickTime, poseStack, collector, packedLight, packedOverlay);
			count++;
			if (count >= 27) {
				break;
			} else if (count % 9 == 0) {
				CoordinateUtils.add(pos, Direction.SOUTH, dist * 2.0);
				CoordinateUtils.add(pos, Direction.EAST, dist * 2.0);
				CoordinateUtils.add(pos, Direction.DOWN, dist);
			} else if (count % 3 == 0) {
				CoordinateUtils.add(pos, Direction.SOUTH, dist * 2.0);
				CoordinateUtils.add(pos, Direction.WEST, dist);
			} else {
				CoordinateUtils.add(pos, Direction.NORTH, dist);
			}
		}

		poseStack.popPose();
	}

	public void doRenderItem(@Nonnull ItemStack itemstack, Level world, double x, double y, double z, float light, float renderScale, double boxScale, double yaw, double pitch, double yawForPitch, float partialTickTime,
			PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
		LogisticsRenderPipe.boxRenderer.doRenderItem(itemstack, light, x, y, z, boxScale, yaw, pitch, yawForPitch, poseStack, collector, packedLight, packedOverlay);

		poseStack.pushPose();
		poseStack.translate(x, y, z);
		poseStack.scale(renderScale, renderScale, renderScale);
		// Historic order: yaw around Y, then pitch around X after a secondary yawForPitch
		// rotation, matching the 1.12.2 glRotated sequence in CoreRoutedPipe-driven items.
		poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(yaw)));
		poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(yawForPitch)));
		poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.toRadians(pitch)));
		poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(-yawForPitch)));
		// 1.12's -0.35 offset compensated EntityItem's foot-to-center gap; the GROUND transform has none
		submitItem(itemstack, ItemDisplayContext.GROUND, world, poseStack, collector, packedLight, packedOverlay);
		poseStack.popPose();
	}

	private boolean needDistance(List<Pair<Direction, IPipeSign>> list) {
		List<Pair<Direction, IPipeSign>> copy = new ArrayList<>(list);
		Iterator<Pair<Direction, IPipeSign>> iter = copy.iterator();
		boolean north = false, south = false, east = false, west = false;
		while (iter.hasNext()) {
			Pair<Direction, IPipeSign> pair = iter.next();
			if (pair.getValue1() == Direction.UP || pair.getValue1() == Direction.DOWN || pair.getValue1() == null) {
				iter.remove();
			}
			if (pair.getValue1() == Direction.NORTH) {
				north = true;
			}
			if (pair.getValue1() == Direction.SOUTH) {
				south = true;
			}
			if (pair.getValue1() == Direction.EAST) {
				east = true;
			}
			if (pair.getValue1() == Direction.WEST) {
				west = true;
			}
		}
		boolean result = copy.size() > 1;
		if (copy.size() == 2) {
			if (north && south) {
				result = false;
			}
			if (east && west) {
				result = false;
			}
		}
		return result;
	}

	private void renderPipeSigns(CoreRoutedPipe pipe, double x, double y, double z, float partialTickTime,
			PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
		List<Pair<Direction, IPipeSign>> pipeSigns = pipe.getPipeSigns();
		if (pipe.container != null && !pipeSigns.isEmpty()) {
			for (Pair<Direction, IPipeSign> pair : pipeSigns) {
				if (pipe.container.renderState.pipeConnectionMatrix.isConnected(pair.getValue1())) {
					continue;
				}
				poseStack.pushPose();
				poseStack.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
				switch (pair.getValue1()) {
					case UP:
						poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.toRadians(90)));
						break;
					case DOWN:
						poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.toRadians(-90)));
						break;
					case NORTH:
						// 0° yaw; no rotation required
						if (needDistance(pipeSigns)) {
							poseStack.translate(0.0F, 0.0F, -0.15F);
						}
						break;
					case SOUTH:
						poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(-180)));
						if (needDistance(pipeSigns)) {
							poseStack.translate(0.0F, 0.0F, -0.15F);
						}
						break;
					case EAST:
						poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(-90)));
						if (needDistance(pipeSigns)) {
							poseStack.translate(0.0F, 0.0F, -0.15F);
						}
						break;
					case WEST:
						poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(90)));
						if (needDistance(pipeSigns)) {
							poseStack.translate(0.0F, 0.0F, -0.15F);
						}
						break;
					default:
				}
				renderSign(pipe, pair.getValue2(), partialTickTime, poseStack, collector, packedLight);
				poseStack.popPose();
			}
		}
	}

	private void renderSign(CoreRoutedPipe pipe, IPipeSign type, float partialTickTime, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
		float signScale = 2 / 3.0F;
		poseStack.translate(0.0F, -0.3125F, -0.36F);
		poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(180)));
		// LP1 drew the vanilla sign board (scaled by signScale); 26.x has it as the wall sign block model, front at +z
		poseStack.pushPose();
		poseStack.translate(-0.5F, -0.1875F, -0.0625F);
		List<BlockStateModelPart> board = new ArrayList<>();
		Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(SIGN_BOARD).collectParts(RandomSource.create(0), board);
		collector.submitBlockModel(poseStack, ATLAS_TYPE, board, BlockModelRenderState.EMPTY_TINTS, packedLight, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
		// LP1's non-framebuffer placement; its framebuffer quad lands within 0.02 of it
		poseStack.translate(-0.32F, 0.5F * signScale + 0.08F, 0.07F * signScale);
		type.render(pipe, this, poseStack, collector, packedLight);
	}

	public void renderItemStackOnSign(@Nonnull ItemStack itemstack) {
		// Legacy no-arg entry; the PoseStack overload below draws.
	}

	public void renderItemStackOnSign(@Nonnull ItemStack itemstack, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
		if (itemstack.isEmpty()) return;
		poseStack.pushPose();
		// LP1: flat GUI-style item on the left of the sign
		poseStack.translate(0.05F, 0F, 0F);
		poseStack.scale(0.8F, 0.8F, 0.001F);
		poseStack.scale(0.4F, 0.4F, 0.4F);
		submitItem(itemstack, ItemDisplayContext.GUI, Minecraft.getInstance().level, poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}

	// a fresh state per item: the collector keeps the state's quad list until the frame is drawn
	private void submitItem(ItemStack stack, ItemDisplayContext context, Level level, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
		ItemStackRenderState state = new ItemStackRenderState();
		itemModelResolver.updateForTopItem(state, stack, context, level, null, 0);
		state.submit(poseStack, collector, light, overlay, 0);
	}

	public String cut(String name, Font renderer) {
		if (renderer.width(name) < 90) {
			return name;
		}
		StringBuilder sum = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			if (renderer.width(sum.toString() + name.charAt(i) + "...") < 90) {
				sum.append(name.charAt(i));
			} else {
				return sum + "...";
			}
		}
		return sum.toString();
	}
}
