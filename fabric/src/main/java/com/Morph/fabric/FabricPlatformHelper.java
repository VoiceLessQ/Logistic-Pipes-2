package com.Morph.fabric;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.Morph.logisticspipes.platform.IPlatformHelper;

public final class FabricPlatformHelper implements IPlatformHelper {

    public static final FabricPlatformHelper INSTANCE = new FabricPlatformHelper();

    private FabricPlatformHelper() {}

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public ItemStack insertItem(Level level, BlockPos pos, Direction side, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos.relative(side), side.getOpposite());
        if (storage == null) return stack;
        ItemVariant variant = ItemVariant.of(stack);
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = storage.insert(variant, stack.getCount(), tx);
            if (!simulate) tx.commit();
            if (inserted <= 0) return stack;
            if (inserted >= stack.getCount()) return ItemStack.EMPTY;
            ItemStack remainder = stack.copy();
            remainder.setCount((int) (stack.getCount() - inserted));
            return remainder;
        }
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public Map<Item, Integer> getInventoryContents(Level level, BlockPos pos, Direction side) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos.relative(side), side.getOpposite());
        if (storage == null) return Map.of();
        Map<Item, Integer> result = new HashMap<>();
        for (StorageView<ItemVariant> view : storage) {
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            result.merge(view.getResource().getItem(), (int) view.getAmount(), Integer::sum);
        }
        return result;
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public ItemStack extractItem(Level level, BlockPos pos, Direction side, ItemStack template, int maxCount, boolean simulate) {
        if (template.isEmpty() || maxCount <= 0) return ItemStack.EMPTY;
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos.relative(side), side.getOpposite());
        if (storage == null) return ItemStack.EMPTY;
        ItemVariant variant = ItemVariant.of(template);
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = storage.extract(variant, maxCount, tx);
            if (extracted <= 0) return ItemStack.EMPTY;
            if (!simulate) tx.commit();
            ItemStack result = template.copy();
            result.setCount((int) extracted);
            return result;
        }
    }
}
