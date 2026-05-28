package com.Morph.logisticspipes.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.Morph.logisticspipes.pipes.PipeItemsRequestLogistics;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Platform-agnostic LP packet handler bodies.
 *
 * Payload types live in their own files ({@link RequestItemPayload} etc.).
 * Platform-specific wiring (codec registration + receiver registration) lives in
 * {@code com.Morph.fabric.network.FabricNetworking} and
 * {@code com.Morph.neoforge.network.NeoForgeNetworking}; both delegate to the
 * static handlers below so the LP-domain logic stays in one place.
 *
 * Receivers must call these from the main thread (already true on both
 * platforms — Fabric uses {@code context.player().server.execute(...)} and
 * NeoForge uses {@code context.enqueueWork(...)}).
 */
public final class LPNetworking {

    private LPNetworking() {}

    /** Phase 3 stub — kept so {@code ExampleMod.init()} still compiles. Platforms register payloads themselves. */
    public static void init() {
        // no-op
    }

    /** C2S: client requested an item; resolve the pipe at {@code pos} and forward to its request handler. */
    public static void handleRequestItem(Player player, RequestItemPayload payload) {
        Level level = player.level();
        Item item = BuiltInRegistries.ITEM.getOptional(payload.itemKey()).orElse(null);
        if (item == null) return;
        BlockEntity be = level.getBlockEntity(payload.pos());
        if (be instanceof LogisticsPipeBlockEntity lpbe
                && lpbe.getPipe() instanceof PipeItemsRequestLogistics req) {
            req.requestItem(ItemIdentifier.get(item), payload.amount());
        }
    }
}
