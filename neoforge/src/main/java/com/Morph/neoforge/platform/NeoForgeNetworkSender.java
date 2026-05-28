package com.Morph.neoforge.platform;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.Morph.logisticspipes.platform.NetworkSender;

/**
 * Client-side NeoForge {@link NetworkSender}. Wraps {@link PacketDistributor#sendToServer}.
 * Set in {@code ExampleModNeoForgeClient}.
 */
public final class NeoForgeNetworkSender implements NetworkSender {

    public static final NeoForgeNetworkSender INSTANCE = new NeoForgeNetworkSender();

    private NeoForgeNetworkSender() {}

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
