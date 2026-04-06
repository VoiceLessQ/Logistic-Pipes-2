package com.Morph.logisticspipes.utils;

/**
 * Reply from a pipe indicating it can accept an item, with priority.
 * Ported directly from LP 1.12.2 — no API changes needed.
 */
public final class SinkReply {

    public enum FixedPriority {
        DefaultRoute,
        ModBasedItemSink,
        OreDictItemSink,
        EnchantmentItemSink,
        Terminus,
        ItemSink,
        PassiveSupplier,
    }

    public final FixedPriority fixedPriority;
    public final int customPriority;
    public final boolean isPassive;
    public final boolean isDefault;
    public final int maxNumberOfItems;

    public SinkReply(FixedPriority fixedPriority, int customPriority,
                     boolean isPassive, boolean isDefault, int maxNumberOfItems) {
        this.fixedPriority = fixedPriority;
        this.customPriority = customPriority;
        this.isPassive = isPassive;
        this.isDefault = isDefault;
        this.maxNumberOfItems = maxNumberOfItems;
    }
}
