package com.Morph.logisticspipes.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
}
