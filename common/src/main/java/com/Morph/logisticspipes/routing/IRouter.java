package com.Morph.logisticspipes.routing;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Interface for all routers.
 * Ported from LP 1.12.2 IRouter — power and CC/OC removed for Phase 3.
 */
public interface IRouter {

    void destroy();

    void update(boolean doFullRefresh, CoreRoutedPipe pipe);

    boolean isRoutedExit(Direction dir);

    boolean hasRoute(int id, boolean active, ItemIdentifier type);

    ExitRoute getExitFor(int id, boolean active, ItemIdentifier type);

    List<List<ExitRoute>> getRouteTable();

    List<ExitRoute> getIRoutersByCost();

    CoreRoutedPipe getPipe();

    CoreRoutedPipe getCachedPipe();

    boolean isAt(BlockPos pos);

    UUID getId();

    void clearPipeCache();

    int getSimpleID();

    BlockPos getPos();

    boolean isSideDisconnected(Direction dir);

    List<ExitRoute> getDistanceTo(IRouter r);
}
