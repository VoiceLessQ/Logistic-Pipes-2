package logisticspipes.proxy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import team.reborn.energy.api.EnergyStorage;


import logisticspipes.proxy.interfaces.ICoFHEnergyReceiver;
import logisticspipes.proxy.interfaces.ICoFHEnergyStorage;
import logisticspipes.proxy.interfaces.IEnergyStorage;
import logisticspipes.proxy.interfaces.IPowerProxy;

public class PowerProxy implements IPowerProxy {

	private static class MEnergyStorage {

		private final int capacity;
		private int energy;

		public MEnergyStorage(int capacity) {
			this.capacity = capacity;
		}

		public int receiveEnergy(int maxReceive, boolean simulate) {
			int received = Math.min(capacity - energy, Math.max(0, maxReceive));
			if (!simulate) energy += received;
			return received;
		}

		public int extractEnergy(int maxExtract, boolean simulate) {
			int extracted = Math.min(energy, Math.max(0, maxExtract));
			if (!simulate) energy -= extracted;
			return extracted;
		}

		public int getEnergyStored() {
			return energy;
		}

		public int getMaxEnergyStored() {
			return capacity;
		}

		public void readFromNBT(CompoundTag nbt) {
			this.energy = nbt.getIntOr("Energy", 0);

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
			IEnergyStorage storage = IEnergyStorage.SIDED.find(tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile, face);
			if (storage != null) return storage.canReceive();
			EnergyStorage trStorage = EnergyStorage.SIDED.find(tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile, face);
			if (trStorage != null) return trStorage.supportsInsertion();
		}
		return tile instanceof IEnergyStorage;
	}

	@Override
	public ICoFHEnergyReceiver getEnergyReceiver(BlockEntity tile, Direction face) {
		IEnergyStorage bHandler = null;
		if (tile != null && tile.getLevel() != null) {
			bHandler = IEnergyStorage.SIDED.find(tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile, face);
		}
		if (bHandler == null && tile instanceof IEnergyStorage) {
			bHandler = (IEnergyStorage) tile;
		}
		if (bHandler == null && tile != null && tile.getLevel() != null) {
			EnergyStorage trStorage = EnergyStorage.SIDED.find(tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile, face);
			if (trStorage != null) return trReceiver(trStorage);
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

	private static ICoFHEnergyReceiver trReceiver(EnergyStorage storage) {
		return new ICoFHEnergyReceiver() {

			@Override
			public int getMaxEnergyStored() {
				return (int) Math.min(Integer.MAX_VALUE, storage.getCapacity());
			}

			@Override
			public int getEnergyStored() {
				return (int) Math.min(Integer.MAX_VALUE, storage.getAmount());
			}

			@Override
			public int receiveEnergy(Direction opposite, int amount, boolean simulate) {
				try (Transaction transaction = Transaction.openOuter()) {
					int received = (int) storage.insert(amount, transaction);
					if (!simulate) transaction.commit();
					return received;
				}
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
