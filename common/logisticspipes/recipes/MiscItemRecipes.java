package logisticspipes.recipes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import logisticspipes.LPItems;
import logisticspipes.LPRegistries;

/**
 * Recipes LP 0.9 had in its soldering station, IC2 proxy and CC proxy (LP 0.10+ and LP2 dropped them).
 * The four LP 0.9 part variants are one {@code parts} item now; the diamond chip is the advanced chip.
 */
public class MiscItemRecipes extends CraftingPartRecipes {

	@Override
	protected void loadRecipes(CraftingParts parts) {
		RecipeManager.craftingManager.addRecipe(LPRegistries.PARTS.get(), 1,
				new RecipeManager.RecipeLayout(
						"rCr",
						"rGr",
						"rrr"
				),
				new RecipeManager.RecipeIndex('r', "dustRedstone"),
				new RecipeManager.RecipeIndex('C', parts.getChipAdvanced()),
				new RecipeManager.RecipeIndex('G', Items.GLASS));

		RecipeManager.craftingManager.addRecipe(LPItems.hudGlasses.get(), 1,
				new RecipeManager.RecipeLayoutSmall(
						"D D",
						"PCP"
				),
				new RecipeManager.RecipeIndex('D', "gemDiamond"),
				new RecipeManager.RecipeIndex('P', LPRegistries.PARTS.get()),
				new RecipeManager.RecipeIndex('C', parts.getChipAdvanced()));

		RecipeManager.craftingManager.addRecipe(LPItems.itemCard.get(), 2,
				new RecipeManager.RecipeLayoutSmall(
						"C C",
						"MEM"
				),
				new RecipeManager.RecipeIndex('C', parts.getChipAdvanced()),
				new RecipeManager.RecipeIndex('M', LPItems.blankModule.get()),
				new RecipeManager.RecipeIndex('E', Items.ENDER_PEARL));

		// Tech Reborn and IC2 Reborn share the upgrade item names; a recipe is skipped when its mod is absent
		for (String mod : new String[] { "techreborn", "ic2reborn" }) {
			Item storageUpgrade = modItem(mod + ":energy_storage_upgrade");
			Item overclocker = modItem(mod + ":overclocker_upgrade");
			Item transformer = modItem(mod + ":transformer_upgrade");
			addEURecipe(LPRegistries.ITEM_POWER_PROVIDER_EU.get(), Items.REDSTONE_BLOCK, storageUpgrade, overclocker, transformer);
			addEURecipe(LPRegistries.UPGRADE_POWER_EU_LV.get(), LPRegistries.UPGRADE_POWER_TRANSPORTATION.get(), storageUpgrade, overclocker, transformer);
			addEURecipe(LPRegistries.UPGRADE_POWER_EU_MV.get(), LPRegistries.UPGRADE_POWER_EU_LV.get(), storageUpgrade, overclocker, transformer);
			addEURecipe(LPRegistries.UPGRADE_POWER_EU_HV.get(), LPRegistries.UPGRADE_POWER_EU_MV.get(), storageUpgrade, overclocker, transformer);
			addEURecipe(LPRegistries.UPGRADE_POWER_EU_EV.get(), LPRegistries.UPGRADE_POWER_EU_HV.get(), storageUpgrade, overclocker, transformer);
		}

		RecipeManager.craftingManager.addRecipe(LPRegistries.UPGRADE_CC_REMOTE_CONTROL.get(), 1,
				new RecipeManager.RecipeLayout(
						"rTr",
						"WCM",
						"rKr"
				),
				new RecipeManager.RecipeIndex('r', "dustRedstone"),
				new RecipeManager.RecipeIndex('T', Items.REDSTONE_TORCH),
				new RecipeManager.RecipeIndex('C', parts.getChipAdvanced()),
				new RecipeManager.RecipeIndex('W', modItem("computercraft:wireless_modem_normal")),
				new RecipeManager.RecipeIndex('M', modItem("computercraft:wired_modem")),
				new RecipeManager.RecipeIndex('K', modItem("computercraft:cable")));
	}

	private static void addEURecipe(Item result, Item center, Item storageUpgrade, Item overclocker, Item transformer) {
		RecipeManager.craftingManager.addRecipe(result, 1,
				new RecipeManager.RecipeLayout(
						"PSP",
						"OBO",
						"PTP"
				),
				new RecipeManager.RecipeIndex('P', "paper"),
				new RecipeManager.RecipeIndex('B', center),
				new RecipeManager.RecipeIndex('S', storageUpgrade),
				new RecipeManager.RecipeIndex('O', overclocker),
				new RecipeManager.RecipeIndex('T', transformer));
	}

	/** The item if its mod is loaded, else null, which makes addRecipe skip the recipe. */
	private static Item modItem(String id) {
		return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(null);
	}
}
