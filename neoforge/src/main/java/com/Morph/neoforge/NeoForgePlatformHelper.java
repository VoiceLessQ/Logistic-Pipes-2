package com.Morph.neoforge;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import com.Morph.logisticspipes.platform.IPlatformHelper;

public final class NeoForgePlatformHelper implements IPlatformHelper {

    public static final NeoForgePlatformHelper INSTANCE = new NeoForgePlatformHelper();

    private NeoForgePlatformHelper() {}

    @Override
    public ItemStack insertItem(Level level, BlockPos pos, Direction side, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(side), side.getOpposite());
        if (handler == null) return stack;
        return ItemHandlerHelper.insertItemStacked(handler, stack.copy(), simulate);
    }

    @Override
    public Map<Item, Integer> getInventoryContents(Level level, BlockPos pos, Direction side) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(side), side.getOpposite());
        if (handler == null) return Map.of();
        Map<Item, Integer> result = new HashMap<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                result.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return result;
    }

    @Override
    public ItemStack extractItem(Level level, BlockPos pos, Direction side, ItemStack template, int maxCount, boolean simulate) {
        if (template.isEmpty() || maxCount <= 0) return ItemStack.EMPTY;
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(side), side.getOpposite());
        if (handler == null) return ItemStack.EMPTY;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, template)) {
                return handler.extractItem(slot, maxCount, simulate);
            }
        }
        return ItemStack.EMPTY;
    }
}
