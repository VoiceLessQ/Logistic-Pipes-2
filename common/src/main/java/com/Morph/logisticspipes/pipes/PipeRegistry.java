package com.Morph.logisticspipes.pipes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;

import com.Morph.logisticspipes.pipes.basic.CoreUnroutedPipe;

/**
 * Maps pipe items to their pipe class factories.
 * Used by LogisticsPipeBlockEntity to instantiate the right pipe class on placement.
 */
public final class PipeRegistry {

    private static final Map<Item, Supplier<CoreUnroutedPipe>> factories = new HashMap<>();

    private PipeRegistry() {}

    public static void register(Item item, Supplier<CoreUnroutedPipe> factory) {
        factories.put(item, factory);
    }

    public static CoreUnroutedPipe createFor(Item item) {
        Supplier<CoreUnroutedPipe> factory = factories.get(item);
        return factory != null ? factory.get() : null;
    }

    public static boolean isKnownPipeItem(Item item) {
        return factories.containsKey(item);
    }
}
