package com.Morph;

import com.Morph.logisticspipes.LPBlocks;
import com.Morph.logisticspipes.LPCreativeTab;
import com.Morph.logisticspipes.LPItems;
import com.Morph.logisticspipes.LPMenuTypes;
import com.Morph.logisticspipes.config.LPConfig;
import com.Morph.logisticspipes.network.LPNetworking;
import com.Morph.logisticspipes.platform.Registrar;

public final class ExampleMod {
    public static final String MOD_ID = "morph";

    /**
     * Called by each platform's mod-init class with its Registrar implementation.
     * After all category init() calls, {@link Registrar#commit()} flushes any
     * deferred registrations (NeoForge) or is a no-op (Fabric).
     */
    public static void init(Registrar registrar) {
        LPConfig.load();
        LPBlocks.init(registrar);
        LPItems.init(registrar);
        LPMenuTypes.init(registrar);
        LPCreativeTab.init(registrar);
        registrar.commit();
        LPNetworking.init();
    }

    public static void postInit() {
        LPItems.registerPipeFactories();
    }
}
