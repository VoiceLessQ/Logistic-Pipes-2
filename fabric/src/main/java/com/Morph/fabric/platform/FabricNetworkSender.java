package com.Morph.fabric.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.Morph.logisticspipes.platform.NetworkSender;

/**
 * Client-side Fabric {@link NetworkSender}. Wraps {@link ClientPlayNetworking#send}.
 * Set in {@code ExampleModFabricClient}.
 */
public final class FabricNetworkSender implements NetworkSender {

    public static final FabricNetworkSender INSTANCE = new FabricNetworkSender();

    private FabricNetworkSender() {}

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
