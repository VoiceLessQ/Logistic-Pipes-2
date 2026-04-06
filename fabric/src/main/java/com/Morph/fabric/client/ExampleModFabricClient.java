package com.Morph.fabric.client;

import net.fabricmc.api.ClientModInitializer;

import dev.architectury.registry.menu.MenuRegistry;

import com.Morph.logisticspipes.LPMenuTypes;
import com.Morph.logisticspipes.gui.screen.ChassisPipeScreen;
import com.Morph.logisticspipes.gui.screen.RequestPipeScreen;
import com.Morph.logisticspipes.gui.screen.SupplierPipeScreen;

public final class ExampleModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuRegistry.registerScreenFactory(LPMenuTypes.REQUEST_PIPE.get(), RequestPipeScreen::new);
        MenuRegistry.registerScreenFactory(LPMenuTypes.CHASSIS_PIPE.get(), ChassisPipeScreen::new);
        MenuRegistry.registerScreenFactory(LPMenuTypes.SUPPLIER_PIPE.get(), SupplierPipeScreen::new);
    }
}
