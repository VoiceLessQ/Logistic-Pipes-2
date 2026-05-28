package com.Morph.logisticspipes.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Platform-agnostic helper for sending a typed payload from the client to the server.
 *
 * Set once at client init:
 *   - Fabric: {@code FabricNetworkSender.INSTANCE} (wraps {@code ClientPlayNetworking.send})
 *   - NeoForge: {@code NeoForgeNetworkSender.INSTANCE} (wraps {@code PacketDistributor.sendToServer})
 *
 * Never set on a dedicated server — server code must not call {@link #get()}.
 *
 * Replaces {@code dev.architectury.networking.NetworkManager.sendToServer}.
 */
public interface NetworkSender {

    void sendToServer(CustomPacketPayload payload);

    final class Holder {
        static NetworkSender instance;
        private Holder() {}
    }

    static void set(NetworkSender s) { Holder.instance = s; }
    static NetworkSender get()       { return Holder.instance; }
}
