package com.Morph;

import com.Morph.logisticspipes.LPBlocks;
import com.Morph.logisticspipes.LPItems;

public final class ExampleMod {
    public static final String MOD_ID = "morph";

    public static void init() {
        LPBlocks.register();
        LPItems.register();
    }

    public static void postInit() {
        LPItems.registerPipeFactories();
    }
}
