package com.Morph.logisticspipes.pipes;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.Morph.logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import com.Morph.logisticspipes.interfaces.routing.IProvideItems;
import com.Morph.logisticspipes.interfaces.routing.IRequestItems;
import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.platform.PlatformHelper;
import com.Morph.logisticspipes.routing.LogisticsPromise;
import com.Morph.logisticspipes.routing.order.LogisticsOrder;
import com.Morph.logisticspipes.transport.LPTravelingItem;
import com.Morph.logisticspipes.transport.PipeTransportLogistics;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;
import com.Morph.logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Provider pipe — scans adjacent inventories and supplies items to the network on request.
 * Ported from LP 1.12.2 PipeItemsProviderLogistics.
 */
public class PipeItemsProviderLogistics extends CoreRoutedPipe implements IProvideItems {

    public PipeItemsProviderLogistics(Item item) {
        super(new PipeTransportLogistics(), item);
    }

    // -------------------------------------------------------------------------
    // IProvideItems
    // -------------------------------------------------------------------------

    /**
     * Scans all connected inventories (all 6 sides) and populates the list
     * with available item counts. Called by LogisticsManager when building
     * the network item catalogue.
     */
    @Override
    public void getAllItems(Map<ItemIdentifier, Integer> list) {
        if (container == null) return;
        Level level = container.getLevel();
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            if (!container.isConnected(dir)) continue;
            Map<Item, Integer> contents = PlatformHelper.get()
                    .getInventoryContents(level, container.getBlockPos(), dir);
            for (Map.Entry<Item, Integer> entry : contents.entrySet()) {
                ItemIdentifier id = ItemIdentifier.get(entry.getKey());
                list.merge(id, entry.getValue(), Integer::sum);
            }
        }
    }

    /**
     * Extracts the promised items from the adjacent inventory and injects them
     * into this pipe's transport targeted at the destination router.
     */
    @Override
    public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination,
                                   IAdditionalTargetInformation info) {
        if (container == null) return null;
        Level level = container.getLevel();
        if (level == null || level.isClientSide) return null;

        int remaining = promise.numberOfItems;
        ItemStack template = promise.item.makeStack(1);

        for (Direction dir : Direction.values()) {
            if (remaining <= 0) break;
            if (!container.isConnected(dir)) continue;

            ItemStack extracted = PlatformHelper.get().extractItem(
                    level, container.getBlockPos(), dir, template, remaining, false);
            if (extracted.isEmpty()) continue;

            int destRouterId = destination.getRouter().getSimpleID();
            LPTravelingItem traveling = new LPTravelingItem(extracted, destRouterId);
            transport.items.add(traveling);
            container.setChanged();
            remaining -= extracted.getCount();
        }

        int sent = promise.numberOfItems - remaining;
        if (sent <= 0) return null;
        return new LogisticsOrder(promise.item, sent, this);
    }

    @Override
    public com.Morph.logisticspipes.routing.IRouter getRouter() {
        return super.getRouter();
    }

    @Override
    public int getIconIndex(@Nullable Direction direction) {
        return 1; // texture index — Phase 6 rendering
    }
}
