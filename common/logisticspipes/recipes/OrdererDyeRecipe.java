package logisticspipes.recipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

import logisticspipes.items.RemoteOrderer;

/**
 * Remote orderer + one dye gives a coloured orderer that keeps its pipe link
 * (LP1 ShapelessOrdererRecipe). The colour lives in CUSTOM_MODEL_DATA, 1..16.
 */
public class OrdererDyeRecipe extends CustomRecipe {

	public static final RecipeSerializer<OrdererDyeRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(OrdererDyeRecipe::new);

	public OrdererDyeRecipe(CraftingBookCategory category) {
		super(category);
	}

	/** LP1 dye order was black..white (old dye damage), so variant = 16 - DyeColor id. */
	public static int variantFor(DyeColor color) {
		return 16 - color.getId();
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return !assemble(input, null).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack orderer = ItemStack.EMPTY;
		DyeColor color = null;
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) continue;
			if (stack.getItem() instanceof RemoteOrderer) {
				if (!orderer.isEmpty()) return ItemStack.EMPTY;
				orderer = stack;
			} else {
				DyeColor c = DyeColor.getColor(stack);
				if (c == null || color != null) return ItemStack.EMPTY;
				color = c;
			}
		}
		if (orderer.isEmpty() || color == null) return ItemStack.EMPTY;
		ItemStack result = orderer.copyWithCount(1);
		result.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(variantFor(color)));
		return result;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
