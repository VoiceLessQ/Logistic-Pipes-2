package logisticspipes.recipes;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import logisticspipes.LPConstants;

/**
 * A crafting recipe that produces a clean (NBT-stripped) copy of a specific item.
 * Matches a crafting grid containing exactly one instance of the target item.
 * Used to reset module/orderer state.
 */
public class ShapelessResetRecipe extends CustomRecipe {

	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "reset");

	public static final MapCodec<ShapelessResetRecipe> CODEC = net.minecraft.core.registries.BuiltInRegistries.ITEM
			.byNameCodec().fieldOf("item")
			.xmap(item -> new ShapelessResetRecipe(CraftingBookCategory.MISC, item), recipe -> recipe.targetItem);

	public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessResetRecipe> STREAM_CODEC =
			net.minecraft.network.codec.ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM)
					.map(item -> new ShapelessResetRecipe(CraftingBookCategory.MISC, item),
							recipe -> recipe.targetItem);

	public static final RecipeSerializer<ShapelessResetRecipe> SERIALIZER = new RecipeSerializer<>() {

		@Override
		public MapCodec<ShapelessResetRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ShapelessResetRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	};

	private final Item targetItem;

	public ShapelessResetRecipe(CraftingBookCategory category, Item targetItem) {
		super(category);
		this.targetItem = targetItem;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		boolean found = false;
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (!stack.isEmpty()) {
				if (stack.getItem() == targetItem) {
					if (found) return false; // only one allowed
					found = true;
				} else {
					return false; // no other items allowed
				}
			}
		}
		return found;
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		return new ItemStack(targetItem);
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
