package com.Morph.logisticspipes.pipes.basic;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;

import com.Morph.logisticspipes.api.ILogisticsPowerProvider;
import com.Morph.logisticspipes.routing.IRouter;
import com.Morph.logisticspipes.routing.RouterManager;
import com.Morph.logisticspipes.routing.ServerRouter;
import com.Morph.logisticspipes.transport.PipeTransportLogistics;

/**
 * Base class for all routed LP pipe types (BasicLogistics, Provider, Request, etc.).
 * Ported from LP 1.12.2 CoreRoutedPipe — power, CC/OC, upgrade manager, stats
 * removed for Phase 3.
 *
 * Adds a ServerRouter to CoreUnroutedPipe. The router is created lazily on first
 * server tick and destroyed when the pipe is removed.
 */
public abstract class CoreRoutedPipe extends CoreUnroutedPipe {

    /** Persisted router UUID — survives chunk reload. */
    private UUID routerUUID;

    @Nullable
    private ServerRouter routerCache;

    /** True until the router has been initialized on the first tick. */
    private boolean needsInit = true;

    public CoreRoutedPipe(PipeTransportLogistics transport, Item item) {
        super(transport, item);
    }

    // -------------------------------------------------------------------------
    // Router access
    // -------------------------------------------------------------------------

    public IRouter getRouter() {
        if (routerCache != null) return routerCache;
        if (container == null) return null;
        if (!(container.getLevel() instanceof ServerLevel serverLevel)) return null;

        routerCache = RouterManager.getOrCreateRouter(routerUUID, serverLevel, container.getBlockPos());
        routerUUID = routerCache.getId();
        return routerCache;
    }

    public UUID getRouterId() {
        return routerUUID;
    }

    public void setRouterId(UUID id) {
        this.routerUUID = id;
        this.routerCache = null;
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    @Override
    public void updateEntity() {
        super.updateEntity(); // transport tick

        if (container == null || container.getLevel() == null) return;
        if (container.getLevel().isClientSide) return;

        if (needsInit) {
            needsInit = false;
            getRouter(); // create router on first tick
        }

        IRouter router = getRouter();
        if (router != null) {
            // Refresh routing table periodically (every 20 ticks = 1 second)
            if (container.getLevel().getGameTime() % 20 == 0) {
                router.update(false, this);
            }
        }
    }

    @Override
    public void onBlockPlaced() {
        super.onBlockPlaced();
        needsInit = true;
    }

    @Override
    public void onNeighborBlockChange() {
        super.onNeighborBlockChange();
        if (routerCache != null) {
            routerCache.flagConnectionDirty();
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Called when this pipe block entity is removed from the world. */
    public void onChunkUnload() {
        if (routerCache != null) {
            routerCache.clearPipeCache();
        }
    }

    public void onBlockRemoved() {
        if (routerCache != null) {
            routerCache.destroy();
            routerCache = null;
        }
    }

    // -------------------------------------------------------------------------
    // NBT helpers (called by LogisticsPipeBlockEntity)
    // -------------------------------------------------------------------------

    public void saveRouterUUID(net.minecraft.nbt.CompoundTag tag) {
        if (routerUUID != null) {
            tag.putUUID("routerUUID", routerUUID);
        }
    }

    public void loadRouterUUID(net.minecraft.nbt.CompoundTag tag) {
        if (tag.hasUUID("routerUUID")) {
            routerUUID = tag.getUUID("routerUUID");
        }
        routerCache = null;
    }

    // -------------------------------------------------------------------------
    // Power
    // -------------------------------------------------------------------------

    /**
     * Try to draw {@code amount} LP units from the network's power providers.
     * Walks providers in cost order (nearest first); returns true on the first
     * provider that successfully serves the request.
     */
    public boolean useEnergy(int amount) {
        IRouter router = getRouter();
        if (!(router instanceof ServerRouter sr)) return false;
        for (ILogisticsPowerProvider p : sr.getPowerProvidersInNetwork()) {
            if (p.useEnergy(amount)) return true;
        }
        return false;
    }

    /** Same as {@link #useEnergy} but non-consuming — checks availability only. */
    public boolean canUseEnergy(int amount) {
        IRouter router = getRouter();
        if (!(router instanceof ServerRouter sr)) return false;
        for (ILogisticsPowerProvider p : sr.getPowerProvidersInNetwork()) {
            if (p.canUseEnergy(amount)) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------------------

    @Override
    public boolean canPipeConnect(net.minecraft.world.level.block.entity.BlockEntity neighbor, Direction side) {
        return super.canPipeConnect(neighbor, side);
    }
}
