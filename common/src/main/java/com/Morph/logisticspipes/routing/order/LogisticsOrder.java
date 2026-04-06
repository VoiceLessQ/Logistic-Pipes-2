package com.Morph.logisticspipes.routing.order;

import com.Morph.logisticspipes.interfaces.routing.IProvideItems;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Records that a provider has committed to send items to a destination.
 * Ported from LP 1.12.2 LogisticsOrder — simplified for Phase 4.
 */
public class LogisticsOrder {

    public final ItemIdentifier item;
    public final int amount;
    public final IProvideItems provider;

    public LogisticsOrder(ItemIdentifier item, int amount, IProvideItems provider) {
        this.item = item;
        this.amount = amount;
        this.provider = provider;
    }
}
