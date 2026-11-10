package logisticspipes.routing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.ILogisticsPowerProvider;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class ClientRouter implements IRouter {

	private final int _xCoord;
	private final int _yCoord;
	private final int _zCoord;

	public ClientRouter(UUID id, net.minecraft.resources.Identifier dimension, int xCoord, int yCoord, int zCoord) {
		_xCoord = xCoord;
		_yCoord = yCoord;
		_zCoord = zCoord;
	}

	@Override
	public void destroy() {}

	@Override
	public int getSimpleID() {
		return -420;
	}

	@Override
	public void update(boolean doFullRefresh, CoreRoutedPipe pipe) {}

	@Override
	public boolean isRoutedExit(Direction connection) {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return false;
	}

	@Override
	public boolean hasRoute(int id, boolean flag, ItemIdentifier item) {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return false;
	}

	@Override
	public ExitRoute getExitFor(int id, boolean flag, ItemIdentifier item) {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return null;
	}

	@Override
	public ArrayList<List<ExitRoute>> getRouteTable() {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return new ArrayList<>();
	}

	@Override
	public List<ExitRoute> getIRoutersByCost() {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return new LinkedList<>();
	}

	@Override
	public CoreRoutedPipe getPipe() {
		Level world = MainProxy.proxy.getWorld();
		if (world == null) {
			return null;
		}
		BlockEntity tile = world.getBlockEntity(new BlockPos(_xCoord, _yCoord, _zCoord));

		if (!(tile instanceof LogisticsTileGenericPipe)) {
			return null;
		}
		LogisticsTileGenericPipe pipe = (LogisticsTileGenericPipe) tile;
		if (!(pipe.pipe instanceof CoreRoutedPipe)) {
			return null;
		}
		return (CoreRoutedPipe) pipe.pipe;
	}

	@Override
	public CoreRoutedPipe getCachedPipe() {
		return getPipe();
	}

	@Override
	public boolean isInDim(net.minecraft.resources.Identifier dimension) {
		return true;
	}

	@Override
	public boolean isAt(net.minecraft.resources.Identifier dimension, int xCoord, int yCoord, int zCoord) {
		return _xCoord == xCoord && _yCoord == yCoord && _zCoord == zCoord;
	}

	@Override
	public DoubleCoordinates getLPPosition() {
		return new DoubleCoordinates(_xCoord, _yCoord, _zCoord);
	}

	@Override
	public UUID getId() {
		return UUID.randomUUID();
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		CoreRoutedPipe pipe = getPipe();
		if (pipe == null) {
			return null;
		}
		return pipe.getLogisticsModule();
	}

	@Override
	public void clearPipeCache() {}

	@Override
	public List<Pair<ILogisticsPowerProvider, List<IFilter>>> getPowerProvider() {
		return null;
	}

	@Override
	public boolean isSideDisconnected(Direction dir) {
		return false;
	}

	@Override
	public List<ExitRoute> getDistanceTo(IRouter r) {
		return null;
	}

	@Override
	public void clearInterests() {}

	@Override
	public boolean isCacheInvalid() {
		return false;
	}

	@Override
	public void forceLsaUpdate() {}

	@Override
	public boolean isSubPoweredExit(Direction connection) {
		return false;
	}

	@Override
	public List<Pair<ISubSystemPowerProvider, List<IFilter>>> getSubSystemPowerProvider() {
		return null;
	}

	@Override
	public String toString() {
		return String.format("ServerRouter: {UUID: %s, AT: (%d, %d, %d)}", getId(), _xCoord, _yCoord, _zCoord);
	}

	@Override
	public List<ExitRoute> getRoutersOnSide(Direction exitOrientation) {
		return null;
	}

	@Override
	public int getDistanceToNextPowerPipe(Direction dir) {
		return 0;
	}

	@Override
	public void queueTask(int i, IRouterQueuedTask callable) {}
}
