package logisticspipes.proxy;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.LevelEvent;

import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.bus.api.SubscribeEvent;

import com.google.common.collect.Maps;
import lombok.Getter;

import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.entity.FakePlayerLP;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.interfaces.IProxy;
import logisticspipes.routing.debug.RoutingTableDebugUpdateThread;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.ticks.RoutingTableUpdateThread;
import logisticspipes.utils.PlayerCollectionList;

public class MainProxy {

	private MainProxy() {}

	/**
	 * Side-specific proxy: ClientProxy on client dist, ServerProxy on dedicated server.
	 * Replaces 1.12.2 {@code @SidedProxy} annotation.
	 */
	// NeoForge 1.20.1: DistExecutor removed — use FMLEnvironment.dist check
	public static IProxy proxy = net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
			? new logisticspipes.proxy.side.ClientProxy()
			: new logisticspipes.proxy.side.ServerProxy();

	@Getter
	private static int globalTick;

	private static final WeakHashMap<Thread, LogicalSide> threadSideMap = new WeakHashMap<>();
	private static final Map<ResourceKey<Level>, FakePlayerLP> fakePlayers = Maps.newHashMap();

	public static final String networkChannelName = "LogisticsPipes";

	// ── Side detection ────────────────────────────────────────────────────────

	private static LogicalSide getEffectiveSide() {
		Thread thr = Thread.currentThread();
		if (MainProxy.threadSideMap.containsKey(thr)) {
			return MainProxy.threadSideMap.get(thr);
		}
		LogicalSide side = MainProxy.getEffectiveSide(thr);
		if (MainProxy.threadSideMap.size() > 50) {
			MainProxy.threadSideMap.clear();
		}
		MainProxy.threadSideMap.put(thr, side);
		return side;
	}

	private static LogicalSide getEffectiveSide(Thread thr) {
		if (thr.getName().equals("Server thread")
				|| (thr instanceof RoutingTableUpdateThread)
				|| (thr instanceof RoutingTableDebugUpdateThread)) {
			return LogicalSide.SERVER;
		}
		// ComputerCraft Lua-thread check removed — CC has no 1.20.1 port (former dummy always returned false).
		return LogicalSide.CLIENT;
	}

	/** Use {@link #isClient(Level)} when a level is available; this thread-based fallback is slow. */
	@Deprecated
	public static boolean isClient() {
		return MainProxy.getEffectiveSide() == LogicalSide.CLIENT;
	}

	/** Use {@link #isServer(Level)} when a level is available; this thread-based fallback is slow. */
	@Deprecated
	public static boolean isServer() {
		return MainProxy.getEffectiveSide() == LogicalSide.SERVER;
	}

	public static boolean isClient(Level level) {
		// Mirror isServer(Level): fall back to thread detection when no level is available
		// (e.g. a pipe queried before its container is bound).
		if (level == null) return MainProxy.getEffectiveSide() == LogicalSide.CLIENT;
		return level.isClientSide;
	}

	public static boolean isServer(Level level) {
		if (level == null) return MainProxy.getEffectiveSide() == LogicalSide.SERVER;
		return !level.isClientSide;
	}

	/**
	 * Accepts any {@link LevelAccessor} (e.g. from {@code BlockEvent.getLevel()}).
	 * Falls back to thread detection if the accessor is not a full {@link Level}.
	 */
	public static boolean isServer(@Nullable LevelAccessor levelAccessor) {
		if (levelAccessor instanceof Level level) {
			return !level.isClientSide;
		}
		return MainProxy.isServer();
	}

	public static boolean isClient(@Nullable LevelAccessor levelAccessor) {
		if (levelAccessor instanceof Level level) {
			return level.isClientSide;
		}
		return MainProxy.isClient();
	}

	public static void runOnServer(@Nullable LevelAccessor world, @Nonnull Supplier<Runnable> runnableConsumer) {
		if (isServer(world)) runnableConsumer.get().run();
	}

	public static void runOnClient(@Nullable LevelAccessor world, @Nonnull Supplier<Runnable> runnableConsumer) {
		if (isClient(world)) runnableConsumer.get().run();
	}

	public static Level getClientMainWorld() {
		return MainProxy.proxy.getWorld();
	}

	// ── Networking ────────────────────────────────────────────────────────────

	/** Sends a packet from the client to the server. */
	public static void sendPacketToServer(ModernPacket packet) {
		if (MainProxy.isServer()) {
			LogisticsPipes.log.error("sendPacketToServer called server-side!");
			return;
		}
		logisticspipes.network.PacketHandler.sendToServer(packet);
	}

	/** Sends a packet from the server to a specific player. */
	public static void sendPacketToPlayer(ModernPacket packet, Player player) {
		if (!MainProxy.isServer()) {
			LogisticsPipes.log.error("sendPacketToPlayer called client-side!");
			return;
		}
		logisticspipes.network.PacketHandler.sendToPlayer(packet, player);
	}

	// ── Chunk-watch / broadcast helpers ──────────────────────────────────────

	public static boolean isAnyoneWatching(BlockPos pos, int dimensionID) {
		net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(pos);
		PlayerCollectionList list = logisticspipes.LogisticsEventListener.watcherList.get(chunkPos);
		return list != null && !list.isEmpty();
	}

