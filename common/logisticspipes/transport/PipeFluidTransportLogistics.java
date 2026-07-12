package logisticspipes.transport;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.pipe.PipeFluidUpdate;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.SafeTimeTracker;
import logisticspipes.utils.item.ItemIdentifierStack;

public class PipeFluidTransportLogistics extends PipeTransportLogistics {

	public FluidTank[] sideTanks = new FluidTank[Direction.values().length];
	public FluidTank internalTank = new FluidTank(getInnerCapacity());

	public FluidStack[] renderCache = new FluidStack[7];

	public PipeFluidTransportLogistics() {
		super(true);
		for (Direction dir : Direction.values()) {
			sideTanks[dir.ordinal()] = new FluidTank(getSideCapacity());
		}
	}

	public IFluidHandler getIFluidHandler(Direction face) {
		return new FluidHandler(face);
	}

	private FluidRoutedPipe getFluidPipe() {
		return (FluidRoutedPipe) getPipe();
	}

	/** Returns the current fluid in the side tank for the given direction, or the internal tank if null. */
	public FluidStack getFluidInSideTank(Direction from) {
		if (from == null) return internalTank.getFluid();
		return sideTanks[from.ordinal()].getFluid();
	}

	public int fill(Direction from, FluidStack resource, boolean doFill) {
		if (from.ordinal() < Direction.values().length && getFluidPipe().canReceiveFluid()) {
			return sideTanks[from.ordinal()].fill(resource,
					doFill ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
		} else {
			return 0;
		}
	}

	public FluidStack drain(Direction from, int maxDrain, boolean doDrain) {
		if (from.ordinal() < Direction.values().length) {
			return sideTanks[from.ordinal()].drain(maxDrain,
					doDrain ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
		} else {
			return null;
		}
	}

	public FluidStack drain(Direction from, FluidStack resource, boolean doDrain) {
		if (sideTanks[from.ordinal()].getFluid() == null || !(sideTanks[from.ordinal()].getFluid().isFluidEqual(resource))) {
			return new FluidStack(resource.getFluid(), 0);
		}
		return drain(from, resource.getAmount(), doDrain);
	}

	public class FluidHandler implements IFluidHandler {

		private final Direction from;

		FluidHandler(Direction from) {
			this.from = from;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			if (from.ordinal() < Direction.values().length) {
				return sideTanks[from.ordinal()].getFluid();
			}
			return FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			if (from.ordinal() < Direction.values().length) {
				return sideTanks[from.ordinal()].getCapacity();
			}
			return 0;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return true;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (from.ordinal() < Direction.values().length && getFluidPipe().canReceiveFluid()) {
				return sideTanks[from.ordinal()].fill(resource, action);
			} else {
				return 0;
			}
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			if (from.ordinal() < Direction.values().length) {
				return sideTanks[from.ordinal()].drain(maxDrain, action);
			} else {
				return FluidStack.EMPTY;
			}
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (sideTanks[from.ordinal()].getFluid() == null || !(sideTanks[from.ordinal()].getFluid().isFluidEqual(resource))) {
				return new FluidStack(resource.getFluid(), 0);
			}
			return drain(resource.getAmount(), action);
		}
	}

	@Override
	public void readFromNBT(CompoundTag nbttagcompound) {
		super.readFromNBT(nbttagcompound);

		for (Direction direction : Direction.values()) {
			if (nbttagcompound.contains("tank[" + direction.ordinal() + "]")) {
				sideTanks[direction.ordinal()].readFromNBT(logisticspipes.utils.RegistryAccessUtil.registries(), nbttagcompound.getCompound("tank[" + direction.ordinal() + "]"));
			}
		}
		if (nbttagcompound.contains("tank[middle]")) {
			internalTank.readFromNBT(logisticspipes.utils.RegistryAccessUtil.registries(), nbttagcompound.getCompound("tank[middle]"));
		}
	}

	@Override
	public void writeToNBT(CompoundTag nbttagcompound) {
		super.writeToNBT(nbttagcompound);

		for (Direction direction : Direction.values()) {
			CompoundTag subTag = new CompoundTag();
			sideTanks[direction.ordinal()].writeToNBT(logisticspipes.utils.RegistryAccessUtil.registries(), subTag);
			nbttagcompound.put("tank[" + direction.ordinal() + "]", subTag);
		}
		CompoundTag subTag = new CompoundTag();
		internalTank.writeToNBT(logisticspipes.utils.RegistryAccessUtil.registries(), subTag);
		nbttagcompound.put("tank[middle]", subTag);
	}

	public int getInnerCapacity() {
		return 10000;
	}

	public int getSideCapacity() {
		return 5000;
	}

	@Override
	public void onNeighborBlockChange() {
		super.onNeighborBlockChange();

		for (Direction direction : Direction.values()) {
			if (!MainProxy.checkPipesConnections(container, container.getTile(PipeFluidTransportLogistics.orientations[direction.ordinal()]), PipeFluidTransportLogistics.orientations[direction.ordinal()])) {
				if (MainProxy.isServer(getWorld())) {
					FluidStack stack = sideTanks[direction.ordinal()].getFluid();
					if (stack != null && !stack.isEmpty()) {
						sideTanks[direction.ordinal()].setFluid(FluidStack.EMPTY);
						internalTank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
					}
				}
				if (renderCache[direction.ordinal()] != null) {
					renderCache[direction.ordinal()].setAmount(1);
				}
			}
		}
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		updateFluid();
	}

	/*
	 * BuildCraft Fluid Sync Code
	 */
	private final SafeTimeTracker tracker = new SafeTimeTracker(10);
	private long clientSyncCounter = 30;
	public byte initClient = 0;

	private static final Direction[] orientations = Direction.values();

	private void updateFluid() {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		if (tracker.markTimeIfDelay(getWorld())) {

			boolean init = false;
			if (++clientSyncCounter > 40) {
				clientSyncCounter = 0;
				init = true;
			}
			if (clientSyncCounter < 0) {
				clientSyncCounter = 0;
			}
			ModernPacket packet = computeFluidUpdate(init, true);
			if (packet != null) {
				MainProxy.sendPacketToAllWatchingChunk(container, packet);
			}
		}
	}

	/**
	 * Computes the PacketFluidUpdate packet for transmission to a client
	 *
	 * @param initPacket    everything is sent, no delta stuff ( first packet )
	 * @param persistChange The render cache change is persisted
	 * @return PacketFluidUpdate liquid update packet
	 */
	private ModernPacket computeFluidUpdate(boolean initPacket, boolean persistChange) {

		boolean changed = false;

		if (initClient > 0) {
			initClient--;
			if (initClient == 1) {
				changed = true;
			}
		}

		FluidStack[] renderCache = this.renderCache.clone();

		for (Direction dir : PipeFluidTransportLogistics.orientations) {
			FluidStack current;
			if (dir != null) {
				current = sideTanks[dir.ordinal()].getFluid();
			} else {
				current = internalTank.getFluid();
			}
			FluidStack prev = renderCache[dir.ordinal()];

			if (prev == null && (current == null || current.isEmpty())) {
				continue;
			} else if (prev == null) {
				changed = true;
				renderCache[dir.ordinal()] = current.copy();
				continue;
			} else if (current == null || current.isEmpty()) {
				changed = true;
				renderCache[dir.ordinal()] = null;
				continue;
			}

			if (prev.getFluid() != current.getFluid() || initPacket) {
				changed = true;
				renderCache[dir.ordinal()] = new FluidStack(current.getFluid(), renderCache[dir.ordinal()].getAmount());
			}

			if (prev.getAmount() != current.getAmount() || initPacket) {
				changed = true;
				renderCache[dir.ordinal()].setAmount(current.getAmount());
			}
		}

		if (persistChange) {
			this.renderCache = renderCache;
		}

		if (changed || initPacket) {
			return PacketHandler.getPacket(PipeFluidUpdate.class).setRenderCache(renderCache).setTilePos(container).setChunkDataPacket(initPacket);
		}

		return null;
	}

	@Override
	protected boolean isItemUnwanted(ItemIdentifierStack stack) {
		return false;
	}

	@Override
	protected boolean isPipeCheck(BlockEntity tile) {
		return SimpleServiceLocator.pipeInformationManager.isPipe(tile);
	}
}
