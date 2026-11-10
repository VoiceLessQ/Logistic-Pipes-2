package logisticspipes.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;

import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;
import logisticspipes.renderer.newpipe.RenderEntry;
import logisticspipes.renderer.state.PipeRenderState;

/**
 * Special item model that draws a pipe item using the same OBJ geometry pipeline as the in-world
 * {@link LogisticsRenderPipe}. Built with a fresh all-disconnected {@link PipeRenderState}
 * and the item's dummyPipe, so inventory icons show the 3D pipe body instead of a sprite.
 */
public class LogisticsPipeItemRenderer implements SpecialModelRenderer<ItemStack> {

	public static final LogisticsPipeItemRenderer INSTANCE = new LogisticsPipeItemRenderer();

	public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {

		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext context) {
			return INSTANCE;
		}

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}
	}

	@Override
	public ItemStack extractArgument(ItemStack stack) {
		return stack;
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
		output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
	}

	@Override
	public void submit(ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
		if (stack == null || !(stack.getItem() instanceof ItemLogisticsPipe)) return;
		CoreUnroutedPipe dummyPipe = ((ItemLogisticsPipe) stack.getItem()).getDummyPipe();
		if (dummyPipe == null) return;
		if (!SimpleServiceLocator.cclProxy.isActivated()) return;

		PipeRenderState renderState = new PipeRenderState();
		// Fresh ConnectionMatrix has all sides false, matching the dev-branch inventory look.
		renderState.textureMatrix.refreshStatesForItem(dummyPipe);

		List<RenderEntry> entries = new ArrayList<>();
		try {
			LogisticsNewRenderPipe.fillObjectsToRenderList(entries, dummyPipe, renderState);
		} catch (Exception e) {
			// HS tubes need orientation state a dummy pipe lacks; the slot stays empty
			return;
		}

		// the item transform already centres the [0,1] geometry (ItemTransform.apply ends with translate(-0.5))
		LogisticsRenderPipe.submitGeometry(collector, pose, LogisticsRenderPipe.ATLAS_TYPE, light, overlay,
				() -> entries.forEach(entry -> entry.getModel().render(entry.getOperations())));
	}
}
