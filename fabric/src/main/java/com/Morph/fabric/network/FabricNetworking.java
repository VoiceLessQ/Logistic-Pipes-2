package com.Morph.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.Morph.logisticspipes.network.LPNetworking;
import com.Morph.logisticspipes.network.RequestItemPayload;

/**
 * Fabric-side networking registration.
 *
 * Registers the payload type on both endpoints (C2S only today, but kept symmetric
 * with the platform helper shape) and wires the server-side receiver. Receivers
 * marshal back to the main thread via {@code context.player().server.execute(...)}
 * before calling the common {@link LPNetworking} handler.
 */
public final class FabricNetworking {

    private FabricNetworking() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(RequestItemPayload.TYPE, RequestItemPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestItemPayload.TYPE, (payload, context) ->
                context.player().server.execute(() ->
                        LPNetworking.handleRequestItem(context.player(), payload)));
    }
}
