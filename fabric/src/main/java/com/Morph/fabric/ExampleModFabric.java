package com.Morph.fabric;

import net.fabricmc.api.ModInitializer;

import com.Morph.ExampleMod;
import com.Morph.fabric.network.FabricNetworking;
import com.Morph.fabric.platform.FabricMenuOpener;
import com.Morph.fabric.platform.FabricRegistrar;
import com.Morph.logisticspipes.platform.MenuOpener;
import com.Morph.logisticspipes.platform.PlatformHelper;

public final class ExampleModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PlatformHelper.set(FabricPlatformHelper.INSTANCE);
        MenuOpener.set(FabricMenuOpener.INSTANCE);
        ExampleMod.init(new FabricRegistrar());
        FabricNetworking.register();
        ExampleMod.postInit();
    }
}
