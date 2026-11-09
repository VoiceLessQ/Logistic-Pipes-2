package logisticspipes.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.request.RequestTree;
import logisticspipes.transport.PipeTransportLogistics;
import logisticspipes.utils.item.ItemIdentifierStack;

/** Dev-only {@code /lptest} commands for unattended server checks; registered only with {@code -Dlp.devtest=true}. */
final class DevTestCommands {

	private DevTestCommands() {}

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(Commands.literal("lptest")
				.then(Commands.literal("recipes").executes(DevTestCommands::checkRecipes))
				.then(Commands.literal("craft").then(Commands.argument("item", StringArgumentType.string()).executes(DevTestCommands::craft)))
				.then(Commands.literal("info").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(DevTestCommands::info)))
				.then(Commands.literal("place").then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("item", StringArgumentType.string()).executes(DevTestCommands::place))))
				.then(Commands.literal("open").then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("player", EntityArgument.player()).executes(DevTestCommands::open))))
				.then(Commands.literal("break").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(DevTestCommands::breakBlock)))
				.then(Commands.literal("request").then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("item", StringArgumentType.string()).then(Commands.argument("count", IntegerArgumentType.integer(1))
								.executes(DevTestCommands::request)))))
				.then(Commands.literal("put").then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("inv", StringArgumentType.word()).then(Commands.argument("slot", IntegerArgumentType.integer(0))
								.then(Commands.argument("item", StringArgumentType.string()).then(Commands.argument("count", IntegerArgumentType.integer(0))
										.executes(DevTestCommands::put)))))))));
	}

	private static int reply(CommandContext<CommandSourceStack> ctx, String msg) {
		ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
		return 1;
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	/** Crafts one of every crafting recipe through the recipe manager lookup the crafting grid uses, and lists LP items without a recipe. */
	private static int checkRecipes(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		ContextMap display = SlotDisplayContext.fromLevel(level);
		Set<String> crafted = new TreeSet<>();
		List<String> failures = new ArrayList<>();
		int lpRecipes = 0;
		for (RecipeHolder<CraftingRecipe> holder : level.recipeAccess().getAllOfType(RecipeType.CRAFTING)) {
			if (!holder.id().identifier().getNamespace().equals(LPConstants.LP_MOD_ID)) continue;
			lpRecipes++;
			for (RecipeDisplay d : holder.value().display()) {
				CraftingInput input;
				if (d instanceof ShapedCraftingRecipeDisplay shaped) {
					input = CraftingInput.of(shaped.width(), shaped.height(), shaped.ingredients().stream().map(s -> s.resolveForFirstStack(display)).toList());
				} else if (d instanceof ShapelessCraftingRecipeDisplay shapeless) {
					List<ItemStack> stacks = shapeless.ingredients().stream().map(s -> s.resolveForFirstStack(display)).toList();
					input = CraftingInput.of(3, 3, padTo9(stacks));
				} else {
					continue;
				}
				ItemStack expected = d.result().resolveForFirstStack(display);
				Optional<RecipeHolder<CraftingRecipe>> found = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
				ItemStack result = found.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
				if (result.isEmpty() || result.getItem() != expected.getItem()) {
					failures.add(holder.id().identifier() + " -> " + (found.isEmpty() ? "no match" : result));
				} else {
					crafted.add(BuiltInRegistries.ITEM.getKey(result.getItem()).getPath());
				}
			}
		}
		List<String> missing = BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals(LPConstants.LP_MOD_ID))
				.map(Identifier::getPath).filter(p -> !crafted.contains(p)).sorted().toList();
		return reply(ctx, "lp recipes=" + lpRecipes + " virtual=" + logisticspipes.recipes.RecipeManager.craftingManager.virtualRecipes.size()
				+ " craftedItems=" + crafted.size() + "\nfailures=" + failures + "\nnoRecipe=" + missing);
	}

	/** Fills a vanilla crafting table menu with the inputs of the item's LP recipe and shift-clicks the result, like a player. */
	private static int craft(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		Item wanted = item(StringArgumentType.getString(ctx, "item"));
		ContextMap display = SlotDisplayContext.fromLevel(level);
		for (RecipeHolder<CraftingRecipe> holder : level.recipeAccess().getAllOfType(RecipeType.CRAFTING)) {
			for (RecipeDisplay d : holder.value().display()) {
				if (d.result().resolveForFirstStack(display).getItem() != wanted) continue;
				List<ItemStack> grid;
				int width;
				if (d instanceof ShapedCraftingRecipeDisplay shaped) {
					width = shaped.width();
					grid = shaped.ingredients().stream().map(st -> st.resolveForFirstStack(display)).toList();
				} else if (d instanceof ShapelessCraftingRecipeDisplay shapeless) {
					width = 3;
					grid = shapeless.ingredients().stream().map(st -> st.resolveForFirstStack(display)).toList();
				} else {
					continue;
				}
				FakePlayer player = FakePlayer.get(level);
				player.getInventory().clearContent();
				CraftingMenu menu = new CraftingMenu(0, player.getInventory(), ContainerLevelAccess.create(level, BlockPos.ZERO));
				for (int i = 0; i < grid.size(); i++) {
					menu.getSlot(1 + (i / width) * 3 + i % width).set(grid.get(i).copy());
				}
				String preview = menu.getSlot(0).getItem().toString();
				menu.quickMoveStack(player, 0);
				List<String> left = new ArrayList<>();
				for (int i = 1; i <= 9; i++) {
					if (menu.getSlot(i).hasItem()) left.add(menu.getSlot(i).getItem().toString());
				}
				String got = describe(player.getInventory());
				menu.removed(player);
				player.getInventory().clearContent();
				return reply(ctx, holder.id().identifier() + " inputs=" + grid.stream().filter(st -> !st.isEmpty()).map(ItemStack::toString).toList()
						+ " preview=" + preview + " playerGot=" + got + " gridLeft=" + left);
			}
		}
		return reply(ctx, "no crafting recipe for " + wanted);
	}

	private static List<ItemStack> padTo9(List<ItemStack> stacks) {
		List<ItemStack> out = new ArrayList<>(stacks);
		while (out.size() < 9) out.add(ItemStack.EMPTY);
		return out;
	}

	private static String describe(Container inv) {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty()) out.add(i + ":" + s.getCount() + "x" + BuiltInRegistries.ITEM.getKey(s.getItem()).getPath() + (s.getComponentsPatch().isEmpty() ? "" : "*"));
		}
		return out.toString();
	}

	private static int info(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
		BlockEntity be = level.getBlockEntity(pos);
		StringBuilder sb = new StringBuilder(level.getBlockState(pos).toString());
		if (be instanceof LogisticsPowerJunctionTileEntity junction) {
			sb.append(" power=").append(junction.getPowerLevel());
		}
		if (be instanceof logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity provider) {
			sb.append(" provider=").append(provider.getDisplayPowerLevel()).append('/').append(provider.getMaxStorage());
		}
		if (be != null) {
			team.reborn.energy.api.EnergyStorage energy = team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, null);
			if (energy != null) sb.append(" E=").append(energy.getAmount()).append('/').append(energy.getCapacity());
		}
		if (be instanceof LogisticsCraftingTableTileEntity table) {
			sb.append(" matrix=").append(describe(table.matrix)).append(" inv=").append(describe(table.inv));
		}
		if (be instanceof LogisticsTileGenericPipe tile && tile.pipe != null) {
			CoreUnroutedPipe pipe = tile.pipe;
			sb.append(" pipe=").append(pipe.getClass().getSimpleName());
			if (pipe.transport instanceof PipeTransportLogistics transport) sb.append(" inTransit=").append(transport.items.stored().count());
			if (pipe instanceof CoreRoutedPipe routed) {
				sb.append(" upgrades=").append(describe(routed.getOriginalUpgradeManager().inv));
				sb.append(" powerProviders=").append(routed.getRouter().getSubSystemPowerProvider().stream().map(p -> p.getValue1().getBrand()).toList());
				CompoundTag nbt = new CompoundTag();
				routed.writeToNBT(nbt);
				sb.append(" supplierBuffer=").append(nbt.getCompoundOrEmpty("powerHandler"));
				for (Direction dir : Direction.values()) {
					BlockEntity n = level.getBlockEntity(pos.relative(dir));
					if (n == null || n instanceof LogisticsTileGenericPipe) continue;
					sb.append(' ').append(dir).append(":connect=").append(routed.canPipeConnect(n, dir))
							.append(",receiver=").append(SimpleServiceLocator.powerProxy.isEnergyReceiver(n, dir.getOpposite()));
				}
			}
			if (pipe instanceof PipeLogisticsChassis chassis) sb.append(" modules=").append(describe(chassis.getModuleInventory()));
			if (pipe instanceof PipeItemsCraftingLogistics crafting && crafting.getLogisticsModule() instanceof ModuleCrafter module) {
				sb.append(" recipe=").append(describe(module.dummyInventory));
			}
		}
		return reply(ctx, sb.toString());
	}

	/** Breaks the block as a survival player with an iron pickaxe would and lists the item entities that dropped. */
	private static int breakBlock(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
		AABB area = new AABB(pos).inflate(2);
		level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
		FakePlayer player = FakePlayer.get(level);
		player.setGameMode(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
		boolean broken = player.gameMode.destroyBlock(pos);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		String drops = level.getEntitiesOfClass(ItemEntity.class, area).stream()
				.map(e -> e.getItem().getCount() + "x" + BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getPath() + (e.getItem().getComponentsPatch().isEmpty() ? "" : "*"))
				.collect(Collectors.joining(", "));
		return reply(ctx, "broken=" + broken + " now=" + level.getBlockState(pos).getBlock() + " drops=[" + drops + "]");
	}

	/** Places an item at an empty position as a survival player clicking the block below. */
	private static int place(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
		FakePlayer player = FakePlayer.get(level);
		player.setGameMode(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(StringArgumentType.getString(ctx, "item"))));
		InteractionResult result = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return reply(ctx, result + " " + level.getBlockState(pos));
	}

	private static int open(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
		if (!(ctx.getSource().getLevel().getBlockEntity(pos) instanceof logisticspipes.interfaces.IGuiTileEntity tile)) return reply(ctx, "no LP gui at " + pos);
		tile.getGuiProvider().setTilePos((BlockEntity) tile).open(EntityArgument.getPlayer(ctx, "player"));
		return reply(ctx, "opened");
	}

	private static int request(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
		if (!(level.getBlockEntity(pos) instanceof LogisticsTileGenericPipe tile) || !(tile.pipe instanceof IRequestItems requester)) {
			return reply(ctx, "no requesting pipe at " + pos);
		}
		ItemStack stack = new ItemStack(item(StringArgumentType.getString(ctx, "item")), IntegerArgumentType.getInteger(ctx, "count"));
		int promised = RequestTree.requestPartial(ItemIdentifierStack.getFromStack(stack), requester, null);
		return reply(ctx, "promised=" + promised);
	}

	/** Sets a slot of a pipe inventory: inv = module (chassis), upgrade, recipe (crafting pipe), or table (crafting table matrix). */
	private static int put(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
		BlockEntity be = level.getBlockEntity(pos);
		CoreUnroutedPipe pipe = be instanceof LogisticsTileGenericPipe tile ? tile.pipe : null;
		Container inv = switch (StringArgumentType.getString(ctx, "inv")) {
			case "module" -> pipe instanceof PipeLogisticsChassis chassis ? chassis.getModuleInventory() : null;
			case "upgrade" -> pipe instanceof CoreRoutedPipe routed ? routed.getOriginalUpgradeManager().inv : null;
			case "recipe" -> pipe instanceof PipeItemsCraftingLogistics crafting && crafting.getLogisticsModule() instanceof ModuleCrafter module ? module.dummyInventory : null;
			case "table" -> be instanceof LogisticsCraftingTableTileEntity table ? table.matrix : null;
			case "tableinv" -> be instanceof LogisticsCraftingTableTileEntity table ? table.inv : null;
			default -> null;
		};
		if (inv == null) return reply(ctx, "no such inventory at " + pos);
		int count = IntegerArgumentType.getInteger(ctx, "count");
		inv.setItem(IntegerArgumentType.getInteger(ctx, "slot"), count == 0 ? ItemStack.EMPTY : new ItemStack(item(StringArgumentType.getString(ctx, "item")), count));
		inv.setChanged();
		be.setChanged();
		return reply(ctx, describe(inv));
	}
}
