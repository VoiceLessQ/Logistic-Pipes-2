package com.Morph.fabric.power;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import team.reborn.energy.api.EnergyStorage;

import com.Morph.logisticspipes.blocks.power.LogisticsPowerJunctionBlockEntity;

/**
 * Team Reborn Energy {@link EnergyStorage} view over a
 * {@link LogisticsPowerJunctionBlockEntity}, exposing the junction as an
 * energy sink to neighbouring Fabric energy machines.
 *
 * Receive-only (mirrors the NeoForge IEnergyStorage adapter):
 *   - {@link #supportsInsertion()} == true; insert delegates to receiveEnergyRf
 *   - {@link #supportsExtraction()} == false; extract returns 0
 *
 * Transaction semantics: this adapter commits the mutation immediately on
 * {@link #insert}. Aborted transactions therefore leave the junction with
 * the already-deposited energy — a small consistency gap that matches what
 * most receive-only energy adapters in the Fabric ecosystem do. The
 * inserter (cable / generator) is the side that retries via the transaction
 * abort path, so the net effect is invisible to it.
 */
public final class FabricPowerJunctionEnergyAdapter implements EnergyStorage {

    private final LogisticsPowerJunctionBlockEntity be;

    public FabricPowerJunctionEnergyAdapter(LogisticsPowerJunctionBlockEntity be) {
        this.be = be;
    }

    @Override
    public boolean supportsInsertion() {
        return true;
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        int amt = (int) Math.min(maxAmount, Integer.MAX_VALUE);
        return be.receiveEnergyRf(amt, false);
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long getAmount() {
        return be.getEnergyStoredRf();
    }

    @Override
    public long getCapacity() {
        return be.getMaxEnergyStoredRf();
    }
}
