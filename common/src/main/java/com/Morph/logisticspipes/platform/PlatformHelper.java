package com.Morph.logisticspipes.platform;

import javax.annotation.Nullable;

public final class PlatformHelper {

    @Nullable
    private static IPlatformHelper INSTANCE;

    private PlatformHelper() {}

    public static void set(IPlatformHelper helper) {
        if (INSTANCE != null) throw new IllegalStateException("PlatformHelper already set");
        INSTANCE = helper;
    }

    public static IPlatformHelper get() {
        if (INSTANCE == null) throw new IllegalStateException("PlatformHelper not yet initialized");
        return INSTANCE;
    }
}
