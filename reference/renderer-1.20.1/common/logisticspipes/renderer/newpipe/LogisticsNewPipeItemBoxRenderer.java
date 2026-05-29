package logisticspipes.renderer.newpipe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;



import logisticspipes.items.LogisticsFluidContainer;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;

public class LogisticsNewPipeItemBoxRenderer {

	private static final int RENDER_SIZE = 40;

	private int renderList = -1;
	private static final ResourceLocation BLOCKS = new ResourceLocation("textures/atlas/blocks.png");
	private static final Map<FluidIdentifier, int[]> renderLists = new HashMap<>();

	@OnlyIn(Dist.CLIENT)
	public void doRenderItem(@Nonnull ItemStack itemstack, float light, double x, double y, double z, double boxScale, double yaw, double pitch, double yawForPitch, PoseStack poseStack) {
		if (LogisticsNewRenderPipe.innerTransportBox == null) return;
		poseStack.pushPose();

		// 1.20.1: display-list caching is gone — the transport box is emitted directly
		// through the currently-bound VertexConsumer each frame (same pattern as
		// LogisticsNewRenderPipe.renderList). The old renderList sentinel is kept only
		// to preserve the one-time reset semantics.
		poseStack.translate(x, y, z);
		poseStack.scale((float) boxScale, (float) boxScale, (float) boxScale);
		poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(yaw)));
		poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(yawForPitch)));
		poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.toRadians(pitch)));
		poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(-yawForPitch)));
		poseStack.translate(-0.5, -0.5, -0.5);

		// Rebind the render state pose to the current PoseStack top so the transport
		// box geometry below lands at the transformed position.
		if (SimpleServiceLocator.cclProxy.getRenderState() instanceof logisticspipes.proxy.object3d.impl.LPRenderStateImpl) {
			logisticspipes.proxy.object3d.impl.LPRenderStateImpl rs =
				(logisticspipes.proxy.object3d.impl.LPRenderStateImpl) SimpleServiceLocator.cclProxy.getRenderState();
			rs.pose = poseStack.last().pose();
			rs.normal = poseStack.last().normal();
		}
		SimpleServiceLocator.cclProxy.getRenderState().reset();
		LogisticsNewRenderPipe.innerTransportBox.render(LogisticsNewRenderPipe.innerBoxTexture);
		SimpleServiceLocator.cclProxy.getRenderState().draw();

		if (!itemstack.isEmpty() && itemstack.getItem() instanceof LogisticsFluidContainer) {
			FluidIdentifierStack f = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(ItemIdentifierStack.getFromStack(itemstack));
			if (f != null) {
				/*
				FluidContainerRenderer.skipNext = true;
				int list = getRenderListFor(f);
				// TODO: glPushAttrib removed
				// TODO: GL11.glEnable(GL11.GL_CULL_FACE) → RenderSystem equivalent
				// GL_LIGHTING removed — use shaders
				RenderSystem.enableBlend();
				// TODO: glBlendFunc → RenderSystem.blendFunc()

				// TODO: glCallList removed — use vertex buffer rendering
				// TODO: glPopAttrib removed
				*/
			}
		}

		poseStack.popPose();
	}
/*
	private int getRenderListFor(FluidStack fluid) {
		FluidIdentifier ident = FluidIdentifier.get(fluid);
		int[] array = LogisticsNewPipeItemBoxRenderer.renderLists.get(fluid);
		if (array == null) {
			array = new int[LogisticsNewPipeItemBoxRenderer.RENDER_SIZE];
			LogisticsNewPipeItemBoxRenderer.renderLists.put(ident, array);
		}
		int pos = Math.min((int) (((Math.min(fluid.amount, 5000) * 1.0F) * LogisticsNewPipeItemBoxRenderer.RENDER_SIZE) / 5000), LogisticsNewPipeItemBoxRenderer.RENDER_SIZE - 1);
		if (array[pos] != 0) {
			return array[pos];
		}
		RenderInfo block = new RenderInfo();

		block.baseBlock = fluid.getFluid().getBlock();
		block.texture = fluid.getFluid().getStillIcon();

		float ratio = pos * 1.0F / (LogisticsNewPipeItemBoxRenderer.RENDER_SIZE - 1);

		// CENTER HORIZONTAL

		array[pos] = 0;
		// TODO: glNewList removed — use vertex buffer rendering

		block.minX = 0.32;
		block.maxX = 0.68;

		block.minY = 0.32;
		block.maxY = 0.32 + (0.68 - 0.32) * ratio;

		block.minZ = 0.32;
		block.maxZ = 0.68;

		CustomBlockRenderer.INSTANCE.renderBlock(block, Minecraft.getInstance().theWorld, 0, 0, 0, false, true);

		// TODO: glEndList removed
		return array[pos];
	}*/
}
