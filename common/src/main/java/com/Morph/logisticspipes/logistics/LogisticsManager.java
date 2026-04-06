package com.Morph.logisticspipes.logistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Morph.logisticspipes.interfaces.routing.IProvideItems;
import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.routing.ExitRoute;
import com.Morph.logisticspipes.routing.PipeRoutingConnectionType;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Network-level logistics operations.
 * Ported from LP 1.12.2 LogisticsManager — simplified for Phase 4 (no filters, no fluids).
 */
public final class LogisticsManager {

    private LogisticsManager() {}

    /**
     * Scans all reachable routers and aggregates available items from all providers.
     * Called by the request pipe to show what can be requested.
     *
     * @param reachable sorted list of exit routes from the requester's router
     */
    public static Map<ItemIdentifier, Integer> getAvailableItems(List<ExitRoute> reachable) {
        Map<ItemIdentifier, Integer> available = new HashMap<>();

        for (ExitRoute route : reachable) {
            if (!route.containsFlag(PipeRoutingConnectionType.canRequestFrom)) continue;
            CoreRoutedPipe pipe = route.destination.getCachedPipe();
            if (pipe == null) pipe = route.destination.getPipe();
            if (!(pipe instanceof IProvideItems provider)) continue;
            provider.getAllItems(available);
        }

        return available;
    }

    /**
     * Finds the best provider for the requested item and returns a promise,
     * or null if nothing is available.
     *
     * @param reachable sorted list of exit routes from the requester's router
     * @param item      what to request
     * @param amount    how many
     */
    public static com.Morph.logisticspipes.routing.LogisticsPromise getBestProvider(
            List<ExitRoute> reachable, ItemIdentifier item, int amount) {

        for (ExitRoute route : reachable) {
            if (!route.containsFlag(PipeRoutingConnectionType.canRequestFrom)) continue;
            CoreRoutedPipe pipe = route.destination.getCachedPipe();
            if (pipe == null) pipe = route.destination.getPipe();
            if (!(pipe instanceof IProvideItems provider)) continue;

            Map<ItemIdentifier, Integer> stock = new HashMap<>();
            provider.getAllItems(stock);
            int available = stock.getOrDefault(item, 0);
            if (available > 0) {
                int toProvide = Math.min(available, amount);
                return new com.Morph.logisticspipes.routing.LogisticsPromise(item, toProvide, provider);
            }
        }
        return null;
    }
}
