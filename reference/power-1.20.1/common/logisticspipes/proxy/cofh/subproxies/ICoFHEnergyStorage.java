package logisticspipes.proxy.cofh.subproxies;

import net.minecraft.nbt.CompoundTag;

public interface ICoFHEnergyStorage {

	int extractEnergy(int space, boolean b);

	int receiveEnergy(int maxReceive, boolean simulate);

	int getEnergyStored();

	int getMaxEnergyStored();

	void readFromNBT(CompoundTag nbt);

	void writeToNBT(CompoundTag nbt);
}
