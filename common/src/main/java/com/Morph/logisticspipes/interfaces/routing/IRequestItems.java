package com.Morph.logisticspipes.interfaces.routing;

import com.Morph.logisticspipes.routing.IRouter;
import com.Morph.logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Implemented by pipes that can request items from the network.
 * Ported from LP 1.12.2 IRequestItems.
 */
public interface IRequestItems {

    IRouter getRouter();

    /** Called when requested items could not be sourced. */
    void itemCouldNotBeSent(ItemIdentifierStack item, IAdditionalTargetInformation info);
}
