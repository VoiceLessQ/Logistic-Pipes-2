/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.item;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import logisticspipes.LogisticsPipes;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.IStore;
import network.rs485.logisticspipes.util.items.ItemStackLoader;

public class SimpleStackInventory implements Container, IStore, Iterable<Pair<ItemStack, Integer>> {

	private static final Component TEXT_COMPONENT_EMPTY = Component.literal("");

	private final NonNullList<ItemStack> stackList;
	private final String _name;
	private final int _stackLimit;

	private final LinkedList<ISimpleInventoryEventHandler> _listener = new LinkedList<>();

	public SimpleStackInventory(SimpleStackInventory copy) {
		this(copy.getContainerSize(), copy._name, copy._stackLimit);
		for (int i = 0; i < copy.getContainerSize(); i++) {
			stackList.set(i, copy.getItem(i).copy());
		}
	}

	public SimpleStackInventory(int size, String name, int stackLimit) {
		stackList = NonNullList.withSize(size, ItemStack.EMPTY);
		_name = name;
		_stackLimit = stackLimit;
	}

	@Override
	public int getContainerSize() {
		return stackList.size();
	}

	@Override
	public boolean isEmpty() {
		return stackList.stream().allMatch(ItemStack::isEmpty);
	}

	@Nonnull
	@Override
	public ItemStack getItem(int i) {
		return stackList.get(i);
	}

	@Nonnull
	@Override
	public ItemStack removeItem(int slot, int count) {
		final ItemStack stack = stackList.get(slot);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (stack.getCount() > count) {
			ItemStack ret = stack.copy();
			ret.setCount(count);
			stack.setCount(stack.getCount() - count);
			return ret;
		}
		return stackList.set(slot, ItemStack.EMPTY);
	}

	@Override
	public void setItem(int slot, @Nonnull ItemStack itemstack) {
		if (itemstack.isEmpty()) {
			stackList.set(slot, ItemStack.EMPTY);
		} else {
			stackList.set(slot, itemstack.copy());
		}
	}

	@Nonnull
	public String getName() {
		return _name;
	}

	@Nonnull
	public Component getDisplayName() {
		return TEXT_COMPONENT_EMPTY;
	}

	@Override
	public int getMaxStackSize() {
		return _stackLimit;
	}

	@Override
	public void setChanged() {
		for (ISimpleInventoryEventHandler handler : _listener) {
			handler.InventoryChanged(this);
		}
	}

	@Override
	public boolean stillValid(@Nonnull Player entityplayer) {
		return false;
	}

	@Override
	public void startOpen(@Nonnull Player player) {}

	@Override
	public void stopOpen(@Nonnull Player player) {}

	@Override
	public void readFromNBT(@Nonnull CompoundTag nbttagcompound) {
		readFromNBT(nbttagcompound, "");
	}

	public void readFromNBT(CompoundTag nbttagcompound, String prefix) {
		ListTag nbttaglist = nbttagcompound.getList(prefix + "items", nbttagcompound.getId());

		for (int j = 0; j < nbttaglist.size(); ++j) {
			CompoundTag nbttagcompound2 = nbttaglist.getCompound(j);
			int index = nbttagcompound2.getInt("index");
			if (index < stackList.size()) {
				stackList.set(index, ItemStackLoader.loadAndFixItemStackFromNBT(nbttagcompound2));
			} else {
				LogisticsPipes.log.error("SimpleInventory: java.lang.ArrayIndexOutOfBoundsException: " + index + " of " + stackList.size());
			}
		}
	}

	@Override
	public void writeToNBT(@Nonnull CompoundTag nbttagcompound) {
		writeToNBT(nbttagcompound, "");
	}

	public void writeToNBT(CompoundTag nbttagcompound, String prefix) {
		ListTag nbttaglist = new ListTag();
		for (int j = 0; j < stackList.size(); ++j) {
			final ItemStack stack = stackList.get(j);
			if (!stack.isEmpty()) {
				CompoundTag nbttagcompound2 = new CompoundTag();
				nbttaglist.add(nbttagcompound2);
				nbttagcompound2.putInt("index", j);
				stack.save(logisticspipes.utils.RegistryAccessUtil.registries(), nbttagcompound2);
			}
		}
		nbttagcompound.put(prefix + "items", nbttaglist);
		nbttagcompound.putInt(prefix + "itemsCount", stackList.size());
	}

	public void dropContents(Level world, BlockPos pos) {
		if (MainProxy.isServer(world)) {
			for (int i = 0; i < stackList.size(); i++) {
				dropSlot(i, world, pos);
			}
		}
	}

