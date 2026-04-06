package com.Morph.logisticspipes.pipes.basic;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.Morph.logisticspipes.transport.PipeTransportLogistics;

/**
 * Base class for all LP pipe types.
 * Ported from LP 1.12.2 CoreUnroutedPipe.
 *
 * Each concrete pipe type (BasicLogistics, Provider, Request, etc.) extends this.
 * Holds the transport layer and delegates tick/connection logic to it.
 */
public abstract class CoreUnroutedPipe {

    @Nullable
    public LogisticsPipeBlockEntity container;

    public final PipeTransportLogistics transport;

    /** The item that represents this pipe (used for drops). */
    public final Item item;

    public CoreUnroutedPipe(PipeTransportLogistics transport, Item item) {
        this.transport = transport;
        this.item = item;
    }

    public void setContainer(LogisticsPipeBlockEntity be) {
        this.container = be;
        this.transport.setContainer(be);
    }

    /** Called each server tick. Delegates to transport. */
    public void updateEntity() {
        transport.updateEntity();
    }

    /** Called when the block is placed. */
    public void onBlockPlaced() {
        transport.onBlockPlaced();
    }

    /** Called when a neighboring block changes. */
    public void onNeighborBlockChange() {
        transport.onNeighborBlockChange();
    }

    /**
     * Returns true if this pipe can connect to the given neighbor on the given side.
     * Delegates to transport for base check; subclasses may override.
     */
    public boolean canPipeConnect(BlockEntity neighbor, Direction side) {
        return transport.canPipeConnect(neighbor, side);
    }

    /** Abstract: each pipe type defines its own icon index. */
    public abstract int getIconIndex(@Nullable Direction direction);

    /** Override to save pipe-type-specific extra data. */
    public void saveExtra(CompoundTag tag) {}

    /** Override to load pipe-type-specific extra data. */
    public void loadExtra(CompoundTag tag) {}
}
