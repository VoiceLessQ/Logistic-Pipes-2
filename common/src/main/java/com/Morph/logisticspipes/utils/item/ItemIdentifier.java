package com.Morph.logisticspipes.utils.item;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Immutable, interned item identity key.
 * Ported from LP 1.12.2 — simplified for 1.21.1 (no damage/meta, uses Item identity).
 *
 * In 1.21.1, item variants are expressed via DataComponents. For Phase 3 we key only
 * on Item type; component-aware matching can be added in Phase 5.
 */
public final class ItemIdentifier implements Comparable<ItemIdentifier> {

    private static final ConcurrentHashMap<Item, ItemIdentifier> cache = new ConcurrentHashMap<>();

    @Nonnull
    public final Item item;

    private ItemIdentifier(@Nonnull Item item) {
        this.item = item;
    }

    /** Returns the interned ItemIdentifier for the given item type. */
    public static ItemIdentifier get(@Nonnull Item item) {
        return cache.computeIfAbsent(item, ItemIdentifier::new);
    }

    /** Returns the interned ItemIdentifier for the item type of the given stack. */
    public static ItemIdentifier get(@Nonnull ItemStack stack) {
        return get(stack.getItem());
    }

    /** Makes an ItemStack of count 1 for this identifier. */
    public ItemStack makeStack(int count) {
        return new ItemStack(item, count);
    }

    public static void clearCache() {
        cache.clear();
    }

    @Override
    public int compareTo(ItemIdentifier other) {
        return Integer.compare(
                Item.getId(this.item),
                Item.getId(other.item)
        );
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj; // interned — reference equality is correct
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "ItemIdentifier(" + item + ")";
    }
}
