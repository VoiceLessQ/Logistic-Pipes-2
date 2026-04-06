package com.Morph.logisticspipes.interfaces.routing;

import com.Morph.logisticspipes.routing.IRouter;

/** Base interface for anything that can provide items to the network. Ported from LP 1.12.2. */
public interface IProvide {
    IRouter getRouter();
}
