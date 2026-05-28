package com.Morph.logisticspipes.api;

import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * Implemented by things capable of providing power that draw from another source.
 * Implement {@link ILogisticsPowerProvider} instead if you want to provide power
 * directly to the LP network. Distance-based energy loss may be involved.
 *
 * @author Andrew (LP1) — ported for LP2
 */
public interface IRoutedPowerProvider {

    /** Typically delegates to {@code useEnergy(amount, null)}. */
    boolean useEnergy(int amount);

    /** Typically delegates to {@code canUseEnergy(amount, null)}. */
    boolean canUseEnergy(int amount);

    /**
     * Recursive-safe variant. Each implementer must:
     *   a) check that it is not already in {@code providersToIgnore}, and
     *   b) add itself (creating the list if {@code null}).
     */
    boolean useEnergy(int amount, List<Object> providersToIgnore);

    boolean canUseEnergy(int amount, List<Object> providersToIgnore);

    /** Block position of the associated tile — used for packet routing. */
    BlockPos getProviderPos();
}
