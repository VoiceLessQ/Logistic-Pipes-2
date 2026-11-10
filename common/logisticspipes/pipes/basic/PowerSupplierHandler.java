package logisticspipes.pipes.basic;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.ICoFHEnergyReceiver;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.connection.LPNeighborTileEntity;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

public class PowerSupplierHandler {

	private final CoreRoutedPipe pipe;

	private final Channel rf = new Channel("RF", 10000, LogisticsPowerProviderTileEntity.RF_COLOR);
	private final Channel eu = new Channel("EU", 2048 * 4, LogisticsPowerProviderTileEntity.IC2_COLOR);

	private static class Channel {

		final String brand;
		final double bufferMax;
		final int laserColor;
		double buffer;

		Channel(String brand, double bufferMax, int laserColor) {
			this.brand = brand;
			this.bufferMax = bufferMax;
			this.laserColor = laserColor;
		}
	}

	public PowerSupplierHandler(CoreRoutedPipe pipe) {
		this.pipe = pipe;
	}

	public void writeToNBT(CompoundTag nbttagcompound) {
		if (rf.buffer > 0) {
			nbttagcompound.putDouble("bufferRF", rf.buffer);
		}
		if (eu.buffer > 0) {
			nbttagcompound.putDouble("bufferEU", eu.buffer);
		}
	}

	public void readFromNBT(CompoundTag nbttagcompound) {
		if (nbttagcompound.get("bufferRF") instanceof FloatTag) { // support for old float
			rf.buffer = nbttagcompound.getFloatOr("bufferRF", 0f);
		} else {
			rf.buffer = nbttagcompound.getDoubleOr("bufferRF", 0.0);
		}
		if (nbttagcompound.get("bufferEU") instanceof FloatTag) { // support for old float
			eu.buffer = nbttagcompound.getFloatOr("bufferEU", 0f);
		} else {
			eu.buffer = nbttagcompound.getDoubleOr("bufferEU", 0.0);
		}
	}

	public void update() {
		if (SimpleServiceLocator.powerProxy.isAvailable() && pipe.getUpgradeManager().hasRFPowerSupplierUpgrade()) {
			if (requestPower(rf, Integer.MAX_VALUE)) return;
		}
		// EU supplier upgrades push through Team Reborn Energy (Tech Reborn machines), capped per tick at the upgrade's voltage tier
		int euLevel = pipe.getUpgradeManager().getIC2PowerLevel();
		if (euLevel > 0) {
			requestPower(eu, euLevel);
		}
	}

	private boolean requestPower(Channel channel, int maxPerTarget) {
		//Use Buffer

		final List<LPNeighborTileEntity<BlockEntity>> adjacentTileEntities = new WorldCoordinatesWrapper(pipe.container).allNeighborTileEntities();

		double globalNeed = 0;
		double[] need = new double[adjacentTileEntities.size()];
		int i = 0;
		for (NeighborTileEntity<BlockEntity> adjacent : adjacentTileEntities) {
			if (SimpleServiceLocator.powerProxy.isEnergyReceiver(adjacent.getTileEntity(), adjacent.getOurDirection())) {
				if (pipe.canPipeConnect(adjacent.getTileEntity(), adjacent.getDirection())) {
					ICoFHEnergyReceiver energyReceiver = SimpleServiceLocator.powerProxy.getEnergyReceiver(adjacent.getTileEntity(), adjacent.getOurDirection());
					globalNeed += need[i] = Math.min(maxPerTarget, energyReceiver.getMaxEnergyStored() - energyReceiver.getEnergyStored());
				}
			}
			++i;
		}

		if (globalNeed != 0 && !Double.isNaN(globalNeed)) {
			double fullfillable = Math.min(1, channel.buffer / globalNeed);
			i = 0;
			for (NeighborTileEntity<BlockEntity> adjacent : adjacentTileEntities) {
				if (SimpleServiceLocator.powerProxy.isEnergyReceiver(adjacent.getTileEntity(), adjacent.getOurDirection())) {
					if (pipe.canPipeConnect(adjacent.getTileEntity(), adjacent.getDirection())) {
						Direction oppositeDir = adjacent.getOurDirection();
						ICoFHEnergyReceiver energyReceiver = SimpleServiceLocator.powerProxy.getEnergyReceiver(adjacent.getTileEntity(), oppositeDir);
						if (channel.buffer + 1 < need[i] * fullfillable) {
							return true;
						}
						int used = energyReceiver.receiveEnergy(oppositeDir, (int) (need[i] * fullfillable), false);
						if (used > 0) {
							pipe.container.addLaser(adjacent.getDirection(), 0.5F, channel.laserColor, false, true);
							channel.buffer -= used;
						}
						if (channel.buffer < 0) {
							channel.buffer = 0;
							return true;
						}
					}
				}
				++i;
			}
		}
		//Rerequest Buffer
		List<Pair<ISubSystemPowerProvider, List<IFilter>>> provider = pipe.getRouter().getSubSystemPowerProvider();
		double available = 0;
		outer:
		for (Pair<ISubSystemPowerProvider, List<IFilter>> pair : provider) {
			for (IFilter filter : pair.getValue2()) {
				if (filter.blockPower()) {
					continue outer;
				}
			}
			if (pair.getValue1().usePaused()) {
				continue;
			}
			if (!pair.getValue1().getBrand().equals(channel.brand)) {
				continue;
			}
			available += pair.getValue1().getPowerLevel();
		}
		if (available > 0) {
			double neededPower = channel.bufferMax - channel.buffer;
			if (neededPower > 0) {
				if (pipe.useEnergy((int) (neededPower / 100), false)) {
					outer:
					for (Pair<ISubSystemPowerProvider, List<IFilter>> pair : provider) {
						for (IFilter filter : pair.getValue2()) {
							if (filter.blockPower()) {
								continue outer;
							}
						}
						if (pair.getValue1().usePaused()) {
							continue;
						}
						if (!pair.getValue1().getBrand().equals(channel.brand)) {
							continue;
						}
						double requestamount = neededPower * (pair.getValue1().getPowerLevel() / available);
						pair.getValue1().requestPower(pipe.getRouterId(), requestamount);
					}
				}
			}
		}
		return false;
	}

	public void addRFPower(double toSend) {
		rf.buffer += toSend;
	}

	public void addIC2Power(double toSend) {
		eu.buffer += toSend;
	}
}
