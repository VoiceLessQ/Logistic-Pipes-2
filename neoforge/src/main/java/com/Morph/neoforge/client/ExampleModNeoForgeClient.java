package com.Morph.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import com.Morph.ExampleMod;

@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public final class ExampleModNeoForgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            dev.architectury.registry.menu.MenuRegistry.registerScreenFactory(
                    com.Morph.logisticspipes.LPMenuTypes.REQUEST_PIPE.get(),
                    com.Morph.logisticspipes.gui.screen.RequestPipeScreen::new);
            dev.architectury.registry.menu.MenuRegistry.registerScreenFactory(
                    com.Morph.logisticspipes.LPMenuTypes.CHASSIS_PIPE.get(),
                    com.Morph.logisticspipes.gui.screen.ChassisPipeScreen::new);
            dev.architectury.registry.menu.MenuRegistry.registerScreenFactory(
                    com.Morph.logisticspipes.LPMenuTypes.SUPPLIER_PIPE.get(),
                    com.Morph.logisticspipes.gui.screen.SupplierPipeScreen::new);
        });
    }
}
