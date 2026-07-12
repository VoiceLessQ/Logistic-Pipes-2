package logisticspipes.recipes;

import com.google.gson.JsonObject;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
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

	public static final RecipeSerializer<ShapelessResetRecipe> SERIALIZER = new RecipeSerializer<>() {

		@Override
		public ShapelessResetRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
			Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(json.get("item").getAsString()));
			return new ShapelessResetRecipe(recipeId, CraftingBookCategory.MISC, item);
		}

		@Override
		public ShapelessResetRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
			Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(buf.readResourceLocation());
			return new ShapelessResetRecipe(recipeId, CraftingBookCategory.MISC, item);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, ShapelessResetRecipe recipe) {
			buf.writeResourceLocation(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(recipe.targetItem));
		}
	};

	private final Item targetItem;

	public ShapelessResetRecipe(ResourceLocation id, CraftingBookCategory category, Item targetItem) {
		super(id, category);
		this.targetItem = targetItem;
	}

	@Override
	public boolean matches(CraftingContainer input, Level level) {
		boolean found = false;
		for (int i = 0; i < input.getContainerSize(); i++) {
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
	public ItemStack assemble(CraftingContainer input, RegistryAccess registry) {
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
