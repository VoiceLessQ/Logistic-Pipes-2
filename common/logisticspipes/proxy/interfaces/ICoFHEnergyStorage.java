package logisticspipes.proxy.interfaces;

import net.minecraft.nbt.CompoundTag;

/**
 * Forge-energy storage abstraction. Historically wrapped CoFH's EnergyStorage; on 1.20.1 it is
 * implemented by {@link logisticspipes.proxy.PowerProxy} on top of net.neoforged.neoforge.energy.EnergyStorage.
 */
public interface ICoFHEnergyStorage {

	int extractEnergy(int space, boolean b);

	int receiveEnergy(int maxReceive, boolean simulate);

	int getEnergyStored();

	int getMaxEnergyStored();

	void readFromNBT(CompoundTag nbt);

	void writeToNBT(CompoundTag nbt);
}
