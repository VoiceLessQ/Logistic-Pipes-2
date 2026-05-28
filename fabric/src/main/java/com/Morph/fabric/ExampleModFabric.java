package com.Morph.fabric;

import net.fabricmc.api.ModInitializer;

import com.Morph.ExampleMod;
import com.Morph.fabric.platform.FabricRegistrar;
import com.Morph.logisticspipes.platform.PlatformHelper;

public final class ExampleModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PlatformHelper.set(FabricPlatformHelper.INSTANCE);
        ExampleMod.init(new FabricRegistrar());
        ExampleMod.postInit();
    }
}
