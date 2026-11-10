package logisticspipes.proxy.cc;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.cc.wrapper.CCCommandWrapper;
import logisticspipes.proxy.cc.wrapper.LPPeripheralTilePipeWrapper;
import logisticspipes.proxy.cc.wrapper.LPPeripheralTileSolidWrapper;
import logisticspipes.proxy.computers.CCConstants;
import logisticspipes.proxy.computers.wrapper.CCObjectWrapper;
import logisticspipes.proxy.interfaces.ICCProxy;

public class CCProxy implements ICCProxy {

	public CCProxy() {
		BlockEntityType<?>[] lpTypes = BuiltInRegistries.BLOCK_ENTITY_TYPE.stream()
				.filter(type -> BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).getNamespace().equals(LPConstants.LP_MOD_ID))
				.toArray(BlockEntityType<?>[]::new);
		PeripheralLookup.get().registerForBlockEntities(CCProxy::createPeripheral, lpTypes);
	}

	@Nullable
	public static IPeripheral createPeripheral(@Nullable BlockEntity tile, Direction side) {
		if (tile instanceof LogisticsTileGenericPipe pipeTile) {
			if (pipeTile.pipe == null || pipeTile.connections == null) {
				return null;
			}
			Object wrapped = CCObjectWrapper.checkForAnnotations(pipeTile.pipe, CCCommandWrapper.WRAPPER);
			return wrapped instanceof CCCommandWrapper ? new LPPeripheralTilePipeWrapper(pipeTile, (CCCommandWrapper) wrapped, side) : null;
		}
		if (tile instanceof LogisticsSolidTileEntity solidTile) {
			Object wrapped = CCObjectWrapper.checkForAnnotations(solidTile, CCCommandWrapper.WRAPPER);
			return wrapped instanceof CCCommandWrapper ? new LPPeripheralTileSolidWrapper(solidTile, (CCCommandWrapper) wrapped) : null;
		}
		return null;
	}

	@Override
	public boolean isTurtle(BlockEntity tile) {
		if (tile == null) {
			return false;
		}
		Identifier type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(tile.getType());
		return type != null && type.getNamespace().equals(LPConstants.computerCraftModID) && type.getPath().startsWith("turtle");
	}

	@Override
	public boolean isCC() {
		return true;
	}

	@Override
	public boolean isLuaThread(Thread thread) {
		return thread.getName().contains("Computer-Worker");
	}

	@Override
	public void queueEvent(String event, Object[] arguments, LogisticsTileGenericPipe tile) {
		Object[] luaArguments = CCCommandWrapper.toLuaArguments(arguments);
		for (Object computer : tile.connections.keySet()) {
			((IComputerAccess) computer).queueEvent(event, luaArguments);
		}
	}

	@Override
	public void setTurtleConnect(boolean flag, LogisticsTileGenericPipe tile) {
		Direction side = tile.connections.get(tile.currentPC);
		if (side != null) {
			tile.turtleConnect[side.ordinal()] = flag;
			tile.scheduleNeighborChange();
		}
	}

	@Override
	public boolean getTurtleConnect(LogisticsTileGenericPipe tile) {
		Direction side = tile.connections.get(tile.currentPC);
		return side != null && tile.turtleConnect[side.ordinal()];
	}

	@Override
	public int getLastCCID(LogisticsTileGenericPipe tile) {
		if (tile.currentPC == null) {
			return -1;
		}
		return ((IComputerAccess) tile.currentPC).getID();
	}

	@Override
	public void handleMesssage(int computerId, Object message, LogisticsTileGenericPipe tile, int sourceId) {
		Object[] luaArguments = CCCommandWrapper.toLuaArguments(new Object[] { sourceId, message });
		for (Map.Entry<Object, Direction> entry : tile.connections.entrySet()) {
			IComputerAccess computer = (IComputerAccess) entry.getKey();
			if (computer.getID() == computerId) {
				computer.queueEvent(CCConstants.LP_CC_MESSAGE_EVENT, luaArguments);
			}
		}
	}
}
