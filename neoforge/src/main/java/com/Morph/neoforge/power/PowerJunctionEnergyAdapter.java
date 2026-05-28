package com.Morph.neoforge.power;

import net.neoforged.neoforge.energy.IEnergyStorage;

import com.Morph.logisticspipes.blocks.power.LogisticsPowerJunctionBlockEntity;

/**
 * NeoForge {@link IEnergyStorage} view over a {@link LogisticsPowerJunctionBlockEntity}.
 *
 * Only receive is supported — energy stored in the junction can leave via the
 * LP network's useEnergy() path, not back through a neighbouring energy cable.
 */
public final class PowerJunctionEnergyAdapter implements IEnergyStorage {

    private final LogisticsPowerJunctionBlockEntity be;

    public PowerJunctionEnergyAdapter(LogisticsPowerJunctionBlockEntity be) {
        this.be = be;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return be.receiveEnergyRf(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return be.getEnergyStoredRf();
    }

    @Override
    public int getMaxEnergyStored() {
        return be.getMaxEnergyStoredRf();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
