package logisticspipes.recipes;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import logisticspipes.items.RemoteOrderer;
import logisticspipes.utils.item.CustomDataUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Remote orderer + one dye gives a coloured orderer that keeps its pipe link
 * (LP1 ShapelessOrdererRecipe). The colour lives in the CustomModelData tag, 1..16.
 */
public class OrdererDyeRecipe extends CustomRecipe {

	public static final String COLOR_TAG = "CustomModelData";

	public static final OrdererDyeRecipe INSTANCE = new OrdererDyeRecipe();
	public static final RecipeSerializer<OrdererDyeRecipe> SERIALIZER = new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

	/** LP1 dye order was black..white (old dye damage), so variant = 16 - DyeColor id. */
	public static int variantFor(DyeColor color) {
		return 16 - color.getId();
	}

	/** 0 for an uncoloured orderer. */
	public static int getVariant(ItemStack stack) {
		return CustomDataUtil.getOrCreateTag(stack).getIntOr(COLOR_TAG, 0);
	}

	@Nullable
	private static DyeColor dyeColor(ItemStack stack) {
		for (DyeColor color : DyeColor.values()) {
			TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyes/" + color.getName()));
			if (stack.is(tag)) return color;
		}
		return null;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return !assemble(input).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack orderer = ItemStack.EMPTY;
		DyeColor color = null;
		for (int i = 0; i < input.size(); i++) {
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
		int variant = variantFor(color);
		CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.putInt(COLOR_TAG, variant));
		return result;
	}

	@Override
	public RecipeSerializer<OrdererDyeRecipe> getSerializer() {
		return SERIALIZER;
	}
}
