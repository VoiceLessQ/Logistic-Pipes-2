package com.Morph.logisticspipes.utils.item;

import net.minecraft.world.item.ItemStack;

/**
 * An ItemIdentifier paired with a count.
 * Ported directly from LP 1.12.2 — no API changes needed.
 */
public class ItemIdentifierStack {

    public final ItemIdentifier item;
    public int stackSize;

    public ItemIdentifierStack(ItemIdentifier item, int stackSize) {
        this.item = item;
        this.stackSize = stackSize;
    }

    public ItemIdentifierStack(ItemStack stack) {
        this.item = ItemIdentifier.get(stack);
        this.stackSize = stack.getCount();
    }

    public ItemStack makeStack() {
        return item.makeStack(stackSize);
    }

    @Override
    public String toString() {
        return stackSize + "x" + item;
    }
}
