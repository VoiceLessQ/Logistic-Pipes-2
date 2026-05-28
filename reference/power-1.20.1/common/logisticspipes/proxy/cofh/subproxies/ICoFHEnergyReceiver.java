package logisticspipes.proxy.cofh.subproxies;

import net.minecraft.core.Direction;

public interface ICoFHEnergyReceiver {

	int getMaxEnergyStored();

	int getEnergyStored();

	int receiveEnergy(Direction opposite, int i, boolean b);

}
