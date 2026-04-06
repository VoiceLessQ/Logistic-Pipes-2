package com.Morph.logisticspipes.pipes;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;

import com.Morph.logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import com.Morph.logisticspipes.interfaces.routing.IRequestItems;
import com.Morph.logisticspipes.logistics.LogisticsManager;
import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.routing.IRouter;
import com.Morph.logisticspipes.routing.LogisticsPromise;
import com.Morph.logisticspipes.routing.order.LogisticsOrder;
import com.Morph.logisticspipes.transport.PipeTransportLogistics;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;
import com.Morph.logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Request pipe — lets players request items from the network.
 * Ported from LP 1.12.2 PipeItemsRequestLogistics.
 *
 * Phase 4: no GUI yet. Call requestItem() programmatically for testing.
 * Phase 4 GUI wired in Phase 4 GUI milestone.
 */
public class PipeItemsRequestLogistics extends CoreRoutedPipe implements IRequestItems {

    public PipeItemsRequestLogistics(Item item) {
        super(new PipeTransportLogistics(), item);
    }

    // -------------------------------------------------------------------------
    // IRequestItems
    // -------------------------------------------------------------------------

    @Override
    public IRouter getRouter() {
        return super.getRouter();
    }

    @Override
    public void itemCouldNotBeSent(ItemIdentifierStack item, IAdditionalTargetInformation info) {
        // Phase 4: log to console. Phase 4 GUI will show this to the player.
        if (container != null) {
            System.out.println("[LP] Could not send " + item + " to " + container.getBlockPos());
        }
    }

    // -------------------------------------------------------------------------
    // Request API
    // -------------------------------------------------------------------------

    /**
     * Request a specific item from the network.
     * Finds the best provider via LogisticsManager and calls fullFill().
     *
     * @return the LogisticsOrder if fulfilled, null if nothing available
     */
    @Nullable
    public LogisticsOrder requestItem(ItemIdentifier item, int amount) {
        IRouter router = getRouter();
        if (router == null) return null;

        LogisticsPromise promise = LogisticsManager.getBestProvider(
                router.getIRoutersByCost(), item, amount);
        if (promise == null) {
            itemCouldNotBeSent(new ItemIdentifierStack(item, amount), null);
            return null;
        }

        return promise.sender.fullFill(promise, this, null);
    }

    /**
     * Returns all items currently available in the network from this pipe's perspective.
     * Used to populate the request GUI.
     */
    public Map<ItemIdentifier, Integer> getNetworkItems() {
        IRouter router = getRouter();
        if (router == null) return Map.of();
        return LogisticsManager.getAvailableItems(router.getIRoutersByCost());
    }

    @Override
    public int getIconIndex(@Nullable Direction direction) {
        return 2; // texture index — Phase 6 rendering
    }
}
