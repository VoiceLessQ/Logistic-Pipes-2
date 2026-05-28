package logisticspipes.proxy.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import logisticspipes.proxy.cofh.subproxies.ICoFHEnergyReceiver;
import logisticspipes.proxy.cofh.subproxies.ICoFHEnergyStorage;

public interface IPowerProxy {

	boolean isEnergyReceiver(BlockEntity tile, Direction face);

	ICoFHEnergyReceiver getEnergyReceiver(BlockEntity tile, Direction face);

	ICoFHEnergyStorage getEnergyStorage(int i);

	boolean isAvailable();
}
