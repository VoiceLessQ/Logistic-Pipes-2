package logisticspipes.proxy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import logisticspipes.proxy.interfaces.ICoFHEnergyReceiver;
import logisticspipes.proxy.interfaces.ICoFHEnergyStorage;
import logisticspipes.proxy.interfaces.IPowerProxy;

public class PowerProxy implements IPowerProxy {

	private static class MEnergyStorage extends EnergyStorage {

		public MEnergyStorage(int capacity) {
			super(capacity);
		}

		public void readFromNBT(CompoundTag nbt) {
			this.energy = nbt.getInt("Energy");

			if (energy > capacity) {
				energy = capacity;
			}
		}

		public CompoundTag writeToNBT(CompoundTag nbt) {
			if (energy < 0) {
				energy = 0;
			}
			nbt.putInt("Energy", energy);
			return nbt;
		}
	}

	@Override
	public boolean isEnergyReceiver(BlockEntity tile, Direction face) {
		if (tile != null && tile.getLevel() != null) {
			IEnergyStorage storage = tile.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, tile.getBlockPos(), face);
			if (storage != null) return storage.canReceive();
		}
		return tile instanceof IEnergyStorage;
	}

	@Override
	public ICoFHEnergyReceiver getEnergyReceiver(BlockEntity tile, Direction face) {
		IEnergyStorage bHandler = null;
		if (tile != null && tile.getLevel() != null) {
			bHandler = tile.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, tile.getBlockPos(), face);
		}
		if (bHandler == null && tile instanceof IEnergyStorage) {
			bHandler = (IEnergyStorage) tile;
		}
		final IEnergyStorage handler = bHandler;
		return new ICoFHEnergyReceiver() {

			@Override
			public int getMaxEnergyStored() {
				return handler.getMaxEnergyStored();
			}

			@Override
			public int getEnergyStored() {
				return handler.getEnergyStored();
			}

			@Override
			public int receiveEnergy(Direction opposite, int amount, boolean simulate) {
				return handler.receiveEnergy(amount, simulate);
			}
		};
	}

	@Override
	public ICoFHEnergyStorage getEnergyStorage(int i) {
		final MEnergyStorage energy = new MEnergyStorage(i);
		return new ICoFHEnergyStorage() {

			@Override
			public int extractEnergy(int space, boolean b) {
				return energy.extractEnergy(space, b);
			}

			@Override
			public int receiveEnergy(int maxReceive, boolean simulate) {
				return energy.receiveEnergy(maxReceive, simulate);
			}

			@Override
			public int getEnergyStored() {
				return energy.getEnergyStored();
			}

			@Override
			public int getMaxEnergyStored() {
				return energy.getMaxEnergyStored();
			}

			@Override
			public void readFromNBT(CompoundTag nbt) {
				energy.readFromNBT(nbt);
			}

			@Override
			public void writeToNBT(CompoundTag nbt) {
				energy.writeToNBT(nbt);
			}

		};
	}

	@Override
	public boolean isAvailable() {
		return true;
	}
}
