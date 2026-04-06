package com.Morph.neoforge;

import net.neoforged.fml.common.Mod;

import com.Morph.ExampleMod;
import com.Morph.logisticspipes.platform.PlatformHelper;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge() {
        PlatformHelper.set(NeoForgePlatformHelper.INSTANCE);
        // Run our common setup.
        ExampleMod.init();
    }
}
