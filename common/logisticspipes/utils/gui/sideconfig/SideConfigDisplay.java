package logisticspipes.utils.gui.sideconfig;


import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;

import logisticspipes.LPConstants;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.LPPositionSet;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

/**
 * 3D view of the configured blocks and their neighbours inside a GUI rectangle; the hovered face is
 * outlined and a click selects it. Based on EnderIO's GuiOverlayIoConfig. Drawn as a picture-in-picture
 * element ({@link Renderer}, registered from {@link #registerRenderer()}).
 */
@Environment(EnvType.CLIENT)
public abstract class SideConfigDisplay {

	private static final float FOV = 30.0F;
	private static final float Z_NEAR = 0.05F;
	private static final float Z_FAR = 50.0F;
	private static final Identifier SELECTION_TEXTURE = Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "textures/blocks/sideselection.png");

	// LP1's neighbour pass: additive blend (SRC_ALPHA, ONE), depth tested, no depth write
	private static final RenderPipeline NEIGHBOUR_PIPELINE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "pipeline/sideconfig_neighbour"))
			.withShaderDefine("ALPHA_CUTOUT", 0.1F)
			.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.build();
	private static final RenderPipeline SELECTION_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "pipeline/sideconfig_selection"))
			.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
			.withCull(false)
			.build();
	private static final RenderType NEIGHBOUR = RenderType.create("logisticspipes_sideconfig_neighbour",
			RenderSetup.builder(NEIGHBOUR_PIPELINE).useLightmap().withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS).createRenderSetup());
	private static final RenderType SELECTION = RenderType.create("logisticspipes_sideconfig_selection",
			RenderSetup.builder(SELECTION_PIPELINE).withTexture("Sampler0", SELECTION_TEXTURE).createRenderSetup());

	private float pitch;
	private float yaw;
	private double distance;

	private final Minecraft mc = Minecraft.getInstance();
	private final ClientLevel world;

	/** Scene centre in world coordinates; the scene is drawn relative to it. */
	private final Vec3 origin;
	private final Matrix4f view = new Matrix4f();

	public DoubleCoordinates originBC;

	private final List<DoubleCoordinates> configurables = new ArrayList<>();
	private final List<DoubleCoordinates> neighbours = new ArrayList<>();

	private SelectedFace selection;

	public boolean renderNeighbours = true;

	public SideConfigDisplay(CoreRoutedPipe configurables) {
		this(Collections.singletonList(configurables.getLPPosition()));
	}

	public SideConfigDisplay(LPPositionSet<DoubleCoordinates> configurables) {
		this(Arrays.asList(configurables.toArray(new DoubleCoordinates[0])));
	}

	public SideConfigDisplay(List<DoubleCoordinates> configurables) {
		this.configurables.addAll(configurables);

		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
		for (DoubleCoordinates bc : configurables) {
			minX = Math.min(bc.getXInt(), minX);
			minY = Math.min(bc.getYInt(), minY);
			minZ = Math.min(bc.getZInt(), minZ);
			maxX = Math.max(bc.getXInt() + 1, maxX);
			maxY = Math.max(bc.getYInt() + 1, maxY);
			maxZ = Math.max(bc.getZInt() + 1, maxZ);
		}
		origin = new Vec3((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
		originBC = new DoubleCoordinates((int) Math.floor(origin.x), (int) Math.floor(origin.y), (int) Math.floor(origin.z));

		pitch = -mc.player.getXRot();
		yaw = 180 - mc.player.getYRot();

		distance = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ) + 4;

		for (DoubleCoordinates bc : configurables) {
			for (Direction dir : Direction.values()) {
				DoubleCoordinates loc = CoordinateUtils.add(new DoubleCoordinates(bc), dir);
				if (!configurables.contains(loc)) {
					neighbours.add(loc);
				}
			}
		}

		world = mc.level;
	}

	public static void registerRenderer() {
		PictureInPictureRendererRegistry.register(ctx -> new Renderer());
	}

	public abstract void handleSelection(SelectedFace selection);

	public void init() {}

	public SelectedFace getSelection() {
		return selection;
	}

	/** Called by the parent Screen's mouseDragged; rotates the camera. */
	public void onMouseDragged(double dx, double dy, int button) {
		if (button == 0) {
			yaw += (float) dx;
			pitch += (float) dy;
			pitch = Math.max(-90, Math.min(90, pitch));
		}
	}

	/** Called by the parent Screen's mouseScrolled; zooms in/out. */
	public void onMouseScrolled(double delta) {
		distance = Math.max(1.5, Math.min(20.0, distance - delta));
	}

	/** Called by the parent Screen on left-click inside {@code bounds} (GUI coordinates); fires handleSelection on a face. */
	public void onMouseClicked(int mouseX, int mouseY, Rectangle bounds) {
		updateView();
		updateSelection(mouseX, mouseY, bounds);
		if (selection != null) {
			handleSelection(selection);
		}
	}

	/** Draws the scene into {@code bounds} (GUI coordinates) and outlines the face under the mouse. */
	public void drawScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Rectangle bounds) {
		if (bounds.width <= 0 || bounds.height <= 0) {
			return;
		}
		updateView();
		if (bounds.contains(mouseX, mouseY)) {
			updateSelection(mouseX, mouseY, bounds);
		} else {
			selection = null;
		}
		graphics.guiRenderState.addPicturesInPictureState(new State(this, new Matrix4f(view), selection,
				bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, null));
	}

	private void updateView() {
		Vector3f eye = new Vector3f(0, 0, (float) distance)
				.rotateX((float) Math.toRadians(pitch))
				.rotateY((float) Math.toRadians(yaw));
		view.setLookAt(eye.x, eye.y, eye.z, 0, 0, 0, 0, 1, 0);
	}

	private void updateSelection(int mouseX, int mouseY, Rectangle bounds) {
		Matrix4f projView = new Matrix4f().setPerspective((float) Math.toRadians(FOV), (float) bounds.width / bounds.height, Z_NEAR, Z_FAR).mul(view);
		Vector3f rayOrigin = new Vector3f();
		Vector3f rayDir = new Vector3f();
		projView.unprojectRay(mouseX - bounds.x + 0.5F, bounds.height - (mouseY - bounds.y + 0.5F),
				new int[] { 0, 0, bounds.width, bounds.height }, rayOrigin, rayDir);
		Vec3 start = origin.add(rayOrigin.x, rayOrigin.y, rayOrigin.z);
		rayDir.normalize(Z_FAR);
		Vec3 end = start.add(rayDir.x, rayDir.y, rayDir.z);

		selection = null;
		double minDist = Double.POSITIVE_INFINITY;
		for (DoubleCoordinates coord : configurables) {
			BlockPos pos = coord.getBlockPos();
			BlockHitResult hit = AABB.clip(List.of(new AABB(0, 0, 0, 1, 1, 1)), start, end, pos);
			BlockEntity be = world.getBlockEntity(pos);
			if (hit != null && be != null) {
				double d = hit.getLocation().distanceToSqr(start);
				if (d < minDist) {
					minDist = d;
					selection = new SelectedFace(be, hit.getDirection(), hit);
				}
			}
		}
	}

	private void submitScene(PoseStack poseStack, SubmitNodeCollector collector, @Nullable SelectedFace selected) {
		ModelBlockRenderer blockRenderer = new ModelBlockRenderer(mc.options.ambientOcclusion().get(), false, mc.getBlockColors());
		if (renderNeighbours) {
			for (DoubleCoordinates coord : neighbours) {
				submitBlock(coord.getBlockPos(), blockRenderer, poseStack, collector, true);
				// chests and other BER-drawn neighbours have no additive variant; drawn solid as in LP2
				submitBlockEntity(coord.getBlockPos(), poseStack, collector);
			}
		}
		for (DoubleCoordinates coord : configurables) {
			BlockPos pos = coord.getBlockPos();
			submitBlock(pos, blockRenderer, poseStack, collector, false);
			// pipes have RenderShape.INVISIBLE, only their block entity renderer draws them
			submitBlockEntity(pos, poseStack, collector);
		}
		if (selected != null) {
			BlockPos pos = ((BlockHitResult) selected.hit).getBlockPos();
			poseStack.pushPose();
			poseStack.translate(pos.getX() - origin.x, pos.getY() - origin.y, pos.getZ() - origin.z);
			collector.order(1).submitCustomGeometry(poseStack, SELECTION, (pose, buffer) -> emitFace(pose, buffer, selected.face));
			poseStack.popPose();
		}
	}

	private void submitBlock(BlockPos pos, ModelBlockRenderer blockRenderer, PoseStack poseStack, SubmitNodeCollector collector, boolean neighbour) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(pos.getX() - origin.x, pos.getY() - origin.y, pos.getZ() - origin.z);
		var model = mc.getModelManager().getBlockStateModelSet().get(state);
		long seed = state.getSeed(pos);
		if (neighbour) {
			collector.submitCustomGeometry(poseStack, NEIGHBOUR, (pose, buffer) ->
					blockRenderer.tesselateBlock((x, y, z, quad, instance) -> {
						PoseStack.Pose quadPose = pose.copy();
						quadPose.translate(x, y, z);
						buffer.putBakedQuad(quadPose, quad, instance);
					}, 0, 0, 0, world, pos, state, model, seed));
		} else {
			for (RenderType type : new RenderType[] { RenderTypes.solidMovingBlock(), RenderTypes.cutoutMovingBlock() }) {
				boolean cutout = type == RenderTypes.cutoutMovingBlock();
				collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
						blockRenderer.tesselateBlock((x, y, z, quad, instance) -> {
							if ((quad.materialInfo().layer() != net.minecraft.client.renderer.chunk.ChunkSectionLayer.SOLID) == cutout) {
								PoseStack.Pose quadPose = pose.copy();
								quadPose.translate(x, y, z);
								buffer.putBakedQuad(quadPose, quad, instance);
							}
						}, 0, 0, 0, world, pos, state, model, seed));
			}
		}
		poseStack.popPose();
	}

	@SuppressWarnings("unchecked")
	private void submitBlockEntity(BlockPos pos, PoseStack poseStack, SubmitNodeCollector collector) {
		BlockEntity be = world.getBlockEntity(pos);
		if (be == null) {
			return;
		}
		BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = mc.getBlockEntityRenderDispatcher().getRenderer(be);
		if (renderer == null) {
			return;
		}
		BlockEntityRenderState state = renderer.createRenderState();
		renderer.extractRenderState(be, state, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), origin, null);
		poseStack.pushPose();
		poseStack.translate(pos.getX() - origin.x, pos.getY() - origin.y, pos.getZ() - origin.z);
		renderer.submit(state, poseStack, collector, new CameraRenderState());
		poseStack.popPose();
	}

	/** One face of the unit cube, slightly outset so it never z-fights the block below it. */
	private static void emitFace(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, Direction face) {
		float lo = -0.002F;
		float hi = 1.002F;
		float[][] corners = switch (face) {
			case DOWN -> new float[][] { { lo, lo, lo }, { hi, lo, lo }, { hi, lo, hi }, { lo, lo, hi } };
			case UP -> new float[][] { { lo, hi, hi }, { hi, hi, hi }, { hi, hi, lo }, { lo, hi, lo } };
			case NORTH -> new float[][] { { hi, lo, lo }, { lo, lo, lo }, { lo, hi, lo }, { hi, hi, lo } };
			case SOUTH -> new float[][] { { lo, lo, hi }, { hi, lo, hi }, { hi, hi, hi }, { lo, hi, hi } };
			case WEST -> new float[][] { { lo, lo, lo }, { lo, lo, hi }, { lo, hi, hi }, { lo, hi, lo } };
			case EAST -> new float[][] { { hi, lo, hi }, { hi, lo, lo }, { hi, hi, lo }, { hi, hi, hi } };
		};
		float[][] uvs = { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 0, 0 } };
		for (int i = 0; i < 4; i++) {
			buffer.addVertex(pose, corners[i][0], corners[i][1], corners[i][2]).setUv(uvs[i][0], uvs[i][1]).setColor(-1);
		}
	}

	public static class SelectedFace {

		public BlockEntity config;
		public Direction face;
		public HitResult hit;

		public SelectedFace(BlockEntity config, Direction face, HitResult hit) {
			super();
			this.config = config;
			this.face = face;
			this.hit = hit;
		}
	}

	private record State(SideConfigDisplay display, Matrix4f view, @Nullable SelectedFace selection,
			int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) implements PictureInPictureRenderState {

		@Override
		public float scale() {
			return 1.0F;
		}

		@Override
		public ScreenRectangle bounds() {
			return PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea);
		}
	}

	private static final class Renderer extends PictureInPictureRenderer<State> {

		private final Projection projection = new Projection();
		private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("LP side config");

		@Override
		public Class<State> getRenderStateClass() {
			return State.class;
		}

		@Override
		protected void renderToTexture(State state, PoseStack guiPose, SubmitNodeCollector collector) {
			// replaces the GUI ortho set up by prepare(); reversed-Z like the level
			projection.setupPerspective(Z_NEAR, Z_FAR, FOV, state.x1() - state.x0(), state.y1() - state.y0());
			RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection), ProjectionType.PERSPECTIVE);
			RenderSystem.getModelViewStack().set(state.view());
			Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);
			state.display().submitScene(new PoseStack(), collector, state.selection());
		}

		@Override
		protected String getTextureLabel() {
			return "logisticspipes side config";
		}

		@Override
		public void close() {
			super.close();
			projectionBuffer.close();
		}
	}
}
