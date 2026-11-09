package logisticspipes.proxy.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;

/** Forge's IEnergyStorage contract (FE/RF), looked up per side like the old ENERGY capability. */
public interface IEnergyStorage {

	BlockApiLookup<IEnergyStorage, Direction> SIDED = BlockApiLookup.get(
			Identifier.fromNamespaceAndPath("logisticspipes", "energy"), IEnergyStorage.class, Direction.class);

	int receiveEnergy(int maxReceive, boolean simulate);

	int extractEnergy(int maxExtract, boolean simulate);

	int getEnergyStored();

	int getMaxEnergyStored();

	boolean canExtract();

	boolean canReceive();
}
