package logisticspipes.blocks.powertile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;


import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.cofh.subproxies.ICoFHEnergyStorage;

public class LogisticsRFPowerProviderTileEntity extends LogisticsPowerProviderTileEntity {

	public static final int MAX_STORAGE = 10000000;
	public static final int MAX_MAXMODE = 8;
	public static final int MAX_PROVIDE_PER_TICK = 10000; //TODO

	private IEnergyStorage energyInterface = new IEnergyStorage() {

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			return storage.receiveEnergy(maxReceive, simulate);
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			return storage.getEnergyStored();
		}

		@Override
		public int getMaxEnergyStored() {
			return storage.getMaxEnergyStored();
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	};

	private ICoFHEnergyStorage storage;

	public LogisticsRFPowerProviderTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(logisticspipes.LPRegistries.BE_POWER_PROVIDER_RF.get(), pos, state);
		storage = SimpleServiceLocator.powerProxy.getEnergyStorage(10000);
	}

	public void addEnergy(double amount) {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		internalStorage += amount;
		if (internalStorage > LogisticsRFPowerProviderTileEntity.MAX_STORAGE) {
			internalStorage = LogisticsRFPowerProviderTileEntity.MAX_STORAGE;
		}
		if (internalStorage >= getMaxStorage()) {
			needMorePowerTriggerCheck = false;
		}
	}

	private void addStoredRF() {
		int space = freeSpace();
		int available = (storage.extractEnergy(space, true));
		if (available > 0) {
			if (storage.extractEnergy(available, false) == available) {
				addEnergy(available);
			}
		}
	}

	private void pullFromAdjacentStorage() {
		net.minecraft.world.level.Level world = getWorld();
		if (world == null) return;
		int remaining = freeSpace();
		for (Direction dir : Direction.values()) {
			remaining = pullFromNeighbor(world, dir, remaining);
			if (remaining <= 0) return;
		}
	}

	private int pullFromNeighbor(net.minecraft.world.level.Level world, Direction dir, int remaining) {
		net.minecraft.world.level.block.entity.BlockEntity neighbor = world.getBlockEntity(getBlockPos().relative(dir));
		if (neighbor == null) return remaining;
		LazyOptional<IEnergyStorage> cap = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
		if (!cap.isPresent()) return remaining;
		IEnergyStorage neighborStorage = cap.orElseThrow(IllegalStateException::new);
		if (!neighborStorage.canExtract()) return remaining;
		int extracted = neighborStorage.extractEnergy(remaining, false);
		if (extracted > 0) {
			addEnergy(extracted);
			return remaining - extracted;
		}
		return remaining;
	}

	public int freeSpace() {
		return (int) (getMaxStorage() - internalStorage);
	}

	@Override
	public void update() {
		super.update();
		if (MainProxy.isServer(getWorld())) {
			if (freeSpace() > 0) {
				if (logisticspipes.config.Configs.getPowerSourceMode() == logisticspipes.config.Configs.PowerSourceMode.ADJACENT) {
					pullFromAdjacentStorage();
				} else {
					addStoredRF();
				}
			}
		}
	}

	@Override
	public int getMaxStorage() {
		maxMode = Math.min(LogisticsRFPowerProviderTileEntity.MAX_MAXMODE, Math.max(1, maxMode));
		return (LogisticsRFPowerProviderTileEntity.MAX_STORAGE / maxMode);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		storage.readFromNBT(nbt);
	}

	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		storage.writeToNBT(nbt);
	}

	@Override
	public String getBrand() {
		return "RF";
	}

	@Override
	protected double getMaxProvidePerTick() {
		return LogisticsRFPowerProviderTileEntity.MAX_PROVIDE_PER_TICK;
	}

	@Override
	protected void handlePower(CoreRoutedPipe pipe, double toSend) {
		pipe.handleRFPowerArival(toSend);
	}

	@Override
	protected int getLaserColor() {
		return LogisticsPowerProviderTileEntity.RF_COLOR;
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY) {
			return ForgeCapabilities.ENERGY.orEmpty(cap, LazyOptional.of(() -> energyInterface));
		}
		return super.getCapability(cap, side);
	}

	public IEnergyStorage getEnergyInterface() {
		return energyInterface;
	}
}
