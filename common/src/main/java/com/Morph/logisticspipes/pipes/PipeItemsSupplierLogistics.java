package com.Morph.logisticspipes.pipes;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import com.Morph.logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import com.Morph.logisticspipes.interfaces.routing.IRequestItems;
import com.Morph.logisticspipes.logistics.LogisticsManager;
import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.platform.PlatformHelper;
import com.Morph.logisticspipes.routing.IRouter;
import com.Morph.logisticspipes.routing.LogisticsPromise;
import com.Morph.logisticspipes.transport.PipeTransportLogistics;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;
import com.Morph.logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Supplier pipe — actively maintains a target stock level in adjacent inventory.
 * Every 40 ticks scans connected inventories, computes shortfall, requests from network.
 * Ported from LP 1.12.2 PipeItemsSupplierLogistics — simplified for Phase 5 (no pattern mode).
 */
public class PipeItemsSupplierLogistics extends CoreRoutedPipe implements IRequestItems {

    public static final int SUPPLY_SLOTS = 9;

    private final ItemIdentifierStack[] supplyConfig = new ItemIdentifierStack[SUPPLY_SLOTS];
    private int tickCounter = 0;

    public PipeItemsSupplierLogistics(Item item) {
        super(new PipeTransportLogistics(), item);
    }

    // -------------------------------------------------------------------------
    // Supply logic
    // -------------------------------------------------------------------------

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (container == null || container.getLevel() == null) return;
        if (container.getLevel().isClientSide) return;

        tickCounter++;
        if (tickCounter >= 40) {
            tickCounter = 0;
            checkAndRequest();
        }
    }

    private void checkAndRequest() {
        IRouter router = getRouter();
        if (router == null) return;

        Level level = container.getLevel();
        if (level == null) return;

        Map<ItemIdentifier, Integer> currentStock = new HashMap<>();
        for (Direction dir : Direction.values()) {
            if (!container.isConnected(dir)) continue;
            Map<Item, Integer> contents = PlatformHelper.get()
                    .getInventoryContents(level, container.getBlockPos(), dir);
            for (Map.Entry<Item, Integer> entry : contents.entrySet()) {
                currentStock.merge(ItemIdentifier.get(entry.getKey()), entry.getValue(), Integer::sum);
            }
        }

        for (ItemIdentifierStack config : supplyConfig) {
            if (config == null) continue;
            int current = currentStock.getOrDefault(config.item, 0);
            int deficit = config.stackSize - current;
            if (deficit <= 0) continue;

            LogisticsPromise promise = LogisticsManager.getBestProvider(
                    router.getIRoutersByCost(), config.item, deficit);
            if (promise == null) {
                itemCouldNotBeSent(new ItemIdentifierStack(config.item, deficit), null);
            } else {
                promise.sender.fullFill(promise, this, null);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Supply slot configuration
    // -------------------------------------------------------------------------

    public ItemIdentifierStack[] getSupplyConfig() { return supplyConfig; }

    public void setSupplySlot(int slot, @Nullable ItemIdentifierStack stack) {
        if (slot < 0 || slot >= SUPPLY_SLOTS) return;
        supplyConfig[slot] = stack;
        if (container != null) container.setChanged();
    }

    // -------------------------------------------------------------------------
    // IRequestItems
    // -------------------------------------------------------------------------

    @Override
    public IRouter getRouter() { return super.getRouter(); }

    @Override
    public void itemCouldNotBeSent(ItemIdentifierStack item, IAdditionalTargetInformation info) {
        if (container != null) {
            System.out.println("[LP Supplier] Could not supply " + item
                    + " at " + container.getBlockPos());
        }
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    public void saveExtra(CompoundTag tag) {
        for (int i = 0; i < SUPPLY_SLOTS; i++) {
            if (supplyConfig[i] == null) continue;
            CompoundTag slot = new CompoundTag();
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(supplyConfig[i].item.item);
            if (key == null) continue;
            slot.putString("item", key.toString());
            slot.putInt("amount", supplyConfig[i].stackSize);
            tag.put("supply_" + i, slot);
        }
    }

    @Override
    public void loadExtra(CompoundTag tag) {
        for (int i = 0; i < SUPPLY_SLOTS; i++) {
            String key = "supply_" + i;
            if (!tag.contains(key)) continue;
            CompoundTag slot = tag.getCompound(key);
            Item item = BuiltInRegistries.ITEM.getValue(
                    ResourceLocation.parse(slot.getString("item")));
            if (item != null) {
                supplyConfig[i] = new ItemIdentifierStack(ItemIdentifier.get(item),
                        slot.getInt("amount"));
            }
        }
    }

    @Override
    public int getIconIndex(@Nullable Direction direction) { return 4; }
}
