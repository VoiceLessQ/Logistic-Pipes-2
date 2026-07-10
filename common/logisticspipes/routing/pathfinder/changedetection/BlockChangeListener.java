package logisticspipes.routing.pathfinder.changedetection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;

import logisticspipes.asm.te.ILPTEInformation;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.ticks.QueuedTasks;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

/**
 * Listens for block-place and block-break events so LP pipes can refresh their
 * tile cache when a non-LP neighbour appears or disappears.
 *
 * Previously handled globally via ASM injection into all TileEntities; now done
 * via NeoForge block events registered on {@code MinecraftForge.EVENT_BUS}.
 *
 * <p>Both events fire BEFORE the change is committed, so notifications are
 * queued via {@link QueuedTasks} to run on the next server tick when the new
 * block state is already in place.</p>
 */
public class BlockChangeListener {

    /**
     * Fired before a block is placed.  Queue a cache refresh so adjacent LP
     * pipes pick up the new neighbour on the next tick.
     */
    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level world)) return;
        if (!MainProxy.isServer(world)) return;
        final BlockPos pos = event.getPos();
        QueuedTasks.queueTask(() -> {
            notifyAdjacentPipes(world, pos);
            return null;
        });
    }

    /**
     * Fired before a block is broken.  Queue a cache refresh so adjacent LP
     * pipes notice the neighbour is gone on the next tick.
     */
    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level world)) return;
        if (!MainProxy.isServer(world)) return;
        final BlockPos pos = event.getPos();
        QueuedTasks.queueTask(() -> {
            notifyAdjacentPipes(world, pos);
            return null;
        });
    }

    /**
     * For each adjacent position to {@code changedPos}, if there is an LP item
     * pipe, tell it to refresh the side that faces {@code changedPos}.
     */
    private static void notifyAdjacentPipes(Level world, BlockPos changedPos) {
        DoubleCoordinates changed = new DoubleCoordinates(
                changedPos.getX(), changedPos.getY(), changedPos.getZ());

        for (Direction dir : Direction.values()) {
            DoubleCoordinates adjacent = CoordinateUtils.sum(changed, dir);
            if (!adjacent.blockExists(world)) continue;

            BlockEntity adjacentTE = adjacent.getTileEntity(world);
            if (adjacentTE == null) continue;

            // Guard: only LP-managed TEs carry ILPTEInformation
            if (!(adjacentTE instanceof ILPTEInformation lpInfo)) continue;
            if (lpInfo.getLPTileEntityObject() == null) continue;

            if (SimpleServiceLocator.pipeInformationManager.isItemPipe(adjacentTE)) {
                // dir goes from changed → adjacent; from adjacent's perspective
                // the changed block is in direction dir.getOpposite().
                SimpleServiceLocator.pipeInformationManager
                        .getInformationProviderFor(adjacentTE)
                        .refreshTileCacheOnSide(dir.getOpposite());
            }
        }
    }
}
