package logisticspipes.renderer;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.impl.LPRenderStateImpl;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;
import logisticspipes.renderer.newpipe.RenderEntry;
import logisticspipes.renderer.state.PipeRenderState;

/**
 * BEWLR that draws a pipe item using the same OBJ geometry pipeline as the in-world
 * {@link LogisticsRenderPipe}. Built with a fresh all-disconnected {@link PipeRenderState}
 * and the item's dummyPipe, so inventory icons show the 3D pipe body instead of a sprite.
 */
public class LogisticsPipeItemRenderer extends BlockEntityWithoutLevelRenderer {

	private static LogisticsPipeItemRenderer INSTANCE;

	public static LogisticsPipeItemRenderer instance() {
		if (INSTANCE == null) {
			Minecraft mc = Minecraft.getInstance();
			INSTANCE = new LogisticsPipeItemRenderer(
				mc.getBlockEntityRenderDispatcher(),
				mc.getEntityModels());
		}
		return INSTANCE;
	}

	public LogisticsPipeItemRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher dispatcher,
			net.minecraft.client.model.geom.EntityModelSet modelSet) {
		super(dispatcher, modelSet);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay) {
		if (!(stack.getItem() instanceof ItemLogisticsPipe)) return;
		ItemLogisticsPipe item = (ItemLogisticsPipe) stack.getItem();
		CoreUnroutedPipe dummyPipe = item.getDummyPipe();
		if (dummyPipe == null) return;
		if (!SimpleServiceLocator.cclProxy.isActivated()) return;
		if (!(SimpleServiceLocator.cclProxy.getRenderState() instanceof LPRenderStateImpl)) return;

		pose.pushPose();
		try {
			// Vanilla ItemRenderer.render already applies pose.translate(-0.5,-0.5,-0.5)
			// before calling renderByItem, which centres the [0,1] LP pipe geometry. We
			// don't translate further; the model's display transform handles rotation/scale.

			LPRenderStateImpl rs = (LPRenderStateImpl) SimpleServiceLocator.cclProxy.getRenderState();
			VertexConsumer buffer = buffers.getBuffer(RenderType.cutoutMipped());
			rs.bind(buffer, pose.last().pose(), pose.last().normal(), light, overlay);
			rs.colourARGB = 0xFFFFFFFF;

			PipeRenderState renderState = new PipeRenderState();
			// Fresh ConnectionMatrix has all sides false, matching the dev-branch inventory look.
			renderState.textureMatrix.refreshStatesForItem(dummyPipe);

			List<RenderEntry> entries = new ArrayList<>();
			try {
				LogisticsNewRenderPipe.fillObjectsToRenderList(entries, dummyPipe, renderState);
			} catch (Exception e) {
				// Some pipe types (HS tubes) need orientation state that a dummy pipe lacks.
				// Skip the 3D render — the inventory will show an empty slot for now.
				return;
			}

			rs.reset();
			for (RenderEntry entry : entries) {
				entry.getModel().render(entry.getOperations());
			}
			rs.draw();
		} finally {
			pose.popPose();
		}
	}
}
