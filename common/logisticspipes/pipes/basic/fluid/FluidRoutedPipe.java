package logisticspipes.pipes.basic.fluid;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ITankUtil;
import logisticspipes.interfaces.routing.IRequireReliableFluidTransport;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.PipeFluidUtil;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.routing.order.LogisticsFluidOrderManager;
import logisticspipes.routing.order.LogisticsOrderManager;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.CacheHolder.CacheTypes;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.FluidSinkReply;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;
import network.rs485.logisticspipes.connection.NeighborTileEntity;

public abstract class FluidRoutedPipe extends CoreRoutedPipe {

	private LogisticsFluidOrderManager _orderFluidManager;

	public FluidRoutedPipe(Item item) {
		super(new PipeFluidTransportLogistics(), item);
	}

	@Override
	public void setTile(BlockEntity tile) {
		super.setTile(tile);
	}

	@Override
	public boolean logisitcsIsPipeConnected(BlockEntity tile, Direction dir) {
		ITankUtil tank = PipeFluidUtil.INSTANCE.getTankUtilForTE(tile, dir.getOpposite());
		return (tank != null && tank.containsTanks()) || tile instanceof LogisticsTileGenericPipe;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public TextureType getNonRoutedTexture(Direction connection) {
		if (isFluidSidedTexture(connection)) {
			return Textures.LOGISTICSPIPE_LIQUID_TEXTURE;
		}
		return super.getNonRoutedTexture(connection);
	}

	private boolean isFluidSidedTexture(Direction connection) {
		return getAvailableAdjacent().fluidTanks().stream()
				.filter(neighbor -> neighbor.getDirection() == connection)
				.findFirst()
				.map(neighbor -> {
					final ITankUtil tankUtil = LPNeighborTileEntityKt.getTankUtil(neighbor);
					return tankUtil != null && tankUtil.containsTanks();
				})
				.orElse(false);
	}

	@Override
	public @Nullable LogisticsModule getLogisticsModule() {
		return null;
	}

	/***
	 * @param tile
	 *            The connected BlockEntity
	 * @param dir
	 *            The direction the BlockEntity is in relative to the current
	 *            pipe
	 * @param flag
	 *            Whether to list a Nearby Pipe or not
	 */

	public final boolean isConnectableTank(BlockEntity tile, Direction dir, boolean flag) {
		if (SimpleServiceLocator.specialTankHandler.hasHandlerFor(tile)) {
			return true;
		}
		boolean fluidTile = false;
		if (tile != null && tile.getLevel() != null) {
			IFluidHandler fluidHandler = tile.getCapability(ForgeCapabilities.FLUID_HANDLER, dir).orElse(null);
			if (fluidHandler != null) {
				fluidTile = true;
			}
		}
		if (tile instanceof IFluidHandler) {
			fluidTile = true;
		}
		if (!fluidTile) {
			return false;
		}
		if (!this.canPipeConnect(tile, dir)) {
			return false;
		}
		if (tile instanceof LogisticsTileGenericPipe) {
			if (((LogisticsTileGenericPipe) tile).pipe instanceof FluidRoutedPipe) {
				return false;
			}
			if (!flag) {
				return false;
			}
			if (((LogisticsTileGenericPipe) tile).pipe == null || !(((LogisticsTileGenericPipe) tile).pipe.transport instanceof IFluidHandler)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		if (canInsertFromSideToTanks()) {
			int validDirections = 0;
			final List<Pair<NeighborTileEntity<BlockEntity>, ITankUtil>> list =
					PipeFluidUtil.INSTANCE.getAdjacentTanks(this, true);
			for (Pair<NeighborTileEntity<BlockEntity>, ITankUtil> pair : list) {
				if (pair.getValue2() instanceof LogisticsTileGenericPipe) {
					if (((LogisticsTileGenericPipe) pair.getValue2()).pipe instanceof CoreRoutedPipe) {
						continue;
					}
				}
				FluidTank internalTank = ((PipeFluidTransportLogistics) transport).sideTanks[pair.getValue1().getDirection().ordinal()];
				validDirections++;
				if (internalTank.getFluid().isEmpty()) {
					continue;
				}
				int filled = pair.getValue2().fill(FluidIdentifierStack.getFromStack(internalTank.getFluid()), true);
				if (filled == 0) {
					continue;
				}
				FluidStack drain = internalTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
				if (drain == null || filled != drain.getAmount()) {
					if (LogisticsPipes.isDEBUG()) {
						throw new UnsupportedOperationException("Fluid Multiplication");
					}
				}
			}
			if (validDirections == 0) {
				return;
			}
			FluidTank tank = ((PipeFluidTransportLogistics) transport).internalTank;
			FluidStack stack = tank.getFluid();
			if (stack.isEmpty()) {
				return;
			}
			for (Pair<NeighborTileEntity<BlockEntity>, ITankUtil> pair : list) {
				if (pair.getValue1().isLogisticsPipe()) {
					if (((LogisticsTileGenericPipe) pair.getValue1().getTileEntity()).pipe instanceof CoreRoutedPipe) {
						continue;
					}
				}
				FluidTank tankSide = ((PipeFluidTransportLogistics) transport).sideTanks[pair.getValue1().getDirection().ordinal()];
				stack = tank.getFluid();
				if (stack.isEmpty()) {
					continue;
				}
				stack = stack.copy();
				int filled = tankSide.fill(stack, IFluidHandler.FluidAction.EXECUTE);
				if (filled == 0) {
					continue;
				}
				FluidStack drain = tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
				if (drain == null || filled != drain.getAmount()) {
					if (LogisticsPipes.isDEBUG()) {
						throw new UnsupportedOperationException("Fluid Multiplication");
					}
				}
			}
		}
	}

	public int countOnRoute(FluidIdentifier ident) {
		int amount = 0;
		for (ItemRoutingInformation next : _inTransitToMe) {
			ItemIdentifierStack item = next.getItem();
			if (item.getItem().isFluidContainer()) {
				FluidIdentifierStack liquid = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(item);
				if (liquid.getFluid().equals(ident)) {
					amount += liquid.getAmount();
				}
			}
		}
		return amount;
	}

	public abstract boolean canInsertFromSideToTanks();

	public abstract boolean canInsertToTanks();

	public abstract boolean canReceiveFluid();

	public boolean endReached(LPTravelingItemServer arrivingItem, BlockEntity tile) {
		if (canInsertToTanks() && MainProxy.isServer(getWorld())) {
			getCacheHolder().trigger(CacheTypes.Inventory);
			if (arrivingItem.getItemIdentifierStack() == null || !(arrivingItem.getItemIdentifierStack().getItem().isFluidContainer())) {
				return false;
			}
			if (getRouter().getSimpleID() != arrivingItem.getDestination()) {
				return false;
			}
			int filled;
			FluidIdentifierStack liquid = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(arrivingItem.getItemIdentifierStack());
			if (isConnectableTank(tile, arrivingItem.output, false)) {
				//Try to put liquid into all adjacent tanks.
				for (Pair<NeighborTileEntity<BlockEntity>, ITankUtil> util : PipeFluidUtil.INSTANCE.getAdjacentTanks(this, false)) {
					filled = util.getValue2().fill(liquid, true);
					liquid.lowerAmount(filled);
					if (liquid.getAmount() != 0) {
						continue;
					}
					return true;
				}
				//Try inserting the liquid into the pipe side tank
				filled = ((PipeFluidTransportLogistics) transport).sideTanks[arrivingItem.output.ordinal()].fill(liquid.makeFluidStack(), IFluidHandler.FluidAction.EXECUTE);
				if (filled == liquid.getAmount()) {
					return true;
				}
				liquid.lowerAmount(filled);
			}
			//Try inserting the liquid into the pipe internal tank
			filled = ((PipeFluidTransportLogistics) transport).internalTank.fill(liquid.makeFluidStack(), IFluidHandler.FluidAction.EXECUTE);
			if (filled == liquid.getAmount()) {
				return true;
			}
			//If liquids still exist,
			liquid.lowerAmount(filled);

			if (this instanceof IRequireReliableFluidTransport) {
				((IRequireReliableFluidTransport) this).liquidNotInserted(liquid.getFluid(), liquid.getAmount());
			}

			IRoutedItem routedItem = SimpleServiceLocator.routedItemHelper.createNewTravelItem(SimpleServiceLocator.logisticsFluidManager.getFluidContainer(liquid));
			// Carry forward the arriving item's jam list so the rerouted remainder does
			// not immediately pick the same (now-full) path again, and add this pipe's
			// router to prevent looping back here on the very next hop.
			for (int simpleId : arrivingItem.getJamList()) {
				logisticspipes.routing.IRouter r = SimpleServiceLocator.routerManager.getRouter(simpleId);
				if (r != null) {
					routedItem.addToJamList(r);
				}
			}
			routedItem.addToJamList(getRouter());
			Pair<Integer, FluidSinkReply> replies = SimpleServiceLocator.logisticsFluidManager.getBestReply(liquid, getRouter(), routedItem.getJamList());
			if (replies == null) {
				// clear destination without marking item as lost
				routedItem.setDestination(0);
			} else {
				int dest = replies.getValue1();
				routedItem.setDestination(dest);
			}
			routedItem.setTransportMode(TransportMode.Passive);
			this.queueRoutedItem(routedItem, arrivingItem.output.getOpposite());
			return true;
		}
		return false;
	}

	@Override
	public boolean isFluidPipe() {
		return true;
	}

	@Override
	public boolean isOnSameContainer(CoreRoutedPipe other) {
		if (!(other instanceof FluidRoutedPipe)) {
			return false;
		}
		List<BlockEntity> theirs = PipeFluidUtil.INSTANCE.getAllTankTiles((FluidRoutedPipe) other);
		for (BlockEntity tile : PipeFluidUtil.INSTANCE.getAllTankTiles(this)) {
			if (theirs.contains(tile)) {
				return true;
			}
		}
		return false;
	}

	public LogisticsFluidOrderManager getFluidOrderManager() {
		_orderFluidManager = _orderFluidManager != null ? _orderFluidManager : new LogisticsFluidOrderManager(this);
		return _orderFluidManager;
	}

	@Override
	public LogisticsOrderManager<?, ?> getOrderManager() {
		return getFluidOrderManager();
	}
}
