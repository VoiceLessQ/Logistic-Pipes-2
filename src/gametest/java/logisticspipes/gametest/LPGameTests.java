package logisticspipes.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
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
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.routing.channels.ChannelManager;
import logisticspipes.utils.CraftingUtil;
import logisticspipes.utils.PlayerIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.util.LPDataIOWrapper;

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

	/** Orderer + dye keeps the link and gets LP1's colour variant; reset keeps the colour. */
	@GameTest(template = "empty")
	public static void ordererDyeKeepsLinkAndColour(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack orderer = new ItemStack(LPItems.remoteOrderer.get());
		CompoundTag tag = new CompoundTag();
		tag.putInt("lpgametest", 7);
		orderer.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

		CraftingInput dyeInput = CraftingInput.of(2, 1, List.of(orderer, new ItemStack(Items.BLACK_DYE)));
		RecipeHolder<CraftingRecipe> dye = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, dyeInput, level)
				.orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("no orderer dye recipe"));
		ItemStack black = dye.value().assemble(dyeInput, level.registryAccess());
		var model = black.get(DataComponents.CUSTOM_MODEL_DATA);
		helper.assertTrue(model != null && model.value() == 1, "black dye -> " + model + " via " + dye.id());
		helper.assertTrue(black.has(DataComponents.CUSTOM_DATA), "dyed orderer lost its link");

		CraftingInput resetInput = CraftingInput.of(1, 1, List.of(black));
		RecipeHolder<CraftingRecipe> reset = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, resetInput, level)
				.orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("no reset for a dyed orderer"));
		ItemStack clean = reset.value().assemble(resetInput, level.registryAccess());
		helper.assertTrue(!clean.has(DataComponents.CUSTOM_DATA), "reset kept the link");
		helper.assertTrue(java.util.Objects.equals(clean.get(DataComponents.CUSTOM_MODEL_DATA), model), "reset lost the colour: " + clean);

		ItemStack white = black.copy();
		white.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(16));
		CraftingInput mixed = CraftingInput.of(2, 1, List.of(black, white));
		helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, mixed, level).isEmpty(), "mixed colours matched a recipe");
		helper.succeed();
	}

	/** Worklog item 5 (server half): recipe list comes from the level's recipe manager. */
	@GameTest(template = "empty")
	public static void recipeListFromLevel(GameTestHelper helper) {
		int size = CraftingUtil.getRecipeList(helper.getLevel()).size();
		helper.assertTrue(size > 0, "getRecipeList(level) is empty");
		helper.succeed();
	}

	/** ItemIdentifier keeps every component (enchantments, names, potions), not just custom data. */
	@GameTest(template = "empty")
	public static void itemIdentifierKeepsComponents(GameTestHelper helper) {
		var registries = helper.getLevel().registryAccess();
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.enchant(registries.holderOrThrow(Enchantments.SHARPNESS), 3);
		sword.set(DataComponents.CUSTOM_NAME, Component.literal("lpgametest"));
		sword.setDamageValue(5);
		ItemStack potion = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
		for (ItemStack stack : new ItemStack[] { sword, potion }) {
			ItemIdentifier ident = ItemIdentifier.get(stack);
			helper.assertTrue(ident != ItemIdentifier.get(new ItemStack(stack.getItem())), stack + " identifies as the plain item");
			helper.assertTrue(ident == ItemIdentifier.get(stack.copy()), stack + " copy gets a different identifier");
			ItemStack rebuilt = ident.makeNormalStack(1);
			helper.assertTrue(ItemStack.isSameItemSameComponents(stack, rebuilt), "rebuilt " + rebuilt.getComponentsPatch() + " != " + stack.getComponentsPatch());
			byte[] data = LPDataIOWrapper.collectData(out -> {
				out.writeItemIdentifier(ident);
				out.writeItemStack(stack);
			});
			LPDataIOWrapper.provideData(data, in -> {
				helper.assertTrue(in.readItemIdentifier() == ident, stack + " identifier changed over the network");
				ItemStack read = in.readItemStack();
				helper.assertTrue(ItemStack.isSameItemSameComponents(stack, read), "network stack " + read.getComponentsPatch() + " != " + stack.getComponentsPatch());
			});
		}
		helper.succeed();
	}

	/** Channels live in one store for every dimension, as in LP1's global map storage. */
	@GameTest(template = "empty")
	public static void channelsSharedAcrossDimensions(GameTestHelper helper) {
		ServerLevel nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
		helper.assertTrue(nether != null, "no nether level");
		ChannelInformation channel = new ChannelManager(nether).createNewChannel("lpgametest",
				PlayerIdentifier.get("lpgametest", java.util.UUID.randomUUID()), ChannelInformation.AccessRights.PUBLIC, null);
		ChannelManager overworld = new ChannelManager(helper.getLevel());
		boolean found = overworld.getChannels().stream().anyMatch(c -> c.getChannelIdentifier().equals(channel.getChannelIdentifier()));
		overworld.removeChannel(channel.getChannelIdentifier());
		helper.assertTrue(found, "channel made in the nether is missing from the overworld manager");
		helper.succeed();
	}

	/** /logisticspipes runs from non-player sources (console, RCON); OP commands need level 4 there. */
	@GameTest(template = "empty")
	public static void commandsRunFromConsole(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int permission : new int[] { 4, 2 }) {
			List<String> output = new ArrayList<>();
			CommandSource capture = new CommandSource() {
				@Override
				public void sendSystemMessage(Component component) { output.add(component.getString()); }

				@Override
				public boolean acceptsSuccess() { return true; }

				@Override
				public boolean acceptsFailure() { return true; }

				@Override
				public boolean shouldInformAdmins() { return false; }
			};
			CommandSourceStack source = new CommandSourceStack(capture, Vec3.ZERO, Vec2.ZERO, level, permission, "Server",
					Component.literal("Server"), level.getServer(), null);
			level.getServer().getCommands().performPrefixedCommand(source, "logisticspipes rt");
			helper.assertTrue(output.stream().anyMatch(line -> line.startsWith("RoutingTableUpdateThread: Queued")),
					"level " + permission + " rt output " + output);
			output.clear();
			level.getServer().getCommands().performPrefixedCommand(source, "logisticspipes help");
			helper.assertTrue(output.stream().anyMatch(line -> line.contains("version")), "level " + permission + " help output " + output);
			boolean op = logisticspipes.commands.LogisticsPipesCommand.isOP(source);
			helper.assertTrue(op == (permission == 4), "isOP at level " + permission + " = " + op);
		}
		helper.succeed();
	}

	/** AE2 compat: an unconfigured ME interface exposes the network storage to LP (LP1 AEInterfaceInventoryHandler). */
	@GameTest(template = "empty", timeoutTicks = 200)
	public static void aeInterfaceExposesNetworkStorage(GameTestHelper helper) {
		if (!net.neoforged.fml.ModList.get().isLoaded(LPConstants.appliedenergisticsModID)) {
			helper.succeed();
			return;
		}
		BlockPos cell = new BlockPos(0, 2, 1), drive = new BlockPos(1, 2, 1), iface = new BlockPos(2, 2, 1);
		helper.setBlock(cell, aeBlock("creative_energy_cell"));
		helper.setBlock(drive, aeBlock("drive"));
		helper.setBlock(iface, aeBlock("interface"));
		net.neoforged.neoforge.items.IItemHandler driveInv = helper.getLevel().getCapability(
				net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, helper.absolutePos(drive), null);
		helper.assertTrue(driveInv != null, "drive has no item handler");
		ItemStack storageCell = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
				net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(LPConstants.appliedenergisticsModID, "item_storage_cell_1k")));
		helper.assertTrue(driveInv.insertItem(0, storageCell, false).isEmpty(), "drive refused the 1k cell");

		ItemStack cobble = new ItemStack(Items.COBBLESTONE, 10);
		ItemIdentifier ident = ItemIdentifier.get(cobble);
		helper.startSequence()
				.thenWaitUntil(() -> {
					// until the grid boots the interface falls back to its own item handler
					logisticspipes.interfaces.IInventoryUtil util = aeUtil(helper, iface, network.rs485.logisticspipes.inventory.ProviderMode.DEFAULT);
					helper.assertTrue(util instanceof logisticspipes.proxy.specialinventoryhandler.AEInterfaceInventoryHandler, "handler " + util);
					helper.assertTrue(util.roomForItem(cobble) == 10, "network not ready");
				})
				.thenExecute(() -> {
					logisticspipes.interfaces.IInventoryUtil util = aeUtil(helper, iface, network.rs485.logisticspipes.inventory.ProviderMode.DEFAULT);
					logisticspipes.utils.transactor.ITransactor transactor = (logisticspipes.utils.transactor.ITransactor) util;
					helper.assertTrue(transactor.add(cobble, null, false).getCount() == 10, "simulated add");
					helper.assertTrue(util.itemCount(ident) == 0, "simulated add stored items");
					helper.assertTrue(transactor.add(cobble, null, true).getCount() == 10, "add");
					helper.assertTrue(util.itemCount(ident) == 10, "count after add " + util.itemCount(ident));
					helper.assertTrue(util.getItems().contains(ident), "getItems " + util.getItems());
					ItemStack taken = util.getMultipleItems(ident, 4);
					helper.assertTrue(taken.is(Items.COBBLESTONE) && taken.getCount() == 4, "extracted " + taken);
					helper.assertTrue(util.getMultipleItems(ident, 7).isEmpty(), "extracted more than stored");
					helper.assertTrue(util.itemCount(ident) == 6, "count after extract " + util.itemCount(ident));
					int hidden = aeUtil(helper, iface, network.rs485.logisticspipes.inventory.ProviderMode.LEAVE_ONE_PER_TYPE).itemCount(ident);
					helper.assertTrue(hidden == 5, "hide one per type count " + hidden);
				})
				.thenSucceed();
	}

	@GameTest(template = "empty")
	public static void storageDrawersHandlerMovesItems(GameTestHelper helper) {
		if (!net.neoforged.fml.ModList.get().isLoaded(LPConstants.storagedrawersModID)) {
			helper.succeed();
			return;
		}
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
				net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(LPConstants.storagedrawersModID, "oak_full_drawers_2")).defaultBlockState());
		logisticspipes.interfaces.IInventoryUtil util = aeUtil(helper, pos, network.rs485.logisticspipes.inventory.ProviderMode.DEFAULT);
		helper.assertTrue(util instanceof network.rs485.logisticspipes.proxy.StorageDrawersInventoryHandler, "handler " + util);
		ItemStack cobble = new ItemStack(Items.COBBLESTONE, 10);
		ItemIdentifier ident = ItemIdentifier.get(cobble);
		logisticspipes.utils.transactor.ITransactor transactor = (logisticspipes.utils.transactor.ITransactor) util;
		helper.assertTrue(util.getItemsAndCount().isEmpty(), "empty drawer reports " + util.getItemsAndCount());
		helper.assertTrue(transactor.add(cobble, null, false).getCount() == 10, "simulated add");
		helper.assertTrue(util.itemCount(ident) == 0, "simulated add stored items");
		helper.assertTrue(transactor.add(cobble, null, true).getCount() == 10, "add");
		helper.assertTrue(util.itemCount(ident) == 10, "count after add " + util.itemCount(ident));
		helper.assertTrue(util.getItems().contains(ident), "getItems " + util.getItems());
		helper.assertTrue(util.getContainerSize() == 2, "size " + util.getContainerSize());
		ItemStack slot0 = util.getItem(0);
		helper.assertTrue(slot0.is(Items.COBBLESTONE) && slot0.getCount() == 10, "slot 0 " + slot0);
		ItemStack fromSlot = util.removeItem(0, 3);
		helper.assertTrue(fromSlot.is(Items.COBBLESTONE) && fromSlot.getCount() == 3, "removeItem " + fromSlot);
		ItemStack taken = util.getMultipleItems(ident, 4);
		helper.assertTrue(taken.is(Items.COBBLESTONE) && taken.getCount() == 4, "extracted " + taken);
		helper.assertTrue(util.itemCount(ident) == 3, "count after extract " + util.itemCount(ident));
		logisticspipes.interfaces.IInventoryUtil hiding = aeUtil(helper, pos, network.rs485.logisticspipes.inventory.ProviderMode.LEAVE_ONE_PER_TYPE);
		helper.assertTrue(hiding.itemCount(ident) == 2, "hide one per type count " + hiding.itemCount(ident));
		ItemStack hiddenTake = hiding.removeItem(0, 64);
		helper.assertTrue(hiddenTake.getCount() == 2 && util.itemCount(ident) == 1, "hide one removeItem " + hiddenTake);
		// drawers already holding the item fill before empty ones
		util.removeItem(0, 64);
		transactor.add(new ItemStack(Items.DIRT, 2), null, true);
		transactor.add(new ItemStack(Items.COBBLESTONE, 5), null, true);
		util.removeItem(0, 64);
		transactor.add(new ItemStack(Items.COBBLESTONE, 5), null, true);
		helper.assertTrue(util.getItem(0).isEmpty() && util.getItem(1).getCount() == 10, "fill order " + util.getItem(0) + " " + util.getItem(1));
		helper.succeed();
	}

	private static BlockState aeBlock(String path) {
		return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
				net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(LPConstants.appliedenergisticsModID, path)).defaultBlockState();
	}

	private static logisticspipes.interfaces.IInventoryUtil aeUtil(GameTestHelper helper, BlockPos pos, network.rs485.logisticspipes.inventory.ProviderMode mode) {
		logisticspipes.interfaces.IInventoryUtil util = logisticspipes.proxy.SimpleServiceLocator.inventoryUtilFactory.getHidingInventoryUtil(
				helper.getLevel().getBlockEntity(helper.absolutePos(pos)), net.minecraft.core.Direction.WEST, mode);
		helper.assertTrue(util != null, "no inventory util for the interface");
		return util;
	}
}
