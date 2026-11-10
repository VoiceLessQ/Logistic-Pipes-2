package logisticspipes.utils;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/**
 * Forge IItemHandler's slot view over a Transfer API item storage, so LP's slot based inventory code
 * keeps working. Non-slotted storages expose their views as slots and take insertions through slot 0.
 */
public class StorageItemHandler {

	private final Storage<ItemVariant> storage;

	public StorageItemHandler(Storage<ItemVariant> storage) {
		this.storage = storage;
	}

	/** Forge's {@code getCapability(ITEM_HANDLER, side)}: null when the block exposes no item storage on that side. */
	@Nullable
	public static StorageItemHandler find(@Nullable BlockEntity tile, @Nullable Direction side) {
		if (tile == null || tile.getLevel() == null) return null;
		BlockPos pos = tile.getBlockPos();
		Storage<ItemVariant> storage = ItemStorage.SIDED.find(tile.getLevel(), pos, tile.getBlockState(), tile, side);
		return storage == null ? null : new StorageItemHandler(storage);
	}

	private List<StorageView<ItemVariant>> views() {
		List<StorageView<ItemVariant>> views = new ArrayList<>();
		storage.forEach(views::add);
		return views;
	}

	@Nullable
	private StorageView<ItemVariant> view(int slot) {
		if (storage instanceof SlottedStorage<ItemVariant> slotted) {
			return slot < slotted.getSlotCount() ? slotted.getSlot(slot) : null;
		}
		List<StorageView<ItemVariant>> views = views();
		return slot < views.size() ? views.get(slot) : null;
	}

	public int getSlots() {
		if (storage instanceof SlottedStorage<ItemVariant> slotted) {
			return slotted.getSlotCount();
		}
		return Math.max(1, views().size());
	}

	@Nonnull
	public ItemStack getStackInSlot(int slot) {
		StorageView<ItemVariant> view = view(slot);
		if (view == null || view.isResourceBlank() || view.getAmount() <= 0) return ItemStack.EMPTY;
		return view.getResource().toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
	}

	public int getSlotLimit(int slot) {
		StorageView<ItemVariant> view = view(slot);
		if (view == null) return storage instanceof SlottedStorage ? 0 : Integer.MAX_VALUE;
		return (int) Math.min(view.getCapacity(), Integer.MAX_VALUE);
	}

	/** @return the part of {@code stack} that did not fit */
	@Nonnull
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		if (stack.isEmpty()) return ItemStack.EMPTY;
		ItemVariant variant = ItemVariant.of(stack);
		try (Transaction tx = Transaction.openOuter()) {
			long inserted;
			if (storage instanceof SlottedStorage<ItemVariant> slotted) {
				inserted = slot < slotted.getSlotCount() ? slotted.getSlot(slot).insert(variant, stack.getCount(), tx) : 0;
			} else {
				inserted = slot == 0 ? storage.insert(variant, stack.getCount(), tx) : 0;
			}
			if (!simulate) tx.commit();
			return stack.copyWithCount(stack.getCount() - (int) inserted);
		}
	}

	@Nonnull
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		StorageView<ItemVariant> view = view(slot);
		if (view == null || view.isResourceBlank() || amount <= 0) return ItemStack.EMPTY;
		ItemVariant variant = view.getResource();
		try (Transaction tx = Transaction.openOuter()) {
			long extracted = view.extract(variant, amount, tx);
			if (!simulate) tx.commit();
			return extracted <= 0 ? ItemStack.EMPTY : variant.toStack((int) extracted);
		}
	}
}
