package com.Morph.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

import com.Morph.fabric.platform.FabricNetworkSender;
import com.Morph.logisticspipes.LPBlocks;
import com.Morph.logisticspipes.LPMenuTypes;
import com.Morph.logisticspipes.client.LPPipeRenderer;
import com.Morph.logisticspipes.gui.screen.ChassisPipeScreen;
import com.Morph.logisticspipes.gui.screen.PowerJunctionScreen;
import com.Morph.logisticspipes.gui.screen.RequestPipeScreen;
import com.Morph.logisticspipes.gui.screen.SupplierPipeScreen;
import com.Morph.logisticspipes.platform.NetworkSender;

public final class ExampleModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkSender.set(FabricNetworkSender.INSTANCE);

        MenuScreens.register(LPMenuTypes.REQUEST_PIPE.get(), RequestPipeScreen::new);
        MenuScreens.register(LPMenuTypes.CHASSIS_PIPE.get(), ChassisPipeScreen::new);
        MenuScreens.register(LPMenuTypes.SUPPLIER_PIPE.get(), SupplierPipeScreen::new);
        MenuScreens.register(LPMenuTypes.POWER_JUNCTION.get(), PowerJunctionScreen::new);

        BlockEntityRendererRegistry.register(LPBlocks.PIPE_BLOCK_ENTITY.get(), LPPipeRenderer::new);
    }
}
