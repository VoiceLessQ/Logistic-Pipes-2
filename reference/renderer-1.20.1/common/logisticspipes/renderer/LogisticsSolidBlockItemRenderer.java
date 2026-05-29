package logisticspipes.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import logisticspipes.items.LogisticsSolidBlockItem;

/**
 * BEWLR that draws an LP solid block item using the same OBJ geometry as
 * {@link LogisticsSolidBlockRenderer}. Vanilla ItemRenderer pre-applies the
 * {@code -0.5,-0.5,-0.5} centring translation before invoking this renderer, so
 * the shared draw path can be used unmodified.
 */
public class LogisticsSolidBlockItemRenderer extends BlockEntityWithoutLevelRenderer {

	private static LogisticsSolidBlockItemRenderer INSTANCE;

	public static LogisticsSolidBlockItemRenderer instance() {
		if (INSTANCE == null) {
			Minecraft mc = Minecraft.getInstance();
			INSTANCE = new LogisticsSolidBlockItemRenderer(
					mc.getBlockEntityRenderDispatcher(),
					mc.getEntityModels());
		}
		return INSTANCE;
	}

	public LogisticsSolidBlockItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
		super(dispatcher, modelSet);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay) {
		if (!(stack.getItem() instanceof LogisticsSolidBlockItem)) return;
		LogisticsSolidBlockItem item = (LogisticsSolidBlockItem) stack.getItem();
		pose.pushPose();
		try {
			LogisticsSolidBlockRenderer.renderSolid(item.getType(), pose, buffers, light, overlay);
		} finally {
			pose.popPose();
		}
	}
}
