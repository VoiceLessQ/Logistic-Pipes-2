package com.Morph.neoforge.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.Morph.ExampleMod;
import com.Morph.logisticspipes.network.LPNetworking;
import com.Morph.logisticspipes.network.RequestItemPayload;

/**
 * NeoForge-side networking registration. One MOD-bus event handler binds every
 * LP payload type + its receiver via the supplied {@link PayloadRegistrar}.
 *
 * Receivers marshal back to the main thread via {@code context.enqueueWork(...)}
 * before calling the common {@link LPNetworking} handler.
 */
@EventBusSubscriber(modid = ExampleMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeNetworking {

    private NeoForgeNetworking() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ExampleMod.MOD_ID);

        registrar.playToServer(
                RequestItemPayload.TYPE,
                RequestItemPayload.CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        LPNetworking.handleRequestItem(context.player(), payload)));
    }
}
