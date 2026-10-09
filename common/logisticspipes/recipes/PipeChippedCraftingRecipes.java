package logisticspipes.recipes;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import logisticspipes.LPItems;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.items.ItemLogisticsProgrammer;

public class PipeChippedCraftingRecipes extends CraftingPartRecipes {

	enum RecipeType {
		LEVEL_1,
		LEVEL_2,
		LEVEL_3,
		ENDER_1,
		ENDER_2,
	}

	private void registerPipeRecipeCategory(ResourceLocation recipeCategory, Item targetPipe) {
		if (!LogisticsProgramCompilerTileEntity.programByCategory.containsKey(recipeCategory)) {
			LogisticsProgramCompilerTileEntity.programByCategory.put(recipeCategory, new HashSet<>());
		}
		LogisticsProgramCompilerTileEntity.programByCategory.get(recipeCategory).add(BuiltInRegistries.ITEM.getKey(targetPipe));
	}

	private void registerPipeRecipe(CraftingParts parts, RecipeType type, ResourceLocation recipeCategory, Item targetPipe, Item basePipe) {
		Ingredient programmer = getIngredientForProgrammer(targetPipe);

		registerPipeRecipeCategory(recipeCategory, targetPipe);

		RecipeManager.RecipeLayout layout = null;
		switch (type) {
			case LEVEL_1:
				layout = new RecipeManager.RecipeLayout(
						" p ",
						"rfr",
						" s "
				);
				break;
			case LEVEL_2:
				layout = new RecipeManager.RecipeLayout(
						" p ",
						"rbr",
						"isi"
				);
				break;
			case LEVEL_3:
				layout = new RecipeManager.RecipeLayout(
						" p ",
						"rar",
						"gsg"
				);
				break;
			case ENDER_1:
				layout = new RecipeManager.RecipeLayout(
						" p ",
						"ebr",
						"isi"
				);
				break;
			case ENDER_2:
				layout = new RecipeManager.RecipeLayout(
						" p ",
						"ear",
						"isi"
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
					new RecipeManager.RecipeIndex('s', basePipe),
					new RecipeManager.RecipeIndex('z', Items.BLAZE_POWDER),
					new RecipeManager.RecipeIndex('e', Items.ENDER_PEARL));
			LinkedList<Object> indexToUse = recipeIndexes.stream()
					.filter(recipeIndex -> !(fLayout.getLine1() + fLayout.getLine2() + fLayout.getLine3()).replace(recipeIndex.getIndex(), ' ')
							.equals((fLayout.getLine1() + fLayout.getLine2() + fLayout.getLine3()))).collect(Collectors.toCollection(LinkedList::new));
			indexToUse.addFirst(layout);
			RecipeManager.craftingManager.addRecipe(new ItemStack(targetPipe), indexToUse.toArray());
		}
	}

	private Ingredient getIngredientForProgrammer(Item targetPipe) {
		ItemStack programmerStack = new ItemStack(LPItems.logisticsProgrammer.get());
		programmerStack.setTag(new CompoundTag());
		programmerStack.getTag().putString(ItemLogisticsProgrammer.RECIPE_TARGET, BuiltInRegistries.ITEM.getKey(targetPipe).toString());
		return NBTIngredient.fromStacks(programmerStack);
	}

	@Override
	protected void loadRecipes(CraftingParts parts) {
		registerPipeRecipe(parts, RecipeType.LEVEL_2, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, LPItems.pipeRequest.get(), LPItems.pipeBasic.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_2, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, LPItems.pipeProvider.get(), LPItems.pipeBasic.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, LPItems.pipeCrafting.get(), LPItems.pipeBasic.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, LPItems.pipeSatellite.get(), LPItems.pipeBasic.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_2, LogisticsProgramCompilerTileEntity.ProgrammCategories.BASIC, LPItems.pipeSupplier.get(), LPItems.pipeBasic.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_3, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, LPItems.pipeRequestMk2.get(), LPItems.pipeRequest.get());
		registerPipeRecipe(parts, RecipeType.ENDER_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, LPItems.pipeRemoteOrderer.get(), LPItems.pipeBasic.get());
		registerPipeRecipe(parts, RecipeType.ENDER_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_3, LPItems.pipeInvSystemConnector.get(), LPItems.pipeBasic.get());

		registerPipeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, LPItems.pipeSystemEntrance.get(), LPItems.pipeProvider.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, LPItems.pipeSystemDestination.get(), LPItems.pipeProvider.get());
		registerPipeRecipe(parts, RecipeType.ENDER_2, LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_3, LPItems.pipeFirewall.get(), LPItems.pipeBasic.get());

