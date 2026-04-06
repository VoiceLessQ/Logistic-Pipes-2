package com.Morph.logisticspipes.routing.pathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;

/**
 * Provides information about a pipe for the PathFinder.
 * Ported from LP 1.12.2 IPipeInformationProvider — simplified for Phase 3.
 */
public interface IPipeInformationProvider {

    BlockPos getPos();

    Level getWorld();

    /**
     * Returns true if this pipe can accept a connection on the given side.
     * Unrouted pipes relay connections; routed pipes are routing endpoints.
     */
    boolean canConnect(Direction side, IPipeInformationProvider other, boolean ignoreSystemDisconnection);

    boolean isRoutedPipe();

    CoreRoutedPipe getRoutedPipe();

    BlockEntity getBlockEntity();
}
