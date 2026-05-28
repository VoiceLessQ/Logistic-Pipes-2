package com.Morph.logisticspipes.pipes;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
 * Supplier pipe — maintains target stock in adjacent inventory by requesting from network.
 * Each of 9 supply slots holds: item type + target amount (encoded as stack count).
 * Every 40 ticks: scans connected inventories, computes deficit, requests shortfall.
 * Ported from LP 1.12.2 PipeItemsSupplierLogistics — simplified for Phase 5/6.
 */
public class PipeItemsSupplierLogistics extends CoreRoutedPipe implements IRequestItems {

    public static final int SUPPLY_SLOTS = 9;

    /** GUI-accessible container: item type + stack size = target amount per slot. */
    public final SimpleContainer supplyContainer = new SimpleContainer(SUPPLY_SLOTS);

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

        if (++tickCounter >= 40) {
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

        for (int i = 0; i < SUPPLY_SLOTS; i++) {
            ItemStack stack = supplyContainer.getItem(i);
            if (stack.isEmpty()) continue;
            ItemIdentifier id = ItemIdentifier.get(stack.getItem());
            int targetAmount = stack.getCount();
            int deficit = targetAmount - currentStock.getOrDefault(id, 0);
            if (deficit <= 0) continue;

            LogisticsPromise promise = LogisticsManager.getBestProvider(
                    router.getIRoutersByCost(), id, deficit);
            if (promise == null) {
                itemCouldNotBeSent(new ItemIdentifierStack(id, deficit), null);
            } else {
                promise.sender.fullFill(promise, this, null);
            }
        }
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
        ListTag list = new ListTag();
        for (int i = 0; i < SUPPLY_SLOTS; i++) {
            ItemStack stack = supplyContainer.getItem(i);
            if (stack.isEmpty()) continue;
            CompoundTag slot = new CompoundTag();
            slot.putInt("slot", i);
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key != null) slot.putString("item", key.toString());
            slot.putInt("amount", stack.getCount());
            list.add(slot);
        }
        tag.put("supply", list);
    }

    @Override
    public void loadExtra(CompoundTag tag) {
        if (!tag.contains("supply")) return;
        ListTag list = tag.getList("supply", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slot = list.getCompound(i);
            int slotIndex = slot.getInt("slot");
            if (slotIndex < SUPPLY_SLOTS && slot.contains("item")) {
                Item item = BuiltInRegistries.ITEM.getOptional(
                        ResourceLocation.parse(slot.getString("item"))).orElse(null);
                if (item != null) {
                    supplyContainer.setItem(slotIndex,
                            new ItemStack(item, slot.getInt("amount")));
                }
            }
        }
    }

    @Override
    public int getIconIndex(@Nullable Direction direction) { return 4; }
}
