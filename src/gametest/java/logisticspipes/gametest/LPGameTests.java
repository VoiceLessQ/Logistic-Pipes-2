package logisticspipes.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import logisticspipes.LPBlocks;
import logisticspipes.LPConstants;
import logisticspipes.LPItems;
import logisticspipes.config.Configs;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.CraftingUtil;

/** Server-side checks for fixes that compile but had no runtime proof. Run with runGameTestServer. */
@GameTestHolder(LPConstants.LP_MOD_ID)
@PrefixGameTestTemplate(false)
public class LPGameTests {

	private static final BlockPos PIPE = new BlockPos(2, 2, 2);

	private static LogisticsTileGenericPipe placeBasicPipe(GameTestHelper helper) {
		BlockPos abs = helper.absolutePos(PIPE);
		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.createPipe(LPItems.pipeBasic.get());
		helper.assertTrue(pipe != null, "createPipe returned null for pipe_basic");
		helper.assertTrue(LogisticsBlockGenericPipe.placePipe(pipe, helper.getLevel(), abs, LPBlocks.pipe.get()), "placePipe failed");
		if (!(helper.getLevel().getBlockEntity(abs) instanceof LogisticsTileGenericPipe tile)) {
			throw new net.minecraft.gametest.framework.GameTestAssertException("no pipe block entity at " + abs);
		}
		return tile;
	}

	/** Worklog item 1: pipe break time follows Configs.pipeDurability (LP1 getBlockHardness). */
	@GameTest(template = "empty")
	public static void pipeBreakTimeFollowsConfig(GameTestHelper helper) {
		placeBasicPipe(helper);
		BlockPos abs = helper.absolutePos(PIPE);
		BlockState state = helper.getLevel().getBlockState(abs);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		float digSpeed = player.getDigSpeed(state, abs);
		float expected = digSpeed / Configs.pipeDurability / 30F;
		float actual = state.getDestroyProgress(player, helper.getLevel(), abs);
		helper.assertTrue(Math.abs(actual - expected) < 1e-5F,
				"destroy progress " + actual + ", expected " + expected + " (pipeDurability " + Configs.pipeDurability + ")");
		helper.succeed();
	}

	/** Worklog item 2: the reset recipe returns one clean item per matching stack (LP1). */
	@GameTest(template = "empty")
	public static void resetRecipeReturnsOnePerItem(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int count : new int[] { 1, 3 }) {
			List<ItemStack> grid = new ArrayList<>();
			for (int i = 0; i < 9; i++) {
				ItemStack stack = ItemStack.EMPTY;
				if (i < count) {
					stack = new ItemStack(LPItems.remoteOrderer.get());
					CompoundTag tag = new CompoundTag();
					tag.putInt("lpgametest", i);
					stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
				}
				grid.add(stack);
			}
			CraftingInput input = CraftingInput.of(3, 3, grid);
			RecipeHolder<CraftingRecipe> holder = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
					.orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("no recipe for " + count + " remote orderers"));
			ItemStack result = holder.value().assemble(input, level.registryAccess());
			helper.assertTrue(result.is(LPItems.remoteOrderer.get()), count + " orderers -> " + result + " via " + holder.id());
			helper.assertTrue(result.getCount() == count, count + " orderers -> count " + result.getCount() + " via " + holder.id());
			helper.assertTrue(!result.has(DataComponents.CUSTOM_DATA), "reset result still has custom data");
		}
		helper.succeed();
	}

	/** Regression: transit items in the update tag were loaded client-side as server items (ClassCastException). */
	@GameTest(template = "empty")
	public static void updateTagCarriesNoTransitState(GameTestHelper helper) {
		LogisticsTileGenericPipe tile = placeBasicPipe(helper);
		var registries = helper.getLevel().registryAccess();
		CompoundTag saved = tile.saveWithoutMetadata(registries);
		helper.assertTrue(saved.contains("travelingEntities"), "save no longer writes travelingEntities; test is stale");
		CompoundTag update = tile.getUpdateTag(registries);
		helper.assertTrue(!update.contains("travelingEntities"), "update tag carries travelingEntities");
		helper.assertTrue(!update.contains("buffercontents"), "update tag carries buffercontents");
		helper.succeed();
	}

	/** Phase 5 item: a creative power source fills an adjacent RF power provider. */
	@GameTest(template = "empty", timeoutTicks = 100)
	public static void creativeSourceFillsRFProvider(GameTestHelper helper) {
		BlockPos provider = new BlockPos(1, 2, 2);
		helper.setBlock(provider, LPBlocks.powerProviderRF.get());
		helper.setBlock(new BlockPos(2, 2, 2), logisticspipes.LPRegistries.CREATIVE_POWER_SOURCE.get());
		helper.succeedWhen(() -> {
			if (!(helper.getBlockEntity(provider) instanceof logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity tile)) {
				throw new net.minecraft.gametest.framework.GameTestAssertException("no power provider block entity");
			}
			helper.assertTrue(tile.getPowerLevel() > 0, "provider power level " + tile.getPowerLevel());
		});
	}

	/** Worklog item 5 (server half): recipe list comes from the level's recipe manager. */
	@GameTest(template = "empty")
	public static void recipeListFromLevel(GameTestHelper helper) {
		int size = CraftingUtil.getRecipeList(helper.getLevel()).size();
		helper.assertTrue(size > 0, "getRecipeList(level) is empty");
		helper.succeed();
	}
}
