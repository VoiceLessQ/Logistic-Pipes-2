package logisticspipes;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.CustomData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import logisticspipes.config.Configs;
import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.PlayerConfigToClientPacket;
import logisticspipes.network.packets.chassis.ChestGuiClosed;
import logisticspipes.network.packets.chassis.ChestGuiOpened;
import logisticspipes.network.packets.gui.GuiReopenPacket;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.GuiOverlay;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.ticks.VersionChecker;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.PlayerIdentifier;
import logisticspipes.utils.QuickSortChestMarkerStorage;
import logisticspipes.utils.string.ChatColor;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.config.PlayerConfiguration;
import network.rs485.logisticspipes.module.AsyncQuicksortModule;
import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsEventListener {

	public static final WeakHashMap<Player, List<WeakReference<AsyncQuicksortModule>>> chestQuickSortConnection = new WeakHashMap<>();
	public static Map<ChunkPos, PlayerCollectionList> watcherList = new ConcurrentHashMap<>();

	public static void register() {
		LogisticsEventListener listener = new LogisticsEventListener();
		ServerEntityEvents.ALLOW_LOAD.register((entity, level, reason, loadedFromDisk) -> listener.onEntitySpawn(entity));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> listener.onPlayerLeftClickBlock(player, pos));
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			listener.onPlayerRightClickBlock(player, hit.getBlockPos());
			return InteractionResult.PASS;
		});
		ServerLevelEvents.LOAD.register((server, level) -> listener.WorldLoad(level));
		ServerLevelEvents.UNLOAD.register((server, level) -> listener.WorldUnload(level));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> listener.onPlayerLogin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> listener.onPlayerLogout(handler.player));
	}

	private static CompoundTag customData(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag();
	}

	/** @return false to cancel the entity joining the level. */
	public boolean onEntitySpawn(Entity entity) {
		if (entity instanceof ItemEntity itemEntity) {
			ItemStack stack = itemEntity.getItem();
			if (!stack.isEmpty() && stack.getItem() instanceof IItemAdvancedExistance && !((IItemAdvancedExistance) stack.getItem()).canExistInWorld(stack)) {
				return false;
			}
			CompoundTag tag = customData(stack);
			if (tag != null) {
				for (String key : tag.keySet()) {
					if (key.startsWith("logisticspipes:routingdata")) {
						ItemRoutingInformation info = ItemRoutingInformation.restoreFromNBT(tag.getCompoundOrEmpty(key));
						info.setItemTimedout();
						itemEntity.setItem(info.getItem().getItem().makeNormalStack(stack.getCount()));
						break;
					}
				}
			}
		}
		return true;
	}

	public InteractionResult onPlayerLeftClickBlock(Player player, BlockPos clickedPos) {
		if (MainProxy.isServer(player.level())) {
			final BlockEntity tile = player.level().getBlockEntity(clickedPos);
			if (tile instanceof LogisticsTileGenericPipe) {
				if (((LogisticsTileGenericPipe) tile).pipe instanceof CoreRoutedPipe) {
					if (!((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).canBeDestroyedByPlayer(player)) {
						player.sendSystemMessage(Component.translatable("lp.chat.permissiondenied"));
						((LogisticsTileGenericPipe) tile).scheduleNeighborChange();
						Level world = player.level();
						BlockPos pos = tile.getBlockPos();
						BlockState state = world.getBlockState(pos);
						world.sendBlockUpdated(pos, state, state, 2);
						((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).delayTo = System.currentTimeMillis() + 200;
						((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).repeatFor = 10;
						return InteractionResult.FAIL;
					} else {
						((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).setDestroyByPlayer();
					}
				}
			}
		}
		return InteractionResult.PASS;
	}

	public void onPlayerRightClickBlock(Player player, BlockPos pos) {
		if (MainProxy.isClient(player.level())) return;
		Level world = player.level();
		BlockEntity te = world.getBlockEntity(pos);
		if (te == null) return;
		// Only act on blocks that expose an item handler (chests, barrels, etc.)
		if (ItemStorage.SIDED.find(world, pos, null) == null) return;

		List<WeakReference<AsyncQuicksortModule>> modules = null;

		for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
			BlockPos neighborPos = pos.relative(dir);
			BlockEntity neighbor = world.getBlockEntity(neighborPos);
			if (!(neighbor instanceof LogisticsTileGenericPipe)) continue;
			LogisticsTileGenericPipe pipeTile = (LogisticsTileGenericPipe) neighbor;
			if (!(pipeTile.pipe instanceof PipeLogisticsChassis)) continue;
			PipeLogisticsChassis chassis = (PipeLogisticsChassis) pipeTile.pipe;
			// The chassis must be pointing at the clicked block
			if (chassis.getPointedOrientation() != dir.getOpposite()) continue;

			final List<WeakReference<AsyncQuicksortModule>> found = modules == null ? new ArrayList<>() : modules;
			chassis.getModules().getModules()
					.filter(m -> m instanceof AsyncQuicksortModule)
					.forEach(m -> found.add(new WeakReference<>((AsyncQuicksortModule) m)));
			if (!found.isEmpty()) modules = found;
		}

		if (modules != null && !modules.isEmpty()) {
			chestQuickSortConnection.put(player, modules);
		}
	}

	public static HashMap<ResourceKey<Level>, Long> WorldLoadTime = new HashMap<>();

	public void WorldLoad(Level level) {
		if (MainProxy.isServer(level)) {
			ResourceKey<Level> dim = level.dimension();
			if (!LogisticsEventListener.WorldLoadTime.containsKey(dim)) {
				LogisticsEventListener.WorldLoadTime.put(dim, System.currentTimeMillis());
			}
		}
		if (MainProxy.isClient(level)) {
			SimpleServiceLocator.routerManager.clearClientRouters();
			LogisticsHUDRenderer.instance().clear();
		}
	}

	public void WorldUnload(Level level) {
		if (MainProxy.isServer(level)) {
			SimpleServiceLocator.routerManager.dimensionUnloaded(level.dimension().identifier());
		}
	}

	/** Called by ChunkMapMixin when a chunk is queued for a player (Forge ChunkWatchEvent.Watch). */
	public static void watchChunk(ServerPlayer player, ChunkPos pos) {
		LogisticsEventListener.watcherList.computeIfAbsent(pos, p -> new PlayerCollectionList()).add(player);
	}

	/** Called by ChunkMapMixin when a chunk is dropped for a player (Forge ChunkWatchEvent.UnWatch). */
	public static void unWatchChunk(ServerPlayer player, ChunkPos pos) {
		if (LogisticsEventListener.watcherList.containsKey(pos)) {
			LogisticsEventListener.watcherList.get(pos).remove(player);
		}
	}

	public void onPlayerLogin(ServerPlayer player) {
		SimpleServiceLocator.securityStationManager.sendClientAuthorizationList(player);

		SimpleServiceLocator.serverBufferHandler.clear(player);
		ClientConfiguration config = LogisticsPipes.getServerConfigManager().getPlayerConfiguration(PlayerIdentifier.get(player));
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PlayerConfigToClientPacket.class).setConfig(config), player);
	}

	public void onPlayerLogout(ServerPlayer player) {
		SimpleServiceLocator.serverBufferHandler.clear(player);
	}

	@AllArgsConstructor
	private static class GuiEntry {

		@Getter
		private final int xCoord;
		@Getter
		private final int yCoord;
		@Getter
		private final int zCoord;
		@Getter
		private final int guiID;
		@Getter
		@Setter
		private boolean isActive;
	}

	@Getter(lazy = true)
	private static final Queue<GuiEntry> guiPos = new LinkedList<>();

	// Opening fires before Closing on a screen swap; Closing must not undo it
	private static boolean screenSwapPending = false;

	// Handle GuiReopen — Opening event (screen becoming visible); called by MinecraftMixin.setScreen
	@Environment(EnvType.CLIENT)
	public static void onGuiOpen(Screen currentScreen, Screen newScreen) {
		// Guard: no server connection (e.g. main menu) — nothing to notify
		if (net.minecraft.client.Minecraft.getInstance().getConnection() == null) {
			return;
		}
		if (!LogisticsEventListener.getGuiPos().isEmpty()) {
			GuiEntry part = LogisticsEventListener.getGuiPos().peek();
			part.setActive(true);
		}
		screenSwapPending = currentScreen != null;
		// LP1 matched GuiChest only, not every container (player inventory included)
		if (newScreen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(ChestGuiOpened.class));
		} else {
			QuickSortChestMarkerStorage.getInstance().disable();
			MainProxy.sendPacketToServer(PacketHandler.getPacket(ChestGuiClosed.class));
		}
	}

	// Handle GuiReopen — Closing event (screen being dismissed); called by MinecraftMixin.setScreen
	@Environment(EnvType.CLIENT)
	public static void onGuiClose() {
		// Guard: no server connection (e.g. main menu) — nothing to notify
		if (net.minecraft.client.Minecraft.getInstance().getConnection() == null) {
			return;
		}
		if (!LogisticsEventListener.getGuiPos().isEmpty()) {
			GuiEntry part = LogisticsEventListener.getGuiPos().peek();
			if (part.isActive()) {
				part = LogisticsEventListener.getGuiPos().poll();
				MainProxy.sendPacketToServer(PacketHandler.getPacket(GuiReopenPacket.class).setGuiID(part.getGuiID()).setPosX(part.getXCoord()).setPosY(part.getYCoord()).setPosZ(part.getZCoord()));
				GuiOverlay.getInstance().setOverlaySlotActive(false);
			}
		}
		GuiOverlay.getInstance().setOverlaySlotActive(false);
		if (screenSwapPending) {
			screenSwapPending = false;
		} else {
			// LP1 got a null-screen GuiOpenEvent here; NeoForge only fires Closing
			QuickSortChestMarkerStorage.getInstance().disable();
			MainProxy.sendPacketToServer(PacketHandler.getPacket(ChestGuiClosed.class));
		}
	}

	@Environment(EnvType.CLIENT)
	public static void addGuiToReopen(int xCoord, int yCoord, int zCoord, int guiID) {
		LogisticsEventListener.getGuiPos().add(new GuiEntry(xCoord, yCoord, zCoord, guiID, false));
	}

	@Environment(EnvType.CLIENT)
	public void clientLoggedIn() {
		SimpleServiceLocator.clientBufferHandler.clear();

		if (Configs.CHECK_FOR_UPDATES) {
			LogisticsPipes.singleThreadExecutor.execute(() -> {
				// try to get player entity ten times, once a second
				int times = 0;
				LocalPlayer playerEntity;
				do {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						return;
					}
					playerEntity = Minecraft.getInstance().player;
					++times;
				} while (playerEntity == null && times <= 10);

				if (times > 10) {
					return;
				}

				VersionChecker checker = LogisticsPipes.versionChecker;
				if (checker.isNewVersionAvailable()) {
					LocalPlayer player = playerEntity;
					Minecraft.getInstance().execute(() -> {
						player.sendSystemMessage(Component.literal(checker.getVersionCheckerStatus()));
						player.sendSystemMessage(Component.literal("Use \"/logisticspipes changelog\" to see a changelog."));
					});
				}
			});
		}
	}

	@Environment(EnvType.CLIENT)
	public void onItemStackToolTip(ItemStack stack, List<Component> list) {
		CompoundTag tag = customData(stack);
		if (tag != null) {
			for (String key : tag.keySet()) {
				if (key.startsWith("logisticspipes:routingdata")) {
					ItemRoutingInformation info = ItemRoutingInformation.restoreFromNBT(tag.getCompoundOrEmpty(key));
					list.set(0, Component.literal(ChatColor.RED + "!!! " + ChatColor.WHITE)
							.append(list.get(0))
							.append(Component.literal(ChatColor.RED + " !!!" + ChatColor.WHITE)));
					list.add(1, Component.translatable("itemstackinfo.lprouteditem"));
					list.add(2, Component.translatable("itemstackinfo.lproutediteminfo"));
					list.add(3, Component.literal(TextUtil.translate("itemstackinfo.lprouteditemtype") + ": " + info.getItem().toString()));
				}
			}
		}
	}

	/**
	 * Replaces the 1.12.2 ASM injection of TEControl.validate/invalidate into all TileEntity subclasses.
	 * When any block changes (placed, broken, or neighbour update), check the six adjacent positions for
	 * LP routing pipes and flag their routers for adjacency recheck.  This covers the case where a
	 * non-LP inventory (chest, machine, etc.) is placed or removed next to a provider/requester pipe.
	 */
	public static void onNeighborNotify(Level level, BlockPos changed, Direction skipDirection) {
		if (level.isClientSide()) return;
		for (Direction dir : Direction.values()) {
			if (dir == skipDirection) continue;
			BlockEntity neighbor = level.getBlockEntity(changed.relative(dir));
			if (neighbor instanceof LogisticsTileGenericPipe pipe
					&& pipe.pipe instanceof CoreRoutedPipe routedPipe
					&& !routedPipe.stillNeedReplace()) {
				routedPipe.getRouter().update(false, routedPipe);
			}
		}
	}

	/** Called by ResultSlotMixin (Forge PlayerEvent.ItemCraftedEvent). */
	public static void onItemCrafting(Player player, ItemStack crafting) {
		if (!player.level().isClientSide() && !crafting.isEmpty()) {
			if (BuiltInRegistries.ITEM.getKey(crafting.getItem()).getNamespace().equals(LPConstants.LP_MOD_ID)) {
				PlayerIdentifier identifier = PlayerIdentifier.get(player);
				PlayerConfiguration config = LogisticsPipes.getServerConfigManager().getPlayerConfiguration(identifier);
				if (!config.getHasCraftedLPItem() && !LogisticsPipes.isDEBUG()) {
					ItemStack book = new ItemStack(LPItems.itemGuideBook.get(), 1);
					player.addItem(book);

					config.setHasCraftedLPItem(true);
					LogisticsPipes.getServerConfigManager().setPlayerConfiguration(identifier, config);
				}
			}
		}
	}
}
