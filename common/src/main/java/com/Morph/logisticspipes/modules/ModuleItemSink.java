package com.Morph.logisticspipes.modules;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import com.Morph.logisticspipes.utils.SinkReply;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Chassis module that accepts items matching a filter list, or all items if defaultRoute=true.
 * Ported from LP 1.12.2 ModuleItemSink — simplified for Phase 5 (no fuzzy, no upgrade manager).
 */
public class ModuleItemSink extends LogisticsModule {

    private final List<ItemIdentifier> filterItems = new ArrayList<>(9);
    private boolean defaultRoute = false;

    private static final SinkReply SINK_REPLY = new SinkReply(
            SinkReply.FixedPriority.ItemSink, 0, true, false, Integer.MAX_VALUE);
    private static final SinkReply DEFAULT_REPLY = new SinkReply(
            SinkReply.FixedPriority.DefaultRoute, 0, true, true, Integer.MAX_VALUE);

    @Override
    public void tick() {}

    @Override
    @Nullable
    public SinkReply sinksItem(ItemIdentifier item) {
        for (ItemIdentifier filter : filterItems) {
            if (filter == item) return SINK_REPLY;
        }
        if (defaultRoute) return DEFAULT_REPLY;
        return null;
    }

    public List<ItemIdentifier> getFilterItems() { return filterItems; }
    public boolean isDefaultRoute() { return defaultRoute; }
    public void setDefaultRoute(boolean value) { this.defaultRoute = value; }

    @Override
    public void saveToNBT(CompoundTag tag, String prefix) {
        tag.putBoolean(prefix + "defaultRoute", defaultRoute);
        ListTag list = new ListTag();
        for (ItemIdentifier id : filterItems) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(id.item);
            if (key != null) list.add(StringTag.valueOf(key.toString()));
        }
        tag.put(prefix + "filter", list);
    }

    @Override
    public void loadFromNBT(CompoundTag tag, String prefix) {
        defaultRoute = tag.getBoolean(prefix + "defaultRoute");
        filterItems.clear();
        ListTag list = tag.getList(prefix + "filter", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(list.getString(i))).orElse(null);
            if (item != null) filterItems.add(ItemIdentifier.get(item));
        }
    }
}