		registerPipeRecipeCategory(LogisticsProgramCompilerTileEntity.ProgrammCategories.CHASSIS, LPItems.pipeChassisMk1.get());
		RecipeManager.craftingManager.addRecipe(new ItemStack(LPItems.pipeChassisMk1.get()),
				new RecipeManager.RecipeLayout(
						" p ",
						" b ",
						"fsf"
				),
				new RecipeManager.RecipeIndex('b', parts.getChipBasic()),
				new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(LPItems.pipeChassisMk1.get())),
				new RecipeManager.RecipeIndex('s', LPItems.pipeBasic.get()),
				new RecipeManager.RecipeIndex('f', parts.getChipFpga())
		);

		registerPipeRecipeCategory(LogisticsProgramCompilerTileEntity.ProgrammCategories.CHASSIS, LPItems.pipeChassisMk2.get());
		RecipeManager.craftingManager.addRecipe(new ItemStack(LPItems.pipeChassisMk2.get()),
				new RecipeManager.RecipeLayout(
						" p ",
						"bsb",
						"ili"
				),
				new RecipeManager.RecipeIndex('b', parts.getChipBasic()),
				new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(LPItems.pipeChassisMk2.get())),
				new RecipeManager.RecipeIndex('s', LPItems.pipeChassisMk1.get()),
				new RecipeManager.RecipeIndex('l', "gemLapis"),
				new RecipeManager.RecipeIndex('i', "ingotIron")
		);

		registerPipeRecipeCategory(LogisticsProgramCompilerTileEntity.ProgrammCategories.CHASSIS, LPItems.pipeChassisMk3.get());
		RecipeManager.craftingManager.addRecipe(new ItemStack(LPItems.pipeChassisMk3.get()),
				new RecipeManager.RecipeLayout(
						" p ",
						"gsg",
						"iai"
				),
				new RecipeManager.RecipeIndex('a', parts.getChipAdvanced()),
				new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(LPItems.pipeChassisMk3.get())),
				new RecipeManager.RecipeIndex('s', LPItems.pipeChassisMk2.get()),
				new RecipeManager.RecipeIndex('g', "dustGlowstone"),
				new RecipeManager.RecipeIndex('i', "ingotIron")
		);

		registerPipeRecipeCategory(LogisticsProgramCompilerTileEntity.ProgrammCategories.CHASSIS, LPItems.pipeChassisMk4.get());
		RecipeManager.craftingManager.addRecipe(new ItemStack(LPItems.pipeChassisMk4.get()),
				new RecipeManager.RecipeLayout(
						" p ",
						"bsb",
						"gag"
				),
				new RecipeManager.RecipeIndex('a', parts.getChipAdvanced()),
				new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(LPItems.pipeChassisMk4.get())),
				new RecipeManager.RecipeIndex('s', LPItems.pipeChassisMk3.get()),
				new RecipeManager.RecipeIndex('b', parts.getChipBasic()),
				new RecipeManager.RecipeIndex('g', "ingotGold")
		);

		registerPipeRecipeCategory(LogisticsProgramCompilerTileEntity.ProgrammCategories.CHASSIS, LPItems.pipeChassisMk5.get());
		RecipeManager.craftingManager.addRecipe(new ItemStack(LPItems.pipeChassisMk5.get()),
				new RecipeManager.RecipeLayout(
						" p ",
						"asa",
						"dnd"
				),
				new RecipeManager.RecipeIndex('a', parts.getChipAdvanced()),
				new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(LPItems.pipeChassisMk5.get())),
				new RecipeManager.RecipeIndex('s', LPItems.pipeChassisMk4.get()),
				new RecipeManager.RecipeIndex('d', "gemDiamond"),
				new RecipeManager.RecipeIndex('n', "gemQuartz")
		);

		registerPipeRecipeCategory(LogisticsProgramCompilerTileEntity.ProgrammCategories.FLUID, LPItems.pipeFluidSupplier.get());
		RecipeManager.craftingManager.addRecipe(new ItemStack(LPItems.pipeFluidSupplier.get()),
				new RecipeManager.RecipeLayout(
						" p ",
						"bsb",
						"iwi"
				),
				new RecipeManager.RecipeIndex('b', parts.getChipBasic()),
				new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(LPItems.pipeFluidSupplier.get())),
				new RecipeManager.RecipeIndex('s', LPItems.pipeBasic.get()),
				new RecipeManager.RecipeIndex('w', Items.BUCKET),
				new RecipeManager.RecipeIndex('i', "ingotIron")
		);

		registerPipeRecipeCategory(LogisticsProgramCompilerTileEntity.ProgrammCategories.FLUID, LPItems.pipeFluidBasic.get());
		RecipeManager.craftingManager.addRecipe(new ItemStack(LPItems.pipeFluidBasic.get()),
				new RecipeManager.RecipeLayout(
						" p ",
						"bsb",
						"gwg"
				),
				new RecipeManager.RecipeIndex('b', parts.getChipBasic()),
				new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(LPItems.pipeFluidBasic.get())),
				new RecipeManager.RecipeIndex('s', LPItems.pipeBasic.get()),
				new RecipeManager.RecipeIndex('w', Items.BUCKET),
				new RecipeManager.RecipeIndex('g', "ingotGold")
		);

		registerPipeRecipeCategory(LogisticsProgramCompilerTileEntity.ProgrammCategories.FLUID, LPItems.pipeFluidTerminus.get());
		RecipeManager.craftingManager.addRecipe(new ItemStack(LPItems.pipeFluidTerminus.get()),
				new RecipeManager.RecipeLayout(
						" p ",
						"wsv",
						"gbg"
				),
				new RecipeManager.RecipeIndex('b', parts.getChipBasic()),
				new RecipeManager.RecipeIndex('p', getIngredientForProgrammer(LPItems.pipeFluidTerminus.get())),
				new RecipeManager.RecipeIndex('s', LPItems.pipeFluidBasic.get()),
				new RecipeManager.RecipeIndex('w', "dyeBlack"),
				new RecipeManager.RecipeIndex('v', "dyePurple"),
				new RecipeManager.RecipeIndex('g', "ingotIron")
		);

		registerPipeRecipe(parts, RecipeType.LEVEL_2, LogisticsProgramCompilerTileEntity.ProgrammCategories.FLUID, LPItems.pipeFluidRequest.get(), LPItems.pipeFluidBasic.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_2, LogisticsProgramCompilerTileEntity.ProgrammCategories.FLUID, LPItems.pipeFluidProvider.get(), LPItems.pipeFluidBasic.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_2, LogisticsProgramCompilerTileEntity.ProgrammCategories.FLUID, LPItems.pipeFluidSupplierMk2.get(), LPItems.pipeFluidSupplier.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.FLUID, LPItems.pipeFluidSatellite.get(), LPItems.pipeFluidBasic.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.FLUID, LPItems.pipeFluidInsertion.get(), LPItems.pipeFluidBasic.get());
		registerPipeRecipe(parts, RecipeType.LEVEL_1, LogisticsProgramCompilerTileEntity.ProgrammCategories.FLUID, LPItems.pipeFluidExtractor.get(), LPItems.pipeFluidBasic.get());

	}

}
