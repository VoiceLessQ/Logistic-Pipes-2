package logisticspipes.renderer;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;

import logisticspipes.items.LogisticsSolidBlockItem;

/**
 * Special item model that draws an LP solid block item using the same OBJ geometry as
 * {@link LogisticsSolidBlockRenderer}.
 */
public class LogisticsSolidBlockItemRenderer implements SpecialModelRenderer<ItemStack> {

	public static final LogisticsSolidBlockItemRenderer INSTANCE = new LogisticsSolidBlockItemRenderer();

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
		if (stack == null || !(stack.getItem() instanceof LogisticsSolidBlockItem)) return;
		LogisticsSolidBlockRenderer.renderSolid(((LogisticsSolidBlockItem) stack.getItem()).getType(), pose, collector, light, overlay);
	}
}
