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

import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;
import com.Morph.logisticspipes.platform.PlatformHelper;

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
            passItemToNeighbor(item);
        }
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
