package logisticspipes.recipes;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundTag;

import logisticspipes.LPItems;
import logisticspipes.items.ItemLogisticsProgrammer;

public abstract class CraftingPartRecipes implements IRecipeProvider {

	private static List<CraftingParts> craftingPartList = null;

	public static List<CraftingParts> getCraftingPartList() {
		if (craftingPartList == null) {
			craftingPartList = new ArrayList<>();
			/*
			CraftingParts parts = SimpleServiceLocator.buildCraftProxy.getRecipeParts();
			// NO BC => NO RECIPES (for now)
			if (parts != null) {
				SimpleServiceLocator.IC2Proxy.addCraftingRecipes(parts);
				SimpleServiceLocator.thaumCraftProxy.addCraftingRecipes(parts);
				SimpleServiceLocator.ccProxy.addCraftingRecipes(parts);
				SimpleServiceLocator.buildCraftProxy.addCraftingRecipes(parts);

				RecipeManager.loadRecipes();
			}
			*/

			if (true) { // TODO: Add Config Option
				craftingPartList.add(new CraftingParts(
						new ItemStack(LPItems.chipFPGA.get(), 1),
						new ItemStack(LPItems.chipBasic.get(), 1),
						new ItemStack(LPItems.chipAdvanced.get(), 1)));
			}
		}

		return craftingPartList;
	}

	@Override
	public final void loadRecipes() {
		getCraftingPartList().forEach(this::loadRecipes);
	}

	@Nonnull
	protected Ingredient programmerIngredient(String recipeTarget) {
		ItemStack programmerStack = new ItemStack(LPItems.logisticsProgrammer.get());
		final CompoundTag tag = new CompoundTag();
		tag.putString(ItemLogisticsProgrammer.RECIPE_TARGET, recipeTarget);
		logisticspipes.utils.item.StackTag.setTag(programmerStack, tag);
		return NBTIngredient.fromStacks(programmerStack);
	}

	protected abstract void loadRecipes(CraftingParts parts);

}
