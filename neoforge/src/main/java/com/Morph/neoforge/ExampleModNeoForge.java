package com.Morph.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import com.Morph.ExampleMod;
import com.Morph.logisticspipes.platform.MenuOpener;
import com.Morph.logisticspipes.platform.PlatformHelper;
import com.Morph.neoforge.platform.NeoForgeMenuOpener;
import com.Morph.neoforge.platform.NeoForgeRegistrar;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge(IEventBus modBus) {
        PlatformHelper.set(NeoForgePlatformHelper.INSTANCE);
        MenuOpener.set(NeoForgeMenuOpener.INSTANCE);
        NeoForgeRegistrar registrar = new NeoForgeRegistrar();
        registrar.attachTo(modBus);
        ExampleMod.init(registrar);
        ExampleMod.postInit();
    }
}
