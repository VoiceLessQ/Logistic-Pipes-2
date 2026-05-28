package com.Morph.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

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

        // postInit calls *.get() on DeferredHolders (to wire PipeRegistry/ModuleRegistry
        // factories from item instances). On NeoForge those holders are unbound until
        // RegisterEvent fires, so postInit MUST run no earlier than FMLCommonSetupEvent —
        // by then every DeferredRegister has flushed and *.get() is safe.
        modBus.addListener(FMLCommonSetupEvent.class, event -> ExampleMod.postInit());
    }
}
