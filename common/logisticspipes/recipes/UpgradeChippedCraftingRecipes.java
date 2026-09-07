package logisticspipes.recipes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;

import logisticspipes.LPItems;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.pipes.upgrades.ActionSpeedUpgrade;
import logisticspipes.pipes.upgrades.AdvancedSatelliteUpgrade;
import logisticspipes.pipes.upgrades.CombinedSneakyUpgrade;
import logisticspipes.pipes.upgrades.ConnectionUpgradeConfig;
import logisticspipes.pipes.upgrades.CraftingByproductUpgrade;
import logisticspipes.pipes.upgrades.CraftingCleanupUpgrade;
import logisticspipes.pipes.upgrades.CraftingMonitoringUpgrade;
import logisticspipes.pipes.upgrades.FluidCraftingUpgrade;
import logisticspipes.pipes.upgrades.FuzzyUpgrade;
import logisticspipes.pipes.upgrades.ItemExtractionUpgrade;
import logisticspipes.pipes.upgrades.ItemStackExtractionUpgrade;
import logisticspipes.pipes.upgrades.OpaqueUpgrade;
import logisticspipes.pipes.upgrades.PatternUpgrade;
import logisticspipes.pipes.upgrades.PowerTransportationUpgrade;
import logisticspipes.pipes.upgrades.SneakyUpgradeConfig;
import logisticspipes.pipes.upgrades.SpeedUpgrade;
import logisticspipes.pipes.upgrades.UpgradeModuleUpgrade;

public class UpgradeChippedCraftingRecipes extends CraftingPartRecipes {

	enum RecipeType {
		LEVEL_1,
		LEVEL_2,
		LEVEL_3,
		LEVEL_4,
	}

	private void registerUpgradeRecipe(CraftingParts parts, RecipeType type, ResourceLocation recipeCategory, String upgradeName) {
		registerUpgradeRecipe(parts, type, recipeCategory, upgradeName, 1);
	}

	private void registerUpgradeRecipe(CraftingParts parts, RecipeType type, ResourceLocation recipeCategory, String upgradeName, int amount) {
		ResourceLocation upgradeResource = LPItems.upgrades.get(upgradeName);
		if (upgradeResource == null) return;
		Item upgrade = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(upgradeResource);
		if (upgrade == null) return;

		Ingredient programmer = programmerIngredient(upgradeResource.toString());
		final Set<ResourceLocation> compilerPrograms = LogisticsProgramCompilerTileEntity.programByCategory.computeIfAbsent(recipeCategory, k -> new HashSet<>());
		compilerPrograms.add(upgradeResource);

		// Beta style (pre-1.8): drop the Logistics Programmer from the top row, chip-only crafting.
		String topRow = logisticspipes.config.Configs.getBetaUpgradeRecipes() ? "r r" : "rpr";
		RecipeManager.RecipeLayout layout = null;
		switch (type) {
			case LEVEL_1:
				layout = new RecipeManager.RecipeLayout(
						topRow,
						"ibi",
						"qnq"
				);
				break;
			case LEVEL_2:
				layout = new RecipeManager.RecipeLayout(
						topRow,
						"iai",
						"qnq"
				);
				break;
			case LEVEL_3:
				layout = new RecipeManager.RecipeLayout(
						topRow,
						"gbg",
						"qnq"
				);
				break;
			case LEVEL_4:
				layout = new RecipeManager.RecipeLayout(
						topRow,
						"gag",
						"qnq"
				);
				break;
		}
		if (layout != null) {
			final RecipeManager.RecipeLayout fLayout = layout;
			List<RecipeManager.RecipeIndex> recipeIndexes = Arrays.asList(
					new RecipeManager.RecipeIndex('a', parts.getChipAdvanced()),
					new RecipeManager.RecipeIndex('b', parts.getChipBasic()),
					new RecipeManager.RecipeIndex('f', parts.getChipFpga()),
					new RecipeManager.RecipeIndex('g', "ingotGold"),
					new RecipeManager.RecipeIndex('n', "nuggetGold"),
					new RecipeManager.RecipeIndex('i', "ingotIron"),
					new RecipeManager.RecipeIndex('l', "gemLapis"),
					new RecipeManager.RecipeIndex('p', programmer),
					new RecipeManager.RecipeIndex('r', "dustRedstone"),
					new RecipeManager.RecipeIndex('q', "paper"),
					new RecipeManager.RecipeIndex('z', Items.BLAZE_POWDER));
			LinkedList<Object> indexToUse = recipeIndexes.stream()
					.filter(recipeIndex -> !(fLayout.getLine1() + fLayout.getLine2() + fLayout.getLine3()).replace(recipeIndex.getIndex(), ' ')
							.equals((fLayout.getLine1() + fLayout.getLine2() + fLayout.getLine3()))).collect(Collectors.toCollection(LinkedList::new));
			indexToUse.addFirst(layout);
			RecipeManager.craftingManager.addRecipe(new ItemStack(upgrade, amount), indexToUse.toArray());
		}
	}

	@Override
	protected void loadRecipes(CraftingParts parts) {
		registerUpgradeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, SneakyUpgradeConfig.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, SpeedUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_4, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, CombinedSneakyUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, ConnectionUpgradeConfig.getName(), 8);
		registerUpgradeRecipe(parts, RecipeType.LEVEL_2, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, AdvancedSatelliteUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, FluidCraftingUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, CraftingByproductUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_4, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, PatternUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, FuzzyUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, PowerTransportationUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_4, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, CraftingMonitoringUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, OpaqueUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_2, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, CraftingCleanupUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_4, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_3, UpgradeModuleUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_3, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, ActionSpeedUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_3, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, ItemExtractionUpgrade.getName());
		registerUpgradeRecipe(parts, RecipeType.LEVEL_4, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, ItemStackExtractionUpgrade.getName());
	}

}
