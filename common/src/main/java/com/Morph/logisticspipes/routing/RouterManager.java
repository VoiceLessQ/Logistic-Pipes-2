package com.Morph.logisticspipes.routing;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * Global registry of all active ServerRouters.
 * Ported from LP 1.12.2 RouterManager — simplified, dimension handling via Level key.
 */
public final class RouterManager {

    private static final ConcurrentHashMap<UUID, ServerRouter> BY_UUID = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ServerRouter> BY_SIMPLE_ID = new ConcurrentHashMap<>();

    private RouterManager() {}

    public static ServerRouter getOrCreateRouter(UUID id, net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        if (id != null && BY_UUID.containsKey(id)) {
            return BY_UUID.get(id);
        }
        ServerRouter router = new ServerRouter(id != null ? id : UUID.randomUUID(), level, pos);
        BY_UUID.put(router.getId(), router);
        BY_SIMPLE_ID.put(router.getSimpleID(), router);
        return router;
    }

    public static void removeRouter(UUID id) {
        ServerRouter router = BY_UUID.remove(id);
        if (router != null) {
            BY_SIMPLE_ID.remove(router.getSimpleID());
        }
    }

    @Nullable
    public static ServerRouter getRouterBySimpleID(int simpleID) {
        return BY_SIMPLE_ID.get(simpleID);
    }

    @Nullable
    public static ServerRouter getRouterByUUID(UUID id) {
        return BY_UUID.get(id);
    }

    public static int getBiggestSimpleID() {
        return BY_SIMPLE_ID.isEmpty() ? 0 : BY_SIMPLE_ID.keySet().stream().mapToInt(i -> i).max().orElse(0);
    }

    public static Collection<ServerRouter> getAllRouters() {
        return BY_UUID.values();
    }

    /** Called on server stop to clear all routers. */
    public static void cleanup() {
        BY_UUID.clear();
        BY_SIMPLE_ID.clear();
    }
}
