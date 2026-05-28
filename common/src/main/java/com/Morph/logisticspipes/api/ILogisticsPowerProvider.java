package com.Morph.logisticspipes.api;

/**
 * Things which directly provide power to the logistics network implement this.
 * Lists of these objects available to a network are cached; the closest provider
 * with available power is preferentially drawn from.
 *
 * @author Andrew (LP1) — ported for LP2
 */
public interface ILogisticsPowerProvider extends IRoutedPowerProvider {

    /** Current stored power level. */
    int getPowerLevel();
}
