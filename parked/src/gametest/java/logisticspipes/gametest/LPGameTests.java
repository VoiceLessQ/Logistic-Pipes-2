package logisticspipes.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import logisticspipes.LPBlocks;
import logisticspipes.LPConstants;
import logisticspipes.LPItems;
import logisticspipes.recipes.OrdererDyeRecipe;
import logisticspipes.config.Configs;
import logisticspipes.routing.IRouter;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
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
		Player player = helper.makeMockPlayer();
		float digSpeed = player.getDigSpeed(state, abs);
		float expected = digSpeed / Configs.pipeDurability / 30F;
		float actual = state.getDestroyProgress(player, helper.getLevel(), abs);
		helper.assertTrue(Math.abs(actual - expected) < 1e-5F,
				"destroy progress " + actual + ", expected " + expected + " (pipeDurability " + Configs.pipeDurability + ")");
		helper.succeed();
	}

	/** A survival player breaking a pipe gets the pipe item and its upgrades back. */
	@GameTest(template = "empty")
	public static void pipeBreakDropsPipeAndUpgrades(GameTestHelper helper) {
		LogisticsTileGenericPipe tile = placeBasicPipe(helper);
		BlockPos abs = helper.absolutePos(PIPE);
		Item speed = logisticspipes.items.ItemUpgrade.getAndCheckUpgrade(LPItems.upgrades.get(logisticspipes.pipes.upgrades.SpeedUpgrade.getName()));
		((CoreRoutedPipe) tile.pipe).getOriginalUpgradeManager().inv.setItem(0, new ItemStack(speed));
		// FakePlayer: a mock ServerPlayer logs in and LP's login packet fails on its unnegotiated connection
		ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
		player.setGameMode(GameType.SURVIVAL);
		helper.assertTrue(player.gameMode.destroyBlock(abs), "destroyBlock refused");
		List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2));
		long pipes = drops.stream().filter(e -> e.getItem().is(LPItems.pipeBasic.get())).mapToInt(e -> e.getItem().getCount()).sum();
		long upgrades = drops.stream().filter(e -> e.getItem().is(speed)).mapToInt(e -> e.getItem().getCount()).sum();
		helper.assertTrue(pipes == 1 && upgrades == 1, "drops " + drops.stream().map(ItemEntity::getItem).toList());
		drops.forEach(ItemEntity::discard);
		helper.succeed();
	}

	/** Chunk unload (onChunkUnloaded, then setRemoved) keeps the router registered, like LP1; a break destroys it. */
	@GameTest(template = "empty")
	public static void chunkUnloadKeepsRouter(GameTestHelper helper) {
		LogisticsTileGenericPipe tile = placeBasicPipe(helper);
		CoreRoutedPipe pipe = (CoreRoutedPipe) tile.pipe;
		IRouter router = pipe.getRouter();
		int id = router.getSimpleID();
		tile.onChunkUnloaded();
		tile.setRemoved();
		boolean kept = SimpleServiceLocator.routerManager.getRouter(id) == router;
		// the unload flag sticks to this block entity; release the router as a real reload would replace it
		pipe.invalidate();
		boolean released = SimpleServiceLocator.routerManager.getRouter(id) == null;
		helper.assertTrue(kept && released, "kept after unload=" + kept + " released after invalidate=" + released);
		helper.succeed();
	}

	/** A vanilla chest placed or broken next to a pipe updates its adjacent inventories within a few ticks, not at the 30s re-scan. */
	@GameTest(template = "empty")
	public static void chestNeighbourRefreshesAdjacent(GameTestHelper helper) {
		CoreRoutedPipe pipe = (CoreRoutedPipe) placeBasicPipe(helper).pipe;
		BlockPos chest = PIPE.east();
		helper.startSequence()
				.thenExecuteAfter(5, () -> helper.setBlock(chest, Blocks.CHEST))
				.thenExecuteAfter(3, () -> helper.assertTrue(pipe.getAvailableAdjacent().inventories().size() == 1,
						"after place: " + pipe.getAvailableAdjacent().inventories()))
				.thenExecute(() -> helper.destroyBlock(chest))
				.thenExecuteAfter(3, () -> helper.assertTrue(pipe.getAvailableAdjacent().inventories().isEmpty(),
						"after break: " + pipe.getAvailableAdjacent().inventories()))
				.thenSucceed();
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
					stack.getOrCreateTag().putInt("lpgametest", i);
				}
				grid.add(stack);
			}
			CraftingContainer input = grid(3, 3, grid);
			CraftingRecipe recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
					.orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("no recipe for " + count + " remote orderers"));
			ItemStack result = recipe.assemble(input, level.registryAccess());
			helper.assertTrue(result.is(LPItems.remoteOrderer.get()), count + " orderers -> " + result + " via " + recipe.getId());
			helper.assertTrue(result.getCount() == count, count + " orderers -> count " + result.getCount() + " via " + recipe.getId());
			helper.assertTrue(!result.hasTag(), "reset result still has a tag: " + result.getTag());
		}
		helper.succeed();
	}

	/** Regression: transit items in the update tag were loaded client-side as server items (ClassCastException). */
	@GameTest(template = "empty")
	public static void updateTagCarriesNoTransitState(GameTestHelper helper) {
		LogisticsTileGenericPipe tile = placeBasicPipe(helper);
		CompoundTag saved = tile.saveWithoutMetadata();
		helper.assertTrue(saved.contains("travelingEntities"), "save no longer writes travelingEntities; test is stale");
		CompoundTag update = tile.getUpdateTag();
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

	/** Regression: the power junction exposed no Forge Energy capability, so nothing could charge it. */
	@GameTest(template = "empty", timeoutTicks = 100)
	public static void creativeSourceFillsPowerJunction(GameTestHelper helper) {
		BlockPos junction = new BlockPos(1, 2, 2);
		helper.setBlock(junction, LPBlocks.powerJunction.get());
		helper.setBlock(new BlockPos(2, 2, 2), logisticspipes.LPRegistries.CREATIVE_POWER_SOURCE.get());
		helper.succeedWhen(() -> {
			if (!(helper.getBlockEntity(junction) instanceof logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity tile)) {
				throw new net.minecraft.gametest.framework.GameTestAssertException("no power junction block entity");
			}
			helper.assertTrue(tile.getPowerLevel() > 0, "junction power level " + tile.getPowerLevel());
		});
	}

	/** Regression: the crafting table exposed no item handler, so hoppers and other mods could not insert. */
	@GameTest(template = "empty")
	public static void craftingTableExposesItemHandler(GameTestHelper helper) {
		BlockPos table = new BlockPos(1, 2, 2);
		helper.setBlock(table, LPBlocks.crafter.get());
		net.minecraftforge.items.IItemHandler handler = helper.getBlockEntity(table)
				.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, net.minecraft.core.Direction.UP).orElse(null);
		helper.assertTrue(handler != null, "crafting table has no item handler");
		ItemStack left = handler.insertItem(0, new ItemStack(Items.OAK_PLANKS, 3), false);
		helper.assertTrue(left.isEmpty(), "insert left " + left);
		helper.succeed();
	}

	/** LP1 machine blocks: Material.IRON (metal map colour), hardness 6; any pickaxe drops them, a bare hand drops nothing. */
	@GameTest(template = "empty")
	public static void solidBlocksNeedPickaxe(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 2, 2);
		BlockPos abs = helper.absolutePos(pos);
		ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
		player.setGameMode(GameType.SURVIVAL);
		for (String name : new String[] { "frame", "power_junction", "security_station", "crafting_table", "crafting_table_fuzzy",
				"statistics_table", "power_provider_rf", "power_provider_eu", "power_provider_mj", "program_compiler" }) {
			net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
					new net.minecraft.resources.ResourceLocation(LPConstants.LP_MOD_ID, name));
			BlockState state = block.defaultBlockState();
			helper.assertTrue(state.getDestroySpeed(helper.getLevel(), abs) == 6.0F, name + " hardness " + state.getDestroySpeed(helper.getLevel(), abs));
			helper.assertTrue(state.getMapColor(helper.getLevel(), abs) == net.minecraft.world.level.material.MapColor.METAL, name + " map colour not METAL");
			helper.assertTrue(state.requiresCorrectToolForDrops(), name + " drops without a tool");
			helper.assertTrue(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state), name + " not harvestable by a wooden pickaxe");
			for (boolean pickaxe : new boolean[] { true, false }) {
				helper.setBlock(pos, state);
				player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pickaxe ? new ItemStack(Items.WOODEN_PICKAXE) : ItemStack.EMPTY);
				helper.assertTrue(player.gameMode.destroyBlock(abs), name + " destroyBlock refused");
				List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2));
				boolean dropped = drops.stream().anyMatch(e -> e.getItem().is(block.asItem()));
				drops.forEach(ItemEntity::discard);
				helper.assertTrue(dropped == pickaxe, name + (pickaxe ? " no drop with a pickaxe" : " dropped by hand"));
			}
		}
		helper.succeed();
	}

	/** Orderer + dye keeps the link and gets LP1's colour variant; reset keeps the colour. */
	@GameTest(template = "empty")
	public static void ordererDyeKeepsLinkAndColour(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack orderer = new ItemStack(LPItems.remoteOrderer.get());
		orderer.getOrCreateTag().putInt("lpgametest", 7);

		CraftingContainer dyeInput = grid(2, 1, List.of(orderer, new ItemStack(Items.BLACK_DYE)));
		CraftingRecipe dye = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, dyeInput, level)
				.orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("no orderer dye recipe"));
		ItemStack black = dye.assemble(dyeInput, level.registryAccess());
		int model = OrdererDyeRecipe.getVariant(black);
		helper.assertTrue(model == 1, "black dye -> " + model + " via " + dye.getId());
		helper.assertTrue(black.getTag().contains("lpgametest"), "dyed orderer lost its link");

		CraftingContainer resetInput = grid(1, 1, List.of(black));
		CraftingRecipe reset = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, resetInput, level)
				.orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("no reset for a dyed orderer"));
		ItemStack clean = reset.assemble(resetInput, level.registryAccess());
		helper.assertTrue(clean.getTag() == null || !clean.getTag().contains("lpgametest"), "reset kept the link");
		helper.assertTrue(OrdererDyeRecipe.getVariant(clean) == model, "reset lost the colour: " + clean.getTag());

		ItemStack white = black.copy();
		white.getOrCreateTag().putInt(OrdererDyeRecipe.COLOR_TAG, 16);
		CraftingContainer mixed = grid(2, 1, List.of(black, white));
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

	/** H3: the five chipped fluid pipes have a crafting recipe and a FLUID compiler program. */
	@GameTest(template = "empty")
	public static void fluidPipesCraftable(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var programs = logisticspipes.blocks.LogisticsProgramCompilerTileEntity.programByCategory
				.get(logisticspipes.blocks.LogisticsProgramCompilerTileEntity.ProgrammCategories.FLUID);
		for (Item pipe : new Item[] { LPItems.pipeFluidRequest.get(), LPItems.pipeFluidProvider.get(), LPItems.pipeFluidSatellite.get(),
				LPItems.pipeFluidInsertion.get(), LPItems.pipeFluidExtractor.get() }) {
			CraftingRecipe recipe = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
					.filter(r -> r.getResultItem(level.registryAccess()).is(pipe)).findFirst().orElse(null);
			helper.assertTrue(recipe != null, "no crafting recipe for " + pipe);
			// dedicated servers send recipes to the client (and JEI) through the serializer
			CraftingRecipe synced = networkRoundTrip(recipe);
			helper.assertTrue(synced.getResultItem(level.registryAccess()).is(pipe)
					&& synced.getIngredients().size() == recipe.getIngredients().size(), "recipe for " + pipe + " changed over the network");
			helper.assertTrue(programs != null && programs.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(pipe)),
					"no FLUID program for " + pipe);
		}
		helper.succeed();
	}

	@SuppressWarnings("unchecked")
	private static <T extends net.minecraft.world.item.crafting.Recipe<?>> T networkRoundTrip(T recipe) {
		var serializer = (net.minecraft.world.item.crafting.RecipeSerializer<T>) recipe.getSerializer();
		var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
		serializer.toNetwork(buf, recipe);
		return serializer.fromNetwork(recipe.getId(), buf);
	}

	/** H2: the creative-tab sink compares against the item's first category tab. */
	@GameTest(template = "empty")
	public static void creativeTabNameFromRegistry(GameTestHelper helper) {
		String iron = ItemIdentifier.get(new ItemStack(Items.IRON_INGOT)).getCreativeTabName();
		String log = ItemIdentifier.get(new ItemStack(Items.OAK_LOG)).getCreativeTabName();
		helper.assertTrue("ingredients".equals(iron), "iron ingot tab '" + iron + "'");
		helper.assertTrue(log != null && !log.equals(iron), "oak log tab '" + log + "'");
		helper.succeed();
	}

	/** ItemIdentifier keeps the whole tag (enchantments, names, potions, damage). */
	@GameTest(template = "empty")
	public static void itemIdentifierKeepsTags(GameTestHelper helper) {
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.enchant(Enchantments.SHARPNESS, 3);
		sword.setHoverName(Component.literal("lpgametest"));
		sword.setDamageValue(5);
		ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS);
		for (ItemStack stack : new ItemStack[] { sword, potion }) {
			ItemIdentifier ident = ItemIdentifier.get(stack);
			helper.assertTrue(ident != ItemIdentifier.get(new ItemStack(stack.getItem())), stack + " identifies as the plain item");
			helper.assertTrue(ident == ItemIdentifier.get(stack.copy()), stack + " copy gets a different identifier");
			ItemStack rebuilt = ident.makeNormalStack(1);
			helper.assertTrue(ItemStack.isSameItemSameTags(stack, rebuilt), "rebuilt " + rebuilt.getTag() + " != " + stack.getTag());
			byte[] data = LPDataIOWrapper.collectData(out -> {
				out.writeItemIdentifier(ident);
				out.writeItemStack(stack);
			});
			LPDataIOWrapper.provideData(data, in -> {
				helper.assertTrue(in.readItemIdentifier() == ident, stack + " identifier changed over the network");
				ItemStack read = in.readItemStack();
				helper.assertTrue(ItemStack.isSameItemSameTags(stack, read), "network stack " + read.getTag() + " != " + stack.getTag());
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
		if (!net.minecraftforge.fml.ModList.get().isLoaded(LPConstants.appliedenergisticsModID)) {
			helper.succeed();
			return;
		}
		BlockPos cell = new BlockPos(0, 2, 1), drive = new BlockPos(1, 2, 1), iface = new BlockPos(2, 2, 1);
		helper.setBlock(cell, aeBlock("creative_energy_cell"));
		helper.setBlock(drive, aeBlock("drive"));
		helper.setBlock(iface, aeBlock("interface"));
		net.minecraftforge.items.IItemHandler driveInv = helper.getBlockEntity(drive)
				.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElse(null);
		helper.assertTrue(driveInv != null, "drive has no item handler");
		ItemStack storageCell = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
				new net.minecraft.resources.ResourceLocation(LPConstants.appliedenergisticsModID, "item_storage_cell_1k")));
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

	/** AE2 compat: fluid pipes see and drain the network's fluids through an ME interface (LP1 AETankHandler). */
	@GameTest(template = "empty", timeoutTicks = 200)
	public static void aeInterfaceExposesNetworkFluids(GameTestHelper helper) {
		if (!net.minecraftforge.fml.ModList.get().isLoaded(LPConstants.appliedenergisticsModID)) {
			helper.succeed();
			return;
		}
		BlockPos cell = new BlockPos(0, 2, 1), drive = new BlockPos(1, 2, 1), iface = new BlockPos(2, 2, 1);
		helper.setBlock(cell, aeBlock("creative_energy_cell"));
		helper.setBlock(drive, aeBlock("drive"));
		helper.setBlock(iface, aeBlock("interface"));
		net.minecraftforge.items.IItemHandler driveInv = helper.getBlockEntity(drive)
				.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElse(null);
		helper.assertTrue(driveInv != null, "drive has no item handler");
		ItemStack storageCell = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
				new net.minecraft.resources.ResourceLocation(LPConstants.appliedenergisticsModID, "fluid_storage_cell_1k")));
		helper.assertTrue(driveInv.insertItem(0, storageCell, false).isEmpty(), "drive refused the fluid cell");

		logisticspipes.utils.FluidIdentifier water = logisticspipes.utils.FluidIdentifier.get(
				new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(AE2Fluids.insertWater(helper.getBlockEntity(iface), 5000) == 5000, "network not ready"))
				.thenExecute(() -> {
					net.minecraft.world.level.block.entity.BlockEntity tile = helper.getBlockEntity(iface);
					logisticspipes.interfaces.ITankUtil util = logisticspipes.pipes.PipeFluidUtil.INSTANCE.getTankUtilForTE(tile, net.minecraft.core.Direction.WEST);
					helper.assertTrue(util instanceof logisticspipes.interfaces.ISpecialTankUtil, "tank util " + util);
					logisticspipes.interfaces.ISpecialTankAccessHandler handler = ((logisticspipes.interfaces.ISpecialTankUtil) util).getSpecialHandler();
					helper.assertTrue(Long.valueOf(5000).equals(handler.getAvailableLiquid(tile).get(water)), "available " + handler.getAvailableLiquid(tile));
					net.minecraftforge.fluids.FluidStack simulated = handler.drainFrom(tile, water, 2000, false);
					helper.assertTrue(simulated != null && simulated.getAmount() == 2000, "simulated drain " + simulated);
					helper.assertTrue(Long.valueOf(5000).equals(handler.getAvailableLiquid(tile).get(water)), "simulated drain removed fluid");
					net.minecraftforge.fluids.FluidStack drained = handler.drainFrom(tile, water, 2000, true);
					helper.assertTrue(drained != null && drained.getFluid() == net.minecraft.world.level.material.Fluids.WATER && drained.getAmount() == 2000, "drain " + drained);
					helper.assertTrue(Long.valueOf(3000).equals(handler.getAvailableLiquid(tile).get(water)), "after drain " + handler.getAvailableLiquid(tile));
					net.minecraftforge.fluids.FluidStack tooMuch = handler.drainFrom(tile, water, 4000, true);
					helper.assertTrue(tooMuch != null && tooMuch.getAmount() == 3000, "over-drain " + tooMuch);
				})
				.thenSucceed();
	}

	/** Supplier pipe slot finder opens an EnderStorage chest through its tile (LP1 EnderStorageProxy). */
	@GameTest(template = "empty")
	public static void slotFinderOpensEnderChest(GameTestHelper helper) {
		if (!net.minecraftforge.fml.ModList.get().isLoaded(LPConstants.enderStorageModID)) {
			helper.succeed();
			return;
		}
		placeBasicPipe(helper);
		helper.setBlock(PIPE.east(), net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
				new net.minecraft.resources.ResourceLocation(LPConstants.enderStorageModID, "ender_chest")));
		ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
		BlockPos abs = helper.absolutePos(PIPE);
		helper.runAfterDelay(5, () -> {
			player.closeContainer();
			logisticspipes.network.packets.pipe.SlotFinderOpenGuiPacket packet = logisticspipes.network.PacketHandler
					.getPacket(logisticspipes.network.packets.pipe.SlotFinderOpenGuiPacket.class);
			packet.setSlot(0);
			packet.setPosX(abs.getX());
			packet.setPosY(abs.getY());
			packet.setPosZ(abs.getZ());
			packet.processPacket(player);
			String menu = player.containerMenu.getClass().getName();
			player.closeContainer();
			helper.assertTrue(menu.startsWith("codechicken.enderstorage"), "opened " + menu);
			helper.succeed();
		});
	}

	/** A pipe ignores a turtle until the turtle calls setTurtleConnect(true); removing the turtle clears the flag. */
	@GameTest(template = "empty")
	public static void turtleConnectsOnRequest(GameTestHelper helper) {
		if (!net.minecraftforge.fml.ModList.get().isLoaded(LPConstants.computerCraftModID)) {
			helper.succeed();
			return;
		}
		LogisticsTileGenericPipe tile = placeBasicPipe(helper);
		BlockPos turtle = PIPE.east();
		helper.setBlock(turtle, net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
				new net.minecraft.resources.ResourceLocation(LPConstants.computerCraftModID, "turtle_normal")));
		int east = net.minecraft.core.Direction.EAST.ordinal();
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(SimpleServiceLocator.ccProxy.isTurtle(helper.getBlockEntity(turtle)), "turtle not recognised");
			helper.assertFalse(tile.pipeConnectionsBuffer[east], "pipe connected to the turtle before setTurtleConnect");
			helper.assertTrue(Turtles.call(tile, "setTurtleConnect", true) == null, "setTurtleConnect failed");
		});
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(tile.turtleConnect[east], "turtleConnect flag not set");
			helper.assertTrue(tile.pipeConnectionsBuffer[east], "pipe did not connect to the turtle");
			helper.assertTrue(Boolean.TRUE.equals(Turtles.call(tile, "getTurtleConnect")), "getTurtleConnect not true");
			helper.setBlock(turtle, Blocks.AIR);
		});
		helper.runAfterDelay(15, () -> {
			helper.assertFalse(tile.turtleConnect[east], "turtleConnect kept after the turtle left");
			helper.succeed();
		});
	}

	// loaded only when CC: Tweaked is present; a stub computer stands in for the turtle's Lua side
	private static final class Turtles {
		static Object call(LogisticsTileGenericPipe tile, String method, Object... args) {
			var peripheral = (dan200.computercraft.api.peripheral.IDynamicPeripheral) logisticspipes.proxy.cc.CCProxy.createPeripheral(tile, net.minecraft.core.Direction.EAST);
			var computer = new dan200.computercraft.api.peripheral.IComputerAccess() {
				public String mount(String desiredLocation, dan200.computercraft.api.filesystem.Mount mount, String driveName) { return null; }
				public String mountWritable(String desiredLocation, dan200.computercraft.api.filesystem.WritableMount mount, String driveName) { return null; }
				public void unmount(String location) {}
				public int getID() { return 1; }
				public void queueEvent(String event, Object... arguments) {}
				public String getAttachmentName() { return "east"; }
				public java.util.Map<String, dan200.computercraft.api.peripheral.IPeripheral> getAvailablePeripherals() { return java.util.Map.of(); }
				public dan200.computercraft.api.peripheral.IPeripheral getAvailablePeripheral(String name) { return null; }
				public dan200.computercraft.api.peripheral.WorkMonitor getMainThreadMonitor() { return null; }
			};
			int id = java.util.Arrays.asList(peripheral.getMethodNames()).indexOf(method);
			peripheral.attach(computer);
			try {
				Object[] result = peripheral.callMethod(computer, task -> 0, id, new dan200.computercraft.api.lua.ObjectArguments(args)).getResult();
				return result == null || result.length == 0 ? null : result[0];
			} catch (dan200.computercraft.api.lua.LuaException e) {
				throw new net.minecraft.gametest.framework.GameTestAssertException(method + ": " + e.getMessage());
			} finally {
				peripheral.detach(computer);
			}
		}
	}

	// loaded only when AE2 is present
	private static final class AE2Fluids {
		static long insertWater(net.minecraft.world.level.block.entity.BlockEntity tile, long amount) {
			appeng.api.storage.MEStorage storage = tile.getCapability(
					net.minecraftforge.common.capabilities.CapabilityManager.get(new net.minecraftforge.common.capabilities.CapabilityToken<appeng.api.storage.MEStorage>() {}), null)
					.orElse(null);
			if (storage == null) return 0;
			return storage.insert(appeng.api.stacks.AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER), amount,
					appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty());
		}
	}

	@GameTest(template = "empty")
	public static void storageDrawersHandlerMovesItems(GameTestHelper helper) {
		if (!net.minecraftforge.fml.ModList.get().isLoaded(LPConstants.storagedrawersModID)) {
			helper.succeed();
			return;
		}
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
				new net.minecraft.resources.ResourceLocation(LPConstants.storagedrawersModID, "oak_full_drawers_2")).defaultBlockState());
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

	@GameTest(template = "empty")
	public static void theOneProbeDescribesSatellitePipe(GameTestHelper helper) {
		if (!net.minecraftforge.fml.ModList.get().isLoaded(LPConstants.theOneProbeModID)) {
			helper.succeed();
			return;
		}
		BlockPos abs = helper.absolutePos(PIPE);
		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.createPipe(LPItems.pipeSatellite.get());
		helper.assertTrue(pipe != null && LogisticsBlockGenericPipe.placePipe(pipe, helper.getLevel(), abs, LPBlocks.pipe.get()), "place satellite pipe");
		List<String> keys = TheOneProbeCheck.probeKeys(helper, abs);
		helper.assertTrue(keys.equals(List.of("top.logisticspipes.pipe.satellite.no_name", "top.logisticspipes.general.no_upgrades")), "probe keys " + keys);
		helper.succeed();
	}

	/** Crafting grid without a menu; the list constructor skips the menu's slotsChanged callback. */
	/** Every single-block pipe survives a chunk unload and cold reload: same type, router id, upgrades and NBT. */
	@GameTest(template = "empty", timeoutTicks = 600)
	public static void everyPipeSurvivesColdLoad(GameTestHelper helper) {
		coldLoadAll(helper, false);
	}

	/** Same with the router dropped first, as after a server restart: the router UUID must come back from NBT. */
	@GameTest(template = "empty", timeoutTicks = 600)
	public static void everyPipeSurvivesRestart(GameTestHelper helper) {
		coldLoadAll(helper, true);
	}

	private static void coldLoadAll(GameTestHelper helper, boolean restart) {
		List<Item> pipes = new ArrayList<>();
		List<String> skipped = new ArrayList<>();
		for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (!(item instanceof logisticspipes.items.ItemLogisticsPipe)) continue;
			CoreUnroutedPipe probe = LogisticsBlockGenericPipe.createPipe(item);
			if (probe instanceof logisticspipes.pipes.basic.CoreMultiBlockPipe) skipped.add(String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)));
			else pipes.add(item);
		}
		helper.assertTrue(pipes.size() >= 28, "only " + pipes.size() + " single-block pipes found");
		List<String> failures = new ArrayList<>();
		coldLoadStep(helper, pipes, 0, failures, skipped, restart);
	}

	private static void coldLoadStep(GameTestHelper helper, List<Item> pipes, int index, List<String> failures, List<String> skipped, boolean restart) {
		if (index >= pipes.size()) {
			org.apache.logging.log4j.LogManager.getLogger("LPGameTests").info("cold load (restart={}): {} pipes checked, multi-block skipped {}", restart, pipes.size(), skipped);
			helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
			helper.succeed();
			return;
		}
		Item item = pipes.get(index);
		String name = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(PIPE);
		Item speed = logisticspipes.items.ItemUpgrade.getAndCheckUpgrade(LPItems.upgrades.get(logisticspipes.pipes.upgrades.SpeedUpgrade.getName()));
		CoreUnroutedPipe created = LogisticsBlockGenericPipe.createPipe(item);
		if (created == null || !LogisticsBlockGenericPipe.placePipe(created, level, abs, LPBlocks.pipe.get())) {
			failures.add(name + ": place failed");
			coldLoadStep(helper, pipes, index + 1, failures, skipped, restart);
			return;
		}
		helper.runAfterDelay(3, () -> {
			if (!(level.getBlockEntity(abs) instanceof LogisticsTileGenericPipe tile) || tile.pipe == null) {
				failures.add(name + ": no tile after place");
				level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
				coldLoadStep(helper, pipes, index + 1, failures, skipped, restart);
				return;
			}
			if (tile.pipe instanceof CoreRoutedPipe routed) routed.getOriginalUpgradeManager().inv.setItem(0, new ItemStack(speed));
			CompoundTag before = tile.saveWithFullMetadata();
			Class<?> type = tile.pipe.getClass();
			IRouter router = tile.pipe instanceof CoreRoutedPipe routed ? routed.getRouter() : null;
			BlockState state = level.getBlockState(abs);
			tile.onChunkUnloaded();
			level.removeBlockEntity(abs);
			if (restart && router != null) router.destroy();
			// chunk load path: load() with a null level, then setBlockEntity and onLoad on the next tick
			net.minecraft.world.level.block.entity.BlockEntity reloaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(abs, state, before);
			if (reloaded == null) {
				failures.add(name + ": loadStatic returned null");
				level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
				coldLoadStep(helper, pipes, index + 1, failures, skipped, restart);
				return;
			}
			level.setBlockEntity(reloaded);
			helper.runAfterDelay(3, () -> {
				if (!(level.getBlockEntity(abs) instanceof LogisticsTileGenericPipe fresh) || fresh.pipe == null) {
					failures.add(name + ": no pipe after reload");
				} else {
					if (fresh.pipe.getClass() != type) failures.add(name + ": type " + fresh.pipe.getClass().getSimpleName());
					if (!fresh.isInitialized()) failures.add(name + ": not initialized after reload");
					if (router != null) {
						CoreRoutedPipe routed = (CoreRoutedPipe) fresh.pipe;
						if (!routed.getRouter().getId().equals(router.getId())) failures.add(name + ": router uuid " + router.getId() + " -> " + routed.getRouter().getId());
						if (!restart && routed.getRouter() != router) failures.add(name + ": router replaced on chunk reload");
						if (!routed.getOriginalUpgradeManager().inv.getItem(0).is(speed)) failures.add(name + ": upgrade lost");
					}
					CompoundTag after = fresh.saveWithFullMetadata();
					if (!after.equals(before)) failures.add(name + ": NBT changed " + tagDiff(before, after));
				}
				level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
				coldLoadStep(helper, pipes, index + 1, failures, skipped, restart);
			});
		});
	}

	/** Every high-speed tube, placed like a player would, comes back from a chunk reload with the same NBT in all its blocks. */
	@GameTest(template = "empty", timeoutTicks = 300)
	public static void everyTubeSurvivesColdLoad(GameTestHelper helper) {
		List<Item> tubes = new ArrayList<>();
		for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (item instanceof logisticspipes.items.ItemLogisticsPipe && LogisticsBlockGenericPipe.createPipe(item) instanceof logisticspipes.pipes.basic.CoreMultiBlockPipe) tubes.add(item);
		}
		helper.assertTrue(tubes.size() == 5, "expected 5 tubes, found " + tubes.size());
		tubeStep(helper, tubes, 0, new ArrayList<>());
	}

	private static void tubeStep(GameTestHelper helper, List<Item> tubes, int index, List<String> failures) {
		if (index >= tubes.size()) {
			helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
			helper.succeed();
			return;
		}
		Item item = tubes.get(index);
		String name = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
		ServerLevel level = helper.getLevel();
		// above the plot: tubes span up to 7 blocks and the empty template is 5x5x5
		BlockPos floor = helper.absolutePos(new BlockPos(2, 20, 2));
		BlockPos min = floor.offset(-8, 1, -8), max = floor.offset(8, 4, 8);
		level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
		ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
		player.moveTo(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 3.5, 180f, 30f);
		ItemStack stack = new ItemStack(item);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
		stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), net.minecraft.core.Direction.UP, floor, false)));
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.runAfterDelay(3, () -> {
			java.util.Map<BlockPos, CompoundTag> before = new java.util.HashMap<>();
			Class<?> type = null;
			BlockPos mainPos = null;
			for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
				net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
				if (be instanceof LogisticsTileGenericPipe tile && tile.pipe instanceof logisticspipes.pipes.basic.CoreMultiBlockPipe) {
					type = tile.pipe.getClass();
					mainPos = pos.immutable();
				}
				if (be instanceof LogisticsTileGenericPipe || be instanceof logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock) before.put(pos.immutable(), be.saveWithFullMetadata());
			}
			if (mainPos == null) {
				failures.add(name + ": not placed (" + before.size() + " block entities)");
				clearTubeArea(level, min, max, floor);
				tubeStep(helper, tubes, index + 1, failures);
				return;
			}
			// unload everything first, then reload: neighbours must not see a half-loaded tube
			for (BlockPos pos : before.keySet()) {
				net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
				be.onChunkUnloaded();
				level.removeBlockEntity(pos);
			}
			for (var e : before.entrySet()) {
				net.minecraft.world.level.block.entity.BlockEntity reloaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(e.getKey(), level.getBlockState(e.getKey()), e.getValue());
				if (reloaded == null) failures.add(name + ": loadStatic null at " + e.getKey());
				else level.setBlockEntity(reloaded);
			}
			Class<?> expected = type;
			BlockPos main = mainPos;
			helper.runAfterDelay(3, () -> {
				if (!(level.getBlockEntity(main) instanceof LogisticsTileGenericPipe fresh) || fresh.pipe == null || fresh.pipe.getClass() != expected) {
					failures.add(name + ": main pipe lost after reload");
				} else if (!fresh.isInitialized()) {
					failures.add(name + ": not initialized after reload");
				}
				for (var e : before.entrySet()) {
					net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(e.getKey());
					CompoundTag after = be == null ? null : be.saveWithFullMetadata();
					if (after == null) failures.add(name + ": block entity gone at " + e.getKey());
					else if (!after.equals(e.getValue())) failures.add(name + ": NBT changed at " + e.getKey() + " " + tagDiff(e.getValue(), after));
				}
				clearTubeArea(level, min, max, floor);
				tubeStep(helper, tubes, index + 1, failures);
			});
		});
	}

	private static void clearTubeArea(ServerLevel level, BlockPos min, BlockPos max, BlockPos floor) {
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(floor, Blocks.AIR.defaultBlockState(), 3);
	}

	/** LP1 mayPlace: a pipe is not placed into a mob, and is placed once the space is clear. */
	@GameTest(template = "empty")
	public static void pipePlacementRespectsMobs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos floor = helper.absolutePos(new BlockPos(2, 1, 2));
		BlockPos target = floor.above();
		level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
		net.minecraft.world.entity.Entity pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(2, 2, 2));
		boolean blocked = !placeWithPlayer(level, floor) && !(level.getBlockEntity(target) instanceof LogisticsTileGenericPipe);
		pig.discard();
		boolean placed = placeWithPlayer(level, floor) && level.getBlockEntity(target) instanceof LogisticsTileGenericPipe;
		helper.assertTrue(blocked && placed, "refused with pig=" + blocked + " placed when clear=" + placed);
		helper.succeed();
	}

	private static boolean placeWithPlayer(ServerLevel level, BlockPos floor) {
		ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
		player.moveTo(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 3.5, 180f, 30f);
		ItemStack stack = new ItemStack(LPItems.pipeBasic.get());
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
		var result = stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), net.minecraft.core.Direction.UP, floor, false)));
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return result.consumesAction();
	}

	private static String tagDiff(CompoundTag a, CompoundTag b) {
		List<String> keys = new ArrayList<>();
		for (String k : a.getAllKeys()) if (!java.util.Objects.equals(a.get(k), b.get(k))) keys.add(k + "=" + a.get(k) + "->" + b.get(k));
		for (String k : b.getAllKeys()) if (!a.contains(k)) keys.add("+" + k);
		return keys.toString();
	}

	private static CraftingContainer grid(int width, int height, List<ItemStack> items) {
		NonNullList<ItemStack> list = NonNullList.withSize(width * height, ItemStack.EMPTY);
		for (int i = 0; i < items.size(); i++) list.set(i, items.get(i));
		return new TransientCraftingContainer(null, width, height, list);
	}

	private static BlockState aeBlock(String path) {
		return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
				new net.minecraft.resources.ResourceLocation(LPConstants.appliedenergisticsModID, path)).defaultBlockState();
	}

	private static logisticspipes.interfaces.IInventoryUtil aeUtil(GameTestHelper helper, BlockPos pos, network.rs485.logisticspipes.inventory.ProviderMode mode) {
		logisticspipes.interfaces.IInventoryUtil util = logisticspipes.proxy.SimpleServiceLocator.inventoryUtilFactory.getHidingInventoryUtil(
				helper.getLevel().getBlockEntity(helper.absolutePos(pos)), net.minecraft.core.Direction.WEST, mode);
		helper.assertTrue(util != null, "no inventory util for the interface");
		return util;
	}
}
