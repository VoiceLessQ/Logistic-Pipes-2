package com.Morph.logisticspipes.modules;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.Morph.logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import com.Morph.logisticspipes.interfaces.routing.IProvideItems;
import com.Morph.logisticspipes.interfaces.routing.IRequestItems;
import com.Morph.logisticspipes.platform.PlatformHelper;
import com.Morph.logisticspipes.routing.IRouter;
import com.Morph.logisticspipes.routing.LogisticsPromise;
import com.Morph.logisticspipes.routing.order.LogisticsOrder;
import com.Morph.logisticspipes.transport.LPTravelingItem;
import com.Morph.logisticspipes.utils.SinkReply;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Chassis module that provides items from adjacent inventories.
 * Ported from LP 1.12.2 ModuleProvider — simplified for Phase 5.
 */
public class ModuleProvider extends LogisticsModule implements IProvideItems {

    @Override
    public void tick() {}

    @Override
    @Nullable
    public SinkReply sinksItem(ItemIdentifier item) { return null; }

    @Override
    public void getAllItems(Map<ItemIdentifier, Integer> list) {
        if (_service == null || _service.container == null) return;
        Level level = _service.container.getLevel();
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            if (!_service.container.isConnected(dir)) continue;
            Map<Item, Integer> contents = PlatformHelper.get()
                    .getInventoryContents(level, _service.container.getBlockPos(), dir);
            for (Map.Entry<Item, Integer> entry : contents.entrySet()) {
                ItemIdentifier id = ItemIdentifier.get(entry.getKey());
                list.merge(id, entry.getValue(), Integer::sum);
            }
        }
    }

    @Override
    public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination,
                                   IAdditionalTargetInformation info) {
        if (_service == null || _service.container == null) return null;
        Level level = _service.container.getLevel();
        if (level == null || level.isClientSide) return null;

        int remaining = promise.numberOfItems;
        ItemStack template = promise.item.makeStack(1);

        for (Direction dir : Direction.values()) {
            if (remaining <= 0) break;
            if (!_service.container.isConnected(dir)) continue;

            ItemStack extracted = PlatformHelper.get().extractItem(
                    level, _service.container.getBlockPos(), dir, template, remaining, false);
            if (extracted.isEmpty()) continue;

            int destRouterId = destination.getRouter().getSimpleID();
            LPTravelingItem traveling = new LPTravelingItem(extracted, destRouterId);
            _service.transport.items.add(traveling);
            _service.container.setChanged();
            remaining -= extracted.getCount();
        }

        int sent = promise.numberOfItems - remaining;
        if (sent <= 0) return null;
        return new LogisticsOrder(promise.item, sent, this);
    }

    @Override
    public IRouter getRouter() {
        return _service != null ? _service.getRouter() : null;
    }
}
