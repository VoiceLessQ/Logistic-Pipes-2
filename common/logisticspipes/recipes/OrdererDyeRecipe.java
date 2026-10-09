package logisticspipes.recipes;

import javax.annotation.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

import logisticspipes.items.RemoteOrderer;

/**
 * Remote orderer + one dye gives a coloured orderer that keeps its pipe link
 * (LP1 ShapelessOrdererRecipe). The colour lives in the CustomModelData tag, 1..16.
 */
public class OrdererDyeRecipe extends CustomRecipe {

	public static final String COLOR_TAG = "CustomModelData";

	public static final RecipeSerializer<OrdererDyeRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(OrdererDyeRecipe::new);

	public OrdererDyeRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	/** LP1 dye order was black..white (old dye damage), so variant = 16 - DyeColor id. */
	public static int variantFor(DyeColor color) {
		return 16 - color.getId();
	}

	/** 0 for an uncoloured orderer. */
	public static int getVariant(ItemStack stack) {
		return stack.getTag() != null ? stack.getTag().getInt(COLOR_TAG) : 0;
	}

	@Nullable
	private static DyeColor dyeColor(ItemStack stack) {
		for (DyeColor color : DyeColor.values()) {
			TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "dyes/" + color.getName()));
			if (stack.is(tag)) return color;
		}
		return null;
	}

	@Override
	public boolean matches(CraftingContainer input, Level level) {
		return !assemble(input, null).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
		ItemStack orderer = ItemStack.EMPTY;
		DyeColor color = null;
		for (int i = 0; i < input.getContainerSize(); i++) {
			ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) continue;
			if (stack.getItem() instanceof RemoteOrderer) {
				if (!orderer.isEmpty()) return ItemStack.EMPTY;
				orderer = stack;
			} else {
				DyeColor c = dyeColor(stack);
				if (c == null || color != null) return ItemStack.EMPTY;
				color = c;
			}
		}
		if (orderer.isEmpty() || color == null) return ItemStack.EMPTY;
		ItemStack result = orderer.copyWithCount(1);
		result.getOrCreateTag().putInt(COLOR_TAG, variantFor(color));
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
