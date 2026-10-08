package logisticspipes.recipes;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import logisticspipes.LPItems;
import logisticspipes.items.ItemModule;
import logisticspipes.modules.LogisticsModule;

public class CraftingRecipes implements IRecipeProvider {

	@Override
	public void loadRecipes() {
		registerResetRecipes();
	}

	private void registerResetRecipes() {
		for (ResourceLocation moduleResource : LPItems.modules.values()) {
			final Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(moduleResource);
			if (item instanceof ItemModule) {
				LogisticsModule module = ((ItemModule) item).getModuleForItem(new ItemStack(item), null, null, null);
				if (module == null) continue;
				CompoundTag tag = new CompoundTag();
				module.writeToNBT(tag);
				if (!tag.isEmpty()) {
					RecipeManager.craftingManager.addShapelessResetRecipe(item, 0);
				}
			}
		}

		// Orderer colours (LP1 meta 1..16) are CUSTOM_MODEL_DATA, dyed via OrdererDyeRecipe; reset keeps the colour
		RecipeManager.craftingManager.addShapelessResetRecipe(LPItems.remoteOrderer.get(), 0);
	}
}
