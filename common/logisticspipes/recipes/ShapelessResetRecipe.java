package logisticspipes.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import logisticspipes.LPConstants;

/**
 * A crafting recipe that produces a clean (NBT-stripped) copy of a specific item.
 * Matches a crafting grid holding only the target item, one or more slots.
 * Used to reset module/orderer state.
 */
public class ShapelessResetRecipe extends CustomRecipe {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "reset");

	public static final MapCodec<ShapelessResetRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(r -> r.targetItem)
	).apply(i, ShapelessResetRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessResetRecipe> STREAM_CODEC =
			ByteBufCodecs.registry(Registries.ITEM).map(ShapelessResetRecipe::new, r -> r.targetItem);
	public static final RecipeSerializer<ShapelessResetRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final Item targetItem;

	public ShapelessResetRecipe(Item targetItem) {
		this.targetItem = targetItem;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		ItemStack first = ItemStack.EMPTY;
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) continue;
			if (stack.getItem() != targetItem) return false;
			// LP1 matched one meta per recipe: no mixing orderer colours
			if (first.isEmpty()) first = stack;
			else if (OrdererDyeRecipe.getVariant(first) != OrdererDyeRecipe.getVariant(stack)) return false;
		}
		return !first.isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		// LP1: one clean item per matching stack in the grid
		int count = 0;
		int variant = 0;
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) continue;
			if (count++ == 0) variant = OrdererDyeRecipe.getVariant(stack);
		}
		ItemStack result = new ItemStack(targetItem, count);
		// keeps the orderer colour, like LP1's meta-preserving reset
		if (variant != 0) {
			int color = variant;
			CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.putInt(OrdererDyeRecipe.COLOR_TAG, color));
		}
		return result;
	}

	@Override
	public RecipeSerializer<ShapelessResetRecipe> getSerializer() {
		return SERIALIZER;
	}
}
