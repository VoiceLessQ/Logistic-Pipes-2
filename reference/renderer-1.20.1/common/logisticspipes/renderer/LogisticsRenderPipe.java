package logisticspipes.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.resources.model.BakedModel;
import com.mojang.blaze3d.pipeline.RenderTarget; // was Framebuffer
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;





import logisticspipes.LogisticsPipes;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.newpipe.LogisticsNewPipeItemBoxRenderer;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsRenderPipe implements BlockEntityRenderer<LogisticsTileGenericPipe> {

	private static final ExecutorService pool = Executors.newFixedThreadPool(1);
	private static final int LIQUID_STAGES = 40;
	private static final int MAX_ITEMS_TO_RENDER = 10;
	private static final ResourceLocation SIGN = new ResourceLocation("textures/entity/sign.png");
	public static LogisticsNewRenderPipe secondRenderer = new LogisticsNewRenderPipe();
	public static LogisticsNewPipeItemBoxRenderer boxRenderer = new LogisticsNewPipeItemBoxRenderer();
	public static ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();
	private static final ItemStackRenderer itemRenderer = new ItemStackRenderer(0, 0, 0, false, false);

	public LogisticsRenderPipe(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(LogisticsTileGenericPipe tileentity, float partialTicks, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (tileentity == null || tileentity.pipe == null) return;

		// Fallback placeholder cube — only draws when the OBJ model pipeline failed to load
		// (empty sideNormal map), so broken-geometry states remain visible in-world. When
		// loadModels() succeeded, the real LogisticsNewRenderPipe path emits textured quads
		// through LPRenderStateImpl and the placeholder is skipped.
		if (logisticspipes.renderer.newpipe.LogisticsNewRenderPipe.sideNormal.isEmpty()) {
			drawPlaceholderCube(tileentity, poseStack, bufferSource, packedLight, packedOverlay);
		}

		// Everything below drives the legacy CCL-based path. Gate until CCLProxy is activated.
		if (!SimpleServiceLocator.cclProxy.isActivated()) return;

		// Bind the current MultiBufferSource buffer + lighting into the render state so that
		// any IModel3D.render(ops...) calls made downstream emit vertices into the correct
		// RenderType batch. The solid RenderType covers opaque pipe geometry; translucent
		// fluid/overlay passes will bind their own buffer when those paths are migrated.
		if (SimpleServiceLocator.cclProxy.getRenderState() instanceof logisticspipes.proxy.object3d.impl.LPRenderStateImpl) {
			logisticspipes.proxy.object3d.impl.LPRenderStateImpl rs =
				(logisticspipes.proxy.object3d.impl.LPRenderStateImpl) SimpleServiceLocator.cclProxy.getRenderState();
			com.mojang.blaze3d.vertex.VertexConsumer buffer =
				bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.cutoutMipped());
			rs.bind(buffer, poseStack.last().pose(), poseStack.last().normal(), packedLight, packedOverlay);
		}

		poseStack.pushPose();
		try {
			// Historic renderInternal() was called with absolute world x/y/z; the new BER
			// pipeline provides a PoseStack pre-translated to the block origin, so pass (0,0,0).
			renderInternal(tileentity, 0, 0, 0, partialTicks, -1, 1.0f, poseStack, bufferSource, packedLight, packedOverlay);
		} finally {
			poseStack.popPose();
		}
	}

	private void drawPlaceholderCube(LogisticsTileGenericPipe tileentity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		com.mojang.blaze3d.vertex.VertexConsumer vc = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.solid());
		org.joml.Matrix4f m = poseStack.last().pose();
		org.joml.Matrix3f n = poseStack.last().normal();

		// Core cube 6/16..10/16 — visual placeholder centered in the block.
		float a = 0.375f, b = 0.625f;
		int pipeHash = tileentity.pipe.getClass().getName().hashCode();
		int r = 64 + ((pipeHash >>> 16) & 0x7F);
		int g = 64 + ((pipeHash >>> 8) & 0x7F);
		int bl = 64 + (pipeHash & 0x7F);
		emitBox(vc, m, n, a, a, a, b, b, b, r, g, bl, packedLight, packedOverlay);

		// Connection stubs on each connected side.
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
				emitBox(vc, m, n, x0, y0, z0, x1, y1, z1, r, g, bl, packedLight, packedOverlay);
			}
		}
	}

	private static void emitBox(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f m, org.joml.Matrix3f n,
			float x0, float y0, float z0, float x1, float y1, float z1,
			int r, int g, int b, int packedLight, int packedOverlay) {
		// -Y
		quad(vc, m, n, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, r, g, b, packedLight, packedOverlay);
		// +Y
		quad(vc, m, n, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, r, g, b, packedLight, packedOverlay);
		// -Z
		quad(vc, m, n, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, r, g, b, packedLight, packedOverlay);
		// +Z
		quad(vc, m, n, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, r, g, b, packedLight, packedOverlay);
		// -X
		quad(vc, m, n, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, r, g, b, packedLight, packedOverlay);
		// +X
		quad(vc, m, n, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, r, g, b, packedLight, packedOverlay);
	}

	private static void quad(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f m, org.joml.Matrix3f n,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4,
			float nx, float ny, float nz, int r, int g, int b, int packedLight, int packedOverlay) {
		vc.vertex(m, x1, y1, z1).color(r, g, b, 255).uv(0, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(n, nx, ny, nz).endVertex();
		vc.vertex(m, x2, y2, z2).color(r, g, b, 255).uv(1, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(n, nx, ny, nz).endVertex();
		vc.vertex(m, x3, y3, z3).color(r, g, b, 255).uv(1, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(n, nx, ny, nz).endVertex();
		vc.vertex(m, x4, y4, z4).color(r, g, b, 255).uv(0, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(n, nx, ny, nz).endVertex();
	}

	private void renderInternal(@Nullable LogisticsTileGenericPipe tileentity, double x, double y, double z, float partialTicks, int destroyStage, float alpha,
			PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		boolean inHand = (tileentity == null || (x == 0 && y == 0 && z == 0));
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
					renderPipeSigns((CoreRoutedPipe) tileentity.pipe, x, y, z, partialTicks, poseStack, bufferSource, packedLight, packedOverlay);
				}
			}

			double distance = !inHand ? new DoubleCoordinates((BlockEntity) tileentity).distanceTo(new DoubleCoordinates(Minecraft.getInstance().player)) : 0;

			if (SimpleServiceLocator.cclProxy.isActivated()) {
				// Refresh the bound pose matrices to the current PoseStack top so that
				// any pipe-geometry emission in LogisticsNewRenderPipe lands at the block.
				if (SimpleServiceLocator.cclProxy.getRenderState() instanceof logisticspipes.proxy.object3d.impl.LPRenderStateImpl) {
					logisticspipes.proxy.object3d.impl.LPRenderStateImpl rs =
						(logisticspipes.proxy.object3d.impl.LPRenderStateImpl) SimpleServiceLocator.cclProxy.getRenderState();
					rs.pose = poseStack.last().pose();
					rs.normal = poseStack.last().normal();
				}
				LogisticsRenderPipe.secondRenderer.renderTileEntityAt(tileentity, x, y, z, partialTicks, distance);
			}

			if (!inHand && !tileentity.isOpaque() && SimpleServiceLocator.cclProxy.isActivated()) {
				if (tileentity.pipe.transport instanceof PipeFluidTransportLogistics) {
					//renderFluids(pipe.pipe, x, y, z);
				}
				if (tileentity.pipe.transport != null) {
					try {
						renderSolids(tileentity.pipe, x, y, z, partialTicks, poseStack, bufferSource, packedLight, packedOverlay);
					} catch (Throwable t) {
						// Item-in-transit rendering depends on ItemStackRenderer which still has TODOs;
						// swallow failures so a broken item render doesn't hide the whole pipe.
					}
				}
			}
		} finally {
			poseStack.popPose();
		}

		if (!inHand) {
			SimpleServiceLocator.mcmpProxy.renderTileEntitySpecialRenderer(tileentity, x, y, z, partialTicks, destroyStage, alpha);
		}
	}

	private void renderSolids(CoreUnroutedPipe pipe, double x, double y, double z, float partialTickTime,
			PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
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
			doRenderItem(stack, pipe.container.getWorld(), lX + pos.getXCoord(), lY + pos.getYCoord(), lZ + pos.getZCoord(), light, 0.75F, boxScale, itemYaw, itemPitch, itemYawForPitch, partialTickTime, poseStack, bufferSource, packedLight, packedOverlay);
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
			doRenderItem(stack, pipe.container.getWorld(), x + pos.getXCoord(), y + pos.getYCoord(), z + pos.getZCoord(), light, 0.25F, 0, 0, 0, 0, partialTickTime, poseStack, bufferSource, packedLight, packedOverlay);
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
			PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		LogisticsRenderPipe.boxRenderer.doRenderItem(itemstack, light, x, y, z, boxScale, yaw, pitch, yawForPitch, poseStack);

		poseStack.pushPose();
		poseStack.translate(x, y, z);
		poseStack.scale(renderScale, renderScale, renderScale);
		// Historic order: yaw around Y, then pitch around X after a secondary yawForPitch
		// rotation, matching the 1.12.2 glRotated sequence in CoreRoutedPipe-driven items.
		poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(yaw)));
		poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(yawForPitch)));
		poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.toRadians(pitch)));
		poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(-yawForPitch)));
		poseStack.translate(0.0F, -0.35F, 0.0F);
		itemRenderer.setItemstack(itemstack).setWorld(world).setPartialTickTime(partialTickTime);
		itemRenderer.renderInWorld();
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
			PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
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
				renderSign(pipe, pair.getValue2(), partialTickTime, poseStack, bufferSource, packedLight);
				poseStack.popPose();
			}
		}
	}

	private void renderSign(CoreRoutedPipe pipe, IPipeSign type, float partialTickTime, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		// ModelSign background rendering deferred; delegate text/item rendering to the sign.
		type.render(pipe, this, poseStack, bufferSource, packedLight);
	}

	private void resetStateManager() {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
	}

	public void renderItemStackOnSign(@Nonnull ItemStack itemstack) {
		// Legacy no-arg stub — rendering deferred. Use the PoseStack overload instead.
	}

	public void renderItemStackOnSign(@Nonnull ItemStack itemstack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		if (itemstack.isEmpty()) return;
		poseStack.pushPose();
		// Position the item onto the front face of the sign and scale it down to fit.
		poseStack.translate(0.0F, 0.08F, 0.0F);
		poseStack.scale(0.45F, 0.45F, 0.45F);
		Level level = Minecraft.getInstance().level;
		Minecraft.getInstance().getItemRenderer().renderStatic(
				itemstack,
				net.minecraft.world.item.ItemDisplayContext.FIXED,
				packedLight,
				net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
				poseStack,
				bufferSource,
				level,
				0);
		poseStack.popPose();
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
