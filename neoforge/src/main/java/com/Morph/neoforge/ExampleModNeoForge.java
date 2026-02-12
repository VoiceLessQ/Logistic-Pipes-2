package com.Morph.neoforge;

import net.neoforged.fml.common.Mod;

import com.Morph.ExampleMod;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge() {
        // Run our common setup.
        ExampleMod.init();
    }
}
