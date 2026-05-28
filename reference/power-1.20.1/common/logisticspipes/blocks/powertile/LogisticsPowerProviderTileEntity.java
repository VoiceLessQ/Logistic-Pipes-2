package logisticspipes.blocks.powertile;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.CrashReportCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.core.Direction;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.gui.hud.HUDPowerLevel;
import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.IHeadUpDisplayBlockRendererProvider;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.PowerProviderGui;
import logisticspipes.network.packets.block.PowerProviderLevel;
import logisticspipes.network.packets.hud.HUDStartBlockWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopBlockWatchingPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.utils.tuples.Triplet;
import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

@CCType(name = "LogisticsPowerProvider")
public abstract class LogisticsPowerProviderTileEntity extends LogisticsSolidTileEntity
		implements IGuiTileEntity, ISubSystemPowerProvider, IPowerLevelDisplay, IGuiOpenControler, IHeadUpDisplayBlockRendererProvider, IBlockWatchingHandler {

	public static final int BC_COLOR = 0x00ffff;
	public static final int RF_COLOR = 0xff0000;
	public static final int IC2_COLOR = 0xffff00;

	// true if it needs more power, turns off at full, turns on at 50%.
	public boolean needMorePowerTriggerCheck = true;

	protected Map<Integer, Double> orders = new HashMap<>();
	protected BitSet reOrdered = new BitSet(ServerRouter.getBiggestSimpleID());
	protected boolean pauseRequesting = false;

	protected double internalStorage = 0;
	protected int maxMode = 1;
	private double lastUpdateStorage = 0;
	private PlayerCollectionList guiListener = new PlayerCollectionList();
	private PlayerCollectionList watcherList = new PlayerCollectionList();
	private IHeadUpDisplayRenderer HUD;
	private boolean init = false;

	protected LogisticsPowerProviderTileEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(type, pos, state);
		HUD = new HUDPowerLevel(this);
	}

	@Override
	public void update() {
		super.update();
		pauseRequesting = false;
		if (!init) {
			if (MainProxy.isClient(getWorld())) {
				LogisticsHUDRenderer.instance().add(this);
			}
			init = true;
		}
		double globalRequest = orders.values().stream().reduce(Double::sum).orElse(0.0);
		if (globalRequest > 0) {
			final double fullfillRatio = Math.min(1, Math.min(internalStorage, getMaxProvidePerTick()) / globalRequest);
			if (fullfillRatio > 0) {
				final Function<NeighborTileEntity<LogisticsTileGenericPipe>, CoreRoutedPipe> getPipe =
						(NeighborTileEntity<LogisticsTileGenericPipe> neighbor) -> (CoreRoutedPipe) neighbor.getTileEntity().pipe;
				orders.entrySet().stream()
						.map(routerIdToOrderCount -> new Pair<>(SimpleServiceLocator.routerManager.getRouter(routerIdToOrderCount.getKey()),
								Math.min(internalStorage, routerIdToOrderCount.getValue() * fullfillRatio)))
						.filter(destinationToPower -> destinationToPower.getValue1() != null && destinationToPower.getValue1().getPipe() != null)
						.forEach(destinationToPower -> new WorldCoordinatesWrapper(this)
								.allNeighborTileEntities().stream()
								.flatMap(neighbor -> LPNeighborTileEntityKt.optionalIs(neighbor, LogisticsTileGenericPipe.class).map(Stream::of).orElseGet(Stream::empty))
								.filter(neighbor -> neighbor.getTileEntity().pipe instanceof CoreRoutedPipe &&
										!getPipe.apply(neighbor).stillNeedReplace())
								.flatMap(neighbor -> getPipe.apply(neighbor).getRouter().getDistanceTo(destinationToPower.getValue1()).stream()
										.map(exitRoute -> new Pair<>(neighbor, exitRoute)))
								.filter(neighborToExit -> neighborToExit.getValue2().containsFlag(PipeRoutingConnectionType.canPowerSubSystemFrom) &&
										neighborToExit.getValue2().filters.stream().noneMatch(IFilter::blockPower))
								.findFirst()
								.ifPresent(neighborToSource -> {
									CoreRoutedPipe sourcePipe = getPipe.apply(neighborToSource.getValue1());
									if (sourcePipe.isInitialized()) {
										sourcePipe.container.addLaser(neighborToSource.getValue1().getOurDirection(), 1, getLaserColor(), true, true);
									}
									sendPowerLaserPackets(sourcePipe.getRouter(), destinationToPower.getValue1(), neighborToSource.getValue2().exitOrientation,
											neighborToSource.getValue2().exitOrientation != neighborToSource.getValue1().getDirection());
									internalStorage -= destinationToPower.getValue2();
									if (internalStorage <= 0) internalStorage = 0; // because calculations with floats
									handlePower(destinationToPower.getValue1().getPipe(), destinationToPower.getValue2());
								}));
			}
		}
		orders.clear();
		if (MainProxy.isServer(getWorld())) {
			if (internalStorage != lastUpdateStorage) {
				updateClients();
				lastUpdateStorage = internalStorage;
			}
		}
	}

	protected abstract void handlePower(CoreRoutedPipe pipe, double toSend);

	private void sendPowerLaserPackets(IRouter sourceRouter, IRouter destinationRouter, Direction exitOrientation, boolean addBall) {
		if (sourceRouter == destinationRouter) {
			return;
		}
		LinkedList<Triplet<IRouter, Direction, Boolean>> todo = new LinkedList<>();
		todo.add(new Triplet<>(sourceRouter, exitOrientation, addBall));
		while (!todo.isEmpty()) {
			Triplet<IRouter, Direction, Boolean> part = todo.pollFirst();
			List<ExitRoute> exits = part.getValue1().getRoutersOnSide(part.getValue2());
			for (ExitRoute exit : exits) {
				if (exit.containsFlag(PipeRoutingConnectionType.canPowerSubSystemFrom)) { // Find only result (caused by only straight connections)
					int distance = part.getValue1().getDistanceToNextPowerPipe(exit.exitOrientation);
					CoreRoutedPipe pipe = part.getValue1().getPipe();
					if (pipe != null && pipe.isInitialized()) {
						pipe.container.addLaser(exit.exitOrientation, distance, getLaserColor(), false, part.getValue3());
					}
					IRouter nextRouter = exit.destination; // Use new sourceRouter
					if (nextRouter == destinationRouter) {
						return;
					}
					outerRouters:
					for (ExitRoute newExit : nextRouter.getDistanceTo(destinationRouter)) {
						if (newExit.containsFlag(PipeRoutingConnectionType.canPowerSubSystemFrom)) {
							for (IFilter filter : newExit.filters) {
								if (filter.blockPower()) {
									continue outerRouters;
								}
							}
							todo.addLast(new Triplet<>(nextRouter, newExit.exitOrientation, newExit.exitOrientation != exit.exitOrientation));
						}
					}
				}
			}
		}
	}

	protected abstract double getMaxProvidePerTick();

	@CCCommand(description = "Returns the color for the power provided by this power provider")
	protected abstract int getLaserColor();

	@Override
	@CCCommand(description = "Returns the max. amount of storable power")
	public abstract int getMaxStorage();

	@Override
	@CCCommand(description = "Returns the power type stored in this power provider")
	public abstract String getBrand();

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (MainProxy.isClient(getWorld())) {
			LogisticsHUDRenderer.instance().remove(this);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (MainProxy.isClient(getWorld())) {
			init = false;
		}
	}

	// onChunkUnload removed in 1.20.1 — setRemoved() covers this case

	@Override
	public void requestPower(int destination, double amount) {
		if (pauseRequesting) {
			return;
		}
		if (orders.containsKey(destination)) {
			if (reOrdered.get(destination)) {
				pauseRequesting = true;
				reOrdered.clear();
			} else {
				reOrdered.set(destination);
			}
		} else {
			reOrdered.clear();
		}
		orders.put(destination, amount);
	}

	@Override
	@CCCommand(description = "Returns the current power level for this power provider")
	public double getPowerLevel() {
		return lastUpdateStorage;
	}

	@Override
	public boolean usePaused() {
		return pauseRequesting;
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		if (nbt.get("internalStorage") instanceof FloatTag) { // support for old float
			internalStorage = nbt.getFloat("internalStorage");
		} else {
			internalStorage = nbt.getDouble("internalStorage");
		}
		maxMode = nbt.getInt("maxMode");

	}

	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.putDouble("internalStorageDouble", internalStorage);
		nbt.putInt("maxMode", maxMode);
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return HUD;
	}

	@Override
	public net.minecraft.world.level.Level getLevelForHUD() {
		return getWorld();
	}

	@Override
	public int getX() {
		return getBlockPos().getX();
	}

	@Override
	public int getY() {
		return getBlockPos().getY();
	}

	@Override
	public int getZ() {
		return getBlockPos().getZ();
	}

	@Override
	public void startWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartBlockWatchingPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void stopWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopBlockWatchingPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void playerStartWatching(Player player) {
		watcherList.add(player);
		updateClients();
	}

	@Override
	public void playerStopWatching(Player player) {
		watcherList.remove(player);
	}

	@Override
	public boolean isHUDExistent() {
		return getWorld().getBlockEntity(getBlockPos()) == this;
	}

	@Override
	public void guiOpenedByPlayer(Player player) {
		guiListener.add(player);
		updateClients();
	}

	@Override
	public void guiClosedByPlayer(Player player) {
		guiListener.remove(player);
	}

	public void updateClients() {
		MainProxy.sendToPlayerList(PacketHandler.getPacket(PowerProviderLevel.class).putDouble(internalStorage).setTilePos(this), guiListener);
		MainProxy.sendToPlayerList(PacketHandler.getPacket(PowerProviderLevel.class).putDouble(internalStorage).setTilePos(this), watcherList);
	}

	@Override
	public void fillCrashReportCategory(CrashReportCategory par1CrashReportCategory) {
		super.fillCrashReportCategory(par1CrashReportCategory);
		par1CrashReportCategory.setDetail("LP-Version", LogisticsPipes.getVersionString());
	}

	public void handlePowerPacket(double d) {
		if (MainProxy.isClient(getWorld())) {
			internalStorage = d;
		}
	}

	@Override
	public int getChargeState() {
		return (int) Math.min(100F, internalStorage * 100 / getMaxStorage());
	}

	@Override
	public int getDisplayPowerLevel() {
		return internalStorage > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) internalStorage;
	}

	@Override
	public boolean isHUDInvalid() {
		return isRemoved();
	}

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(PowerProviderGui.class);
	}
}
