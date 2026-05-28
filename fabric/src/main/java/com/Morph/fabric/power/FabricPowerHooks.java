package com.Morph.fabric.power;

import team.reborn.energy.api.EnergyStorage;

import com.Morph.logisticspipes.LPBlocks;

/**
 * Wires Team Reborn Energy lookup for the Power Junction block entity, so
 * Fabric machines (cables, generators, etc. that use {@code team.reborn.energy.api})
 * can push RF into a Junction the same way NeoForge cables do via {@code IEnergyStorage}.
 *
 * Called once from {@code ExampleModFabric.onInitialize}.
 */
public final class FabricPowerHooks {

    private FabricPowerHooks() {}

    public static void register() {
        EnergyStorage.SIDED.registerForBlockEntity(
                (be, side) -> new FabricPowerJunctionEnergyAdapter(be),
                LPBlocks.POWER_JUNCTION_BLOCK_ENTITY.get());
    }
}