	public static boolean isAnyoneWatching(int X, int Z, int dimensionID) {
		net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(
				net.minecraft.core.SectionPos.blockToSectionCoord(X),
				net.minecraft.core.SectionPos.blockToSectionCoord(Z));
		PlayerCollectionList list = logisticspipes.LogisticsEventListener.watcherList.get(chunkPos);
		return list != null && !list.isEmpty();
	}

	public static void sendPacketToAllWatchingChunk(LogisticsModule module, ModernPacket packet) {
		if (module == null || module.getBlockPos() == null) return;
		net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(module.getBlockPos());
		sendPacketToChunkWatchers(chunkPos, packet);
	}

	public static void sendPacketToAllWatchingChunk(BlockEntity tile, ModernPacket packet) {
		if (tile == null) return;
		net.minecraft.world.level.Level lvl = tile.getLevel();
		if (lvl instanceof net.minecraft.server.level.ServerLevel sl) {
			net.minecraft.world.level.chunk.LevelChunk chunk = sl.getChunkAt(tile.getBlockPos());
			logisticspipes.network.PacketHandler.CHANNEL.send(
				net.minecraftforge.network.PacketDistributor.TRACKING_CHUNK.with(() -> chunk),
				logisticspipes.network.PacketHandler.buildPayloadPublic(packet));
			return;
		}
		net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(tile.getBlockPos());
		sendPacketToChunkWatchers(chunkPos, packet);
	}

	public static void sendPacketToAllWatchingChunk(int X, int Z, int dimensionId, ModernPacket packet) {
		net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(
				net.minecraft.core.SectionPos.blockToSectionCoord(X),
				net.minecraft.core.SectionPos.blockToSectionCoord(Z));
		sendPacketToChunkWatchers(chunkPos, packet);
	}

	private static void sendPacketToChunkWatchers(net.minecraft.world.level.ChunkPos chunkPos, ModernPacket packet) {
		PlayerCollectionList list = logisticspipes.LogisticsEventListener.watcherList.get(chunkPos);
		if (list != null) {
			list.players().forEach(p -> sendPacketToPlayer(packet, p));
		}
	}

	public static void sendToPlayerList(ModernPacket packet, PlayerCollectionList players) {
		players.players().forEach(p -> sendPacketToPlayer(packet, p));
	}

	public static void sendToPlayerList(ModernPacket packet, Iterable<Player> players) {
		players.forEach(p -> sendPacketToPlayer(packet, p));
	}

	public static void sendToPlayerList(ModernPacket packet, Stream<Player> players) {
		players.forEach(p -> sendPacketToPlayer(packet, p));
	}

	public static void sendToAllPlayers(ModernPacket packet) {
		if (!MainProxy.isServer()) {
			LogisticsPipes.log.error("sendToAllPlayers called client-side!");
			return;
		}
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;
		for (ServerLevel level : server.getAllLevels()) {
			for (Player player : level.players()) {
				MainProxy.sendPacketToPlayer(packet, player);
			}
		}
	}

	// ── Fake player ──────────────────────────────────────────────────────────

	@Nullable
	public static FakePlayer getFakePlayer(Level level) {
		if (!(level instanceof ServerLevel serverLevel)) return null;
		ResourceKey<Level> dim = level.dimension();
		if (fakePlayers.containsKey(dim)) return fakePlayers.get(dim);
		FakePlayerLP fp = new FakePlayerLP(serverLevel);
		fakePlayers.put(dim, fp);
		return fp;
	}

	// ── Misc ─────────────────────────────────────────────────────────────────

	public static void addTick() {
		MainProxy.globalTick++;
	}

	public static ItemEntity dropItems(Level world, @Nonnull ItemStack stack, int xCoord, int yCoord, int zCoord) {
		ItemEntity item = new ItemEntity(world, xCoord, yCoord, zCoord, stack);
		world.addFreshEntity(item);
		return item;
	}

	public static boolean checkPipesConnections(BlockEntity from, BlockEntity to, Direction way) {
		return MainProxy.checkPipesConnections(from, to, way, false);
	}

	public static boolean checkPipesConnections(BlockEntity from, BlockEntity to, Direction way, boolean ignoreSystemDisconnection) {
		if (from == null || to == null) return false;
		IPipeInformationProvider fromInfo = SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(from);
		IPipeInformationProvider toInfo   = SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(to);
		if (fromInfo == null && toInfo == null) return false;
		if (fromInfo != null && !fromInfo.canConnect(to, way, ignoreSystemDisconnection)) return false;
		if (toInfo   != null) return toInfo.canConnect(from, way.getOpposite(), ignoreSystemDisconnection);
		return true;
	}

	public static boolean isPipeControllerEquipped(Player player) {
		return player != null &&
				!player.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() &&
				player.getItemBySlot(EquipmentSlot.MAINHAND).getItem() == LPItems.pipeController.get();
	}

	@SubscribeEvent
	public static void onWorldUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof Level level) {
			fakePlayers.keySet().removeIf(key -> key.equals(level.dimension()));
		}
	}

	private static boolean needsToBeCompressed(ModernPacket packet) {
		return false;
	}
}
