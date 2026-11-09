package logisticspipes.blocks.powertile;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import logisticspipes.proxy.interfaces.IEnergyStorage;
import logisticspipes.utils.TagUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;



import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import team.reborn.energy.api.EnergyStorage;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.TREnergyStorage;
import logisticspipes.proxy.interfaces.ICoFHEnergyStorage;

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

	private final TREnergyStorage trEnergy = new TREnergyStorage(energyInterface);

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
		IEnergyStorage neighborStorage = IEnergyStorage.SIDED.find(world, neighbor.getBlockPos(), neighbor.getBlockState(), neighbor, dir.getOpposite());
		int extracted;
		if (neighborStorage != null) {
			if (!neighborStorage.canExtract()) return remaining;
			extracted = neighborStorage.extractEnergy(remaining, false);
		} else {
			EnergyStorage trStorage = EnergyStorage.SIDED.find(world, neighbor.getBlockPos(), neighbor.getBlockState(), neighbor, dir.getOpposite());
			if (trStorage == null || !trStorage.supportsExtraction()) return remaining;
			try (Transaction transaction = Transaction.openOuter()) {
				extracted = (int) trStorage.extract(remaining, transaction);
				transaction.commit();
			}
		}
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
				addStoredRF();
			}
			if (freeSpace() > 0 && logisticspipes.config.Configs.getPowerSourceMode() == logisticspipes.config.Configs.PowerSourceMode.ADJACENT) {
				pullFromAdjacentStorage();
			}
		}
	}

	@Override
	public int getMaxStorage() {
		maxMode = Math.min(LogisticsRFPowerProviderTileEntity.MAX_MAXMODE, Math.max(1, maxMode));
		return (LogisticsRFPowerProviderTileEntity.MAX_STORAGE / maxMode);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag nbt = TagUtil.read(input);
		super.loadAdditional(input);
		storage.readFromNBT(nbt);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag nbt = new CompoundTag();
		super.saveAdditional(output);
		storage.writeToNBT(nbt);
		TagUtil.write(output, nbt);
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

	public IEnergyStorage getEnergyInterface() {
		return energyInterface;
	}

	public TREnergyStorage getTREnergy() {
		return trEnergy;
	}
}
