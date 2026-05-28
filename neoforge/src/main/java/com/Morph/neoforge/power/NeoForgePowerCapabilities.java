package com.Morph.neoforge.power;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.Morph.ExampleMod;
import com.Morph.logisticspipes.LPBlocks;

/**
 * Wires {@link Capabilities.EnergyStorage#BLOCK} for the Power Junction block entity,
 * so adjacent NeoForge energy-aware machines (cables, generators) can push RF in.
 *
 * Output (LP network → external) is not exposed: storage in the junction is
 * intended to feed routing operations via the LP power network, not bleed back
 * into a neighbouring grid.
 */
@EventBusSubscriber(modid = ExampleMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgePowerCapabilities {

    private NeoForgePowerCapabilities() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                LPBlocks.POWER_JUNCTION_BLOCK_ENTITY.get(),
                (be, side) -> new PowerJunctionEnergyAdapter(be)
        );
    }
}
