package com.Morph.logisticspipes.modules;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;

/**
 * Maps module items to their LogisticsModule factories.
 * Populated during postInit, used by chassis GUI slots.
 */
public final class ModuleRegistry {

    private static final Map<Item, Supplier<LogisticsModule>> factories = new HashMap<>();

    private ModuleRegistry() {}

    public static void register(Item item, Supplier<LogisticsModule> factory) {
        factories.put(item, factory);
    }

    public static LogisticsModule createFor(Item item) {
        Supplier<LogisticsModule> factory = factories.get(item);
        return factory != null ? factory.get() : null;
    }

    public static boolean isModuleItem(Item item) {
        return factories.containsKey(item);
    }
}
