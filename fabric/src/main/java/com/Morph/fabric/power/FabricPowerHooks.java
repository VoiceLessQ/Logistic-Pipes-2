package com.Morph.fabric.power;

/**
 * Placeholder for the Fabric energy bridge.
 *
 * Status: not wired. The Power Junction works on Fabric for LP-internal use
 * ({@code useEnergy} / {@code canUseEnergy} via the LP network), but no
 * external Fabric machine can push RF into it yet because Team Reborn Energy
 * (the de-facto Fabric energy std) is not declared as a dependency.
 *
 * To wire it later:
 *   1. fabric/build.gradle:  modImplementation "teamreborn:energy:<ver-for-1.21.1>"
 *      (TR-Energy publishes to https://maven.fabricmc.net/ which is already configured).
 *   2. Implement an {@code EnergyStorage} adapter mirroring
 *      {@code com.Morph.neoforge.power.PowerJunctionEnergyAdapter}.
 *   3. Register via {@code EnergyStorage.SIDED.registerForBlockEntity(...)} at
 *      Fabric mod-init time.
 *
 * Until that's done this class is intentionally inert.
 */
public final class FabricPowerHooks {

    private FabricPowerHooks() {}

    /** Called from {@code ExampleModFabric}. No-op until TR-Energy is wired. */
    public static void register() {
        // intentional no-op
    }
}
