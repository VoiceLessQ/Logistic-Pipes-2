package com.Morph.logisticspipes.routing;

import com.Morph.logisticspipes.interfaces.routing.IProvideItems;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * A commitment from a provider to supply a certain number of items.
 * Ported from LP 1.12.2 LogisticsPromise — simplified for Phase 4.
 */
public class LogisticsPromise {

    public final ItemIdentifier item;
    public int numberOfItems;
    public final IProvideItems sender;

    public LogisticsPromise(ItemIdentifier item, int numberOfItems, IProvideItems sender) {
        this.item = item;
        this.numberOfItems = numberOfItems;
        this.sender = sender;
    }
}
