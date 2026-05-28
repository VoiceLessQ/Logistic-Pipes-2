package com.Morph.logisticspipes.transport;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * An item currently in transit through a pipe.
 * Ported from LP 1.12.2 LPTravelingItem — server-side only for Phase 2.
 */
public class LPTravelingItem {

    // How many ticks until this item moves to the next pipe/inventory
    public int ticksInPipe;

    // Which direction this item is heading
    public Direction output;

    // The item payload
    public ItemStack stack;

    // Unique ID for client sync
    public int id;

    /**
     * Destination router simpleID — set for routed items.
     * -1 means unrouted (dumb transport, Phase 2 behaviour).
     */
    public int destinationRouterId = -1;

    private static int nextId = 0;

    public LPTravelingItem(ItemStack stack, Direction output) {
        this.stack = stack.copy();
        this.output = output;
        this.ticksInPipe = 8; // ticks to traverse one pipe segment
        this.id = nextId++;
    }

    public LPTravelingItem(ItemStack stack, int destinationRouterId) {
        this.stack = stack.copy();
        this.output = null; // resolved each hop
        this.destinationRouterId = destinationRouterId;
        this.ticksInPipe = 8;
        this.id = nextId++;
    }

    private LPTravelingItem() {}

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ticks", ticksInPipe);
        tag.putInt("dir", output.ordinal());
        tag.putInt("id", id);
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        tag.putString("stackItem", key != null ? key.toString() : "minecraft:air");
        tag.putInt("stackCount", stack.getCount());
        return tag;
    }

    public static LPTravelingItem fromNBT(CompoundTag tag) {
        LPTravelingItem item = new LPTravelingItem();
        item.ticksInPipe = tag.getInt("ticks");
        item.output = Direction.values()[tag.getInt("dir")];
        item.id = tag.getInt("id");
        net.minecraft.world.item.Item stackItem = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.parse(tag.getString("stackItem")))
                .orElse(net.minecraft.world.item.Items.AIR);
        item.stack = new ItemStack(stackItem, tag.getInt("stackCount"));
        return item;
    }
}