	private void dropSlot(int slot, Level world, BlockPos pos) {
		final ItemStack slotStack = stackList.get(slot);
		IntStream.range(0, (slotStack.getCount() / slotStack.getMaxStackSize()) + 1)
				.mapToObj(i -> removeItem(slot, slotStack.getMaxStackSize()))
				.filter(dropStack -> !dropStack.isEmpty())
				.forEach(dropStack -> {
					float f1 = 0.7F;
					double d = (world.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
					double d1 = (world.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
					double d2 = (world.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
					ItemEntity entityitem = new ItemEntity(world, pos.getX() + d, pos.getY() + d1, pos.getZ() + d2, dropStack);
					entityitem.setDefaultPickUpDelay();
					world.addFreshEntity(entityitem);
				});
	}

	public void addListener(ISimpleInventoryEventHandler listner) {
		if (!_listener.contains(listner)) {
			_listener.add(listner);
		}
	}

	public void removeListener(ISimpleInventoryEventHandler listner) {
		_listener.remove(listner);
	}

	@Nonnull
	@Override
	public ItemStack removeItemNoUpdate(int i) {
		return stackList.set(i, ItemStack.EMPTY);
	}

	private int tryAddToSlot(int i, @Nonnull ItemStack stack, int realstacklimit) {
		ItemStack slotStack = stackList.get(i);
		if (slotStack.isEmpty()) {
			final ItemStack copy = stack.copy();
			stackList.set(i, copy);
			copy.setCount(Math.min(copy.getCount(), realstacklimit));
			return copy.getCount();
		}
		ItemIdentifier stackIdent = ItemIdentifier.get(stack);
		ItemIdentifier slotIdent = ItemIdentifier.get(slotStack);
		if (slotIdent.equals(stackIdent)) {
			slotStack.setCount(slotStack.getCount() + stack.getCount());
			if (slotStack.getCount() > realstacklimit) {
				int ans = stack.getCount() - (slotStack.getCount() - realstacklimit);
				slotStack.setCount(realstacklimit);
				return ans;
			} else {
				return stack.getCount();
			}
		} else {
			return 0;
		}
	}

	public int addCompressed(@Nonnull ItemStack stack, boolean ignoreMaxStackSize) {
		if (stack.isEmpty()) return 0;
		stack = stack.copy();

		ItemIdentifier stackIdent = ItemIdentifier.get(stack);
		int stacklimit = _stackLimit;
		if (!ignoreMaxStackSize) {
			stacklimit = Math.min(stacklimit, stackIdent.getMaxStackSize());
		}

		for (int i = 0; i < stackList.size(); i++) {
			if (stack.getCount() <= 0) {
				break;
			}
			if (stackList.get(i).isEmpty()) {
				continue; //Skip Empty Slots on first attempt.
			}
			int added = tryAddToSlot(i, stack, stacklimit);
			stack.setCount(stack.getCount() - added);
		}
		for (int i = 0; i < stackList.size(); i++) {
			if (stack.getCount() <= 0) {
				break;
			}
			int added = tryAddToSlot(i, stack, stacklimit);
			stack.setCount(stack.getCount() - added);
		}
		setChanged();
		return stack.getCount();
	}

	@Override
	public boolean canPlaceItem(int i, @Nonnull ItemStack itemstack) {
		return true;
	}

	@kotlin.Deprecated(message = "not implemented")
	public int getField(int id) {
		return 0;
	}

	@kotlin.Deprecated(message = "not implemented")
	public void setField(int id, int value) {}

	@kotlin.Deprecated(message = "not implemented")
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clearContent() {
		Collections.fill(stackList, ItemStack.EMPTY);
	}

	public void clearInventorySlotContents(int i) {
		stackList.set(i, ItemStack.EMPTY);
	}

	public boolean hasCustomName() {
		return true;
	}

	@Nonnull
	@Override
	public Iterator<Pair<ItemStack, Integer>> iterator() {
		final Iterator<ItemStack> iter = stackList.iterator();
		return new Iterator<Pair<ItemStack, Integer>>() {

			int pos = -1;

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public Pair<ItemStack, Integer> next() {
				pos++;
				return new Pair<>(iter.next(), pos);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Returns a stream over all non-empty item stacks in this inventory.
	 */
	@Nonnull
	public Stream<ItemStack> stackStream() {
		return stackList.stream().filter(itemStack -> !itemStack.isEmpty());
	}

}
