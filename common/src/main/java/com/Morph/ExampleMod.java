package com.Morph;

import com.Morph.logisticspipes.LPBlocks;
import com.Morph.logisticspipes.LPCreativeTab;
import com.Morph.logisticspipes.LPItems;
import com.Morph.logisticspipes.LPMenuTypes;
import com.Morph.logisticspipes.network.LPNetworking;

public final class ExampleMod {
    public static final String MOD_ID = "morph";

    public static void init() {
        LPBlocks.register();
        LPItems.register();
        LPMenuTypes.register();
        LPCreativeTab.register();
        LPNetworking.init();
    }

    public static void postInit() {
        LPItems.registerPipeFactories();
    }
}
