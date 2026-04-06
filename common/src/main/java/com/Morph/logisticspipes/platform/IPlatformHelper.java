package com.Morph.logisticspipes.platform;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IPlatformHelper {

    ItemStack insertItem(Level level, BlockPos pos, Direction side, ItemStack stack, boolean simulate);

    default ItemStack insertItem(Level level, BlockPos pos, Direction side, ItemStack stack) {
        return insertItem(level, pos, side, stack, false);
    }

    ItemStack extractItem(Level level, BlockPos pos, Direction side, ItemStack template, int maxCount, boolean simulate);

    default ItemStack extractItem(Level level, BlockPos pos, Direction side, ItemStack template, int maxCount) {
        return extractItem(level, pos, side, template, maxCount, false);
    }

    /**
     * Returns all items available in the inventory adjacent to pos on the given side,
     * aggregated by item type. Used by provider pipes to advertise their contents.
     */
    Map<Item, Integer> getInventoryContents(Level level, BlockPos pos, Direction side);
}
