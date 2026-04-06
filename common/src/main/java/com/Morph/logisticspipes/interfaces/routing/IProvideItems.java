package com.Morph.logisticspipes.interfaces.routing;

import java.util.Map;

import com.Morph.logisticspipes.routing.LogisticsPromise;
import com.Morph.logisticspipes.routing.order.LogisticsOrder;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Implemented by pipes that can provide items to the network.
 * Ported from LP 1.12.2 IProvideItems.
 */
public interface IProvideItems extends IProvide {

    /** Populate list with all items this provider currently has available. */
    void getAllItems(Map<ItemIdentifier, Integer> list);

    /**
     * Actually extract items from the adjacent inventory and send them
     * toward the destination described by the promise.
     */
    LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination,
                            IAdditionalTargetInformation info);
}
