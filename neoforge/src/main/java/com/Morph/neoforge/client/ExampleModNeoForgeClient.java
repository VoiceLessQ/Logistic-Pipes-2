package com.Morph.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import com.Morph.ExampleMod;
import com.Morph.logisticspipes.LPBlocks;
import com.Morph.logisticspipes.LPMenuTypes;
import com.Morph.logisticspipes.client.LPPipeRenderer;
import com.Morph.logisticspipes.gui.screen.ChassisPipeScreen;
import com.Morph.logisticspipes.gui.screen.PowerJunctionScreen;
import com.Morph.logisticspipes.gui.screen.RequestPipeScreen;
import com.Morph.logisticspipes.gui.screen.SupplierPipeScreen;
import com.Morph.logisticspipes.platform.NetworkSender;
import com.Morph.neoforge.platform.NeoForgeNetworkSender;

@EventBusSubscriber(modid = ExampleMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExampleModNeoForgeClient {

    static {
        NetworkSender.set(NeoForgeNetworkSender.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(LPMenuTypes.REQUEST_PIPE.get(), RequestPipeScreen::new);
        event.register(LPMenuTypes.CHASSIS_PIPE.get(), ChassisPipeScreen::new);
        event.register(LPMenuTypes.SUPPLIER_PIPE.get(), SupplierPipeScreen::new);
        event.register(LPMenuTypes.POWER_JUNCTION.get(), PowerJunctionScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(LPBlocks.PIPE_BLOCK_ENTITY.get(), LPPipeRenderer::new);
    }
}
