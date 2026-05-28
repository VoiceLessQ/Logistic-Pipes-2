package com.Morph.logisticspipes.transport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.Morph.logisticspipes.config.LPConfig;
import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;
import com.Morph.logisticspipes.platform.PlatformHelper;
import com.Morph.logisticspipes.routing.ExitRoute;
import com.Morph.logisticspipes.routing.IRouter;
import com.Morph.logisticspipes.routing.RouterManager;

/**
 * Handles item movement through a pipe.
 * Ported from LP 1.12.2 PipeTransportLogistics — Phase 2 (dumb transport, no routing).
 */
public class PipeTransportLogistics {

    public final List<LPTravelingItem> items = new ArrayList<>();
    public LogisticsPipeBlockEntity container;

    public void setContainer(LogisticsPipeBlockEntity be) {
        this.container = be;
    }

    /** Called each server tick by the block entity. */
    public void updateEntity() {
        Level level = container.getLevel();
        if (level == null || level.isClientSide) return;

        Iterator<LPTravelingItem> it = items.iterator();
        List<LPTravelingItem> arrived = new ArrayList<>();

        while (it.hasNext()) {
            LPTravelingItem item = it.next();
            item.ticksInPipe--;
            if (item.ticksInPipe <= 0) {
                it.remove();
                arrived.add(item);
            }
        }

        for (LPTravelingItem item : arrived) {
            item.output = resolveRoutedOutput(item);
            if (item.output == null) {
                // Arrived at destination — deliver to adjacent inventory or drop
                deliverArrived(item);
            } else {
                passItemToNeighbor(item);
            }
        }
    }

    /**
     * Resolve routing direction for a routed item at this pipe.
     *
     * Power gate (LP1-style): each successful hop debits {@link LPConfig#POWER_ROUTING_COST}
     * LP from the network. If the network has the energy, it's consumed and we route normally.
     * If not, behaviour depends on {@link LPConfig#POWER_REQUIRE_FOR_ROUTING}:
     *   true  → return null (forces local delivery; effectively the item drops);
     *   false → route anyway for free (default — keeps gameplay functional pre-Power-Junction).
     */
    private Direction resolveRoutedOutput(LPTravelingItem item) {
        if (item.destinationRouterId < 0) return item.output;
        if (!(container.getPipe() instanceof CoreRoutedPipe crp)) return item.output;
        IRouter router = crp.getRouter();
        if (router == null) return item.output;
        // If we ARE the destination, deliver locally (output = null signals arrival)
        if (router.getSimpleID() == item.destinationRouterId) return null;
        ExitRoute exit = router.getExitFor(item.destinationRouterId, true, null);
        if (exit == null) return item.output;

        int cost = LPConfig.POWER_ROUTING_COST;
        if (cost > 0 && !LPConfig.POWER_USAGE_DISABLED) {
            boolean paid = crp.useEnergy(cost);
            if (!paid && LPConfig.POWER_REQUIRE_FOR_ROUTING) {
                return null; // out of power, strict mode → deliver locally (drops)
            }
            // paid==true OR strict mode is off: route normally
        }
        return exit.exitOrientation;
    }

    /**
     * Attempts to pass an item to the neighbor in item.output direction.
     * If the neighbor is another pipe, inject it. Otherwise insert into inventory.
     * If neither works, drop the item or buffer it.
     */
    private void passItemToNeighbor(LPTravelingItem item) {
        Level level = container.getLevel();
        BlockPos pos = container.getBlockPos();
        Direction dir = item.output;
        BlockPos neighborPos = pos.relative(dir);
        BlockEntity neighbor = level.getBlockEntity(neighborPos);

        if (neighbor instanceof LogisticsPipeBlockEntity neighborPipe) {
            // Pass into next pipe, coming from opposite direction
            neighborPipe.getTransport().injectItem(item.stack, dir.getOpposite());
        } else {
            // Try inserting into an inventory
            ItemStack remainder = PlatformHelper.get().insertItem(level, pos, dir, item.stack, false);
            if (!remainder.isEmpty()) {
                // Could not insert — put back in buffer (simplified: just drop)
                dropItem(remainder);
            }
        }
    }

    /**
     * Injects an item into this pipe traveling in the given input direction.
     * For Phase 2 (no routing): send it out the first connected side that isn't input.
     */
    public void injectItem(ItemStack stack, Direction inputSide) {
        if (stack.isEmpty()) return;
        Direction output = findOutputSide(inputSide);
        if (output == null) {
            // Dead end — try inserting into any adjacent inventory
            Level level = container.getLevel();
            BlockPos pos = container.getBlockPos();
            for (Direction dir : Direction.values()) {
                if (dir == inputSide) continue;
                if (container.isConnected(dir)) {
                    ItemStack remainder = PlatformHelper.get().insertItem(level, pos, dir, stack, false);
                    if (remainder.isEmpty()) return;
                }
            }
            dropItem(stack);
            return;
        }
        items.add(new LPTravelingItem(stack, output));
        container.setChanged();
    }

    /** Find the first connected output side that isn't the input. */
    private Direction findOutputSide(Direction inputSide) {
        for (Direction dir : Direction.values()) {
            if (dir == inputSide) continue;
            if (container.isConnected(dir)) return dir;
        }
        return null;
    }

    /** Insert arrived item into any adjacent inventory, or drop it. */
    private void deliverArrived(LPTravelingItem item) {
        Level level = container.getLevel();
        BlockPos pos = container.getBlockPos();
        for (Direction dir : Direction.values()) {
            if (!container.isConnected(dir)) continue;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor instanceof LogisticsPipeBlockEntity) continue; // don't deliver into pipes
            ItemStack remainder = PlatformHelper.get().insertItem(level, pos, dir, item.stack, false);
            if (remainder.isEmpty()) return;
            item.stack = remainder;
        }
        dropItem(item.stack);
    }

    private void dropItem(ItemStack stack) {
        Level level = container.getLevel();
        BlockPos pos = container.getBlockPos();
        net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(
                level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        level.addFreshEntity(entity);
    }

    public void onBlockPlaced() {}

    public void onNeighborBlockChange() {}

    public boolean canPipeConnect(BlockEntity neighbor, Direction side) {
        // Connect to other LP pipes always; connect to inventories if they expose one
        if (neighbor instanceof LogisticsPipeBlockEntity) return true;
        // Check if neighbor has an item handler (via platform)
        Level level = container.getLevel();
        if (level == null) return false;
        ItemStack probe = new ItemStack(net.minecraft.world.item.Items.STICK);
        ItemStack result = PlatformHelper.get().insertItem(level, container.getBlockPos(), side, probe, true);
        return result.isEmpty(); // accepted the probe → has inventory
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (LPTravelingItem item : items) {
            list.add(item.toNBT());
        }
        tag.put("traveling_items", list);
    }

    public void load(CompoundTag tag) {
        items.clear();
        ListTag list = tag.getList("traveling_items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            items.add(LPTravelingItem.fromNBT(list.getCompound(i)));
        }
    }
}
