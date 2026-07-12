/*
 * Copyright (c) Krapht, 2011
 * <p>
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.item;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import logisticspipes.LogisticsPipes;
import network.rs485.logisticspipes.IStore;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.inventory.SlotAccess;
import network.rs485.logisticspipes.util.items.ItemStackLoader;

public class ItemIdentifierInventory
		implements IStore, Iterable<Pair<ItemIdentifierStack, Integer>>, IItemIdentifierInventory {

	private final Object[] ccTypeHolder = new Object[1];
	private final ItemIdentifierStack[] _contents;
	private final String _name;
	private final int _stackLimit;
	@Nonnull
	private final HashMap<ItemIdentifier, Integer> _contentsMap;
	private final HashSet<ItemIdentifier> _contentsUndamagedSet;
	private final HashSet<ItemIdentifier> _contentsNoNBTSet;
	private final HashSet<ItemIdentifier> _contentsUndamagedNoNBTSet;
	private final boolean isLiquidInventory;

	private final LinkedList<ISimpleInventoryEventHandler> _listener = new LinkedList<>();

	public final SlotAccess slotAccess = new SlotAccess() {

		@Override
		public void mergeSlots(int intoSlot, int fromSlot) {
			if (_contents[intoSlot] == null) {
				_contents[intoSlot] = _contents[fromSlot];
			} else {
				_contents[intoSlot].setStackSize(_contents[intoSlot].getStackSize() + _contents[fromSlot].getStackSize());
			}
			_contents[fromSlot] = null;
			updateContents();
		}

		@Override
		public boolean canMerge(int intoSlot, int fromSlot) {
			return _contents[intoSlot].getItem().equals(_contents[fromSlot].getItem());
		}

		@Override
		public boolean isSlotEmpty(int idx) {
			return _contents[idx] == null;
		}

	};

	public ItemIdentifierInventory(int size, String name, int stackLimit, boolean liquidInv) {
		_contents = new ItemIdentifierStack[size];
		_name = name;
		_stackLimit = stackLimit;
		_contentsMap = new HashMap<>((int) (size * 1.5));
		_contentsUndamagedSet = new HashSet<>((int) (size * 1.5));
		_contentsNoNBTSet = new HashSet<>((int) (size * 1.5));
		_contentsUndamagedNoNBTSet = new HashSet<>((int) (size * 1.5));
		isLiquidInventory = liquidInv;
	}

	public ItemIdentifierInventory(int size, String name, int stackLimit) {
		this(size, name, stackLimit, false);
	}

	public ItemIdentifierInventory(@Nonnull ItemIdentifierInventory copy) {
		_contents = Arrays.copyOf(copy._contents, copy._contents.length);
		for (int i = 0; i < _contents.length; i++) {
			if (copy._contents[i] != null) _contents[i] = new ItemIdentifierStack(copy._contents[i]);
		}
		_name = copy._name;
		_stackLimit = copy._stackLimit;
		_contentsMap = new HashMap<>(copy._contentsMap);
		_contentsUndamagedSet = new HashSet<>(copy._contentsUndamagedSet);
		_contentsNoNBTSet = new HashSet<>(copy._contentsNoNBTSet);
		_contentsUndamagedNoNBTSet = new HashSet<>(copy._contentsUndamagedNoNBTSet);
		isLiquidInventory = copy.isLiquidInventory;
	}

	public static void dropItems(Level world, @Nonnull ItemStack stack, BlockPos pos) {
		dropItems(world, stack, pos.getX(), pos.getY(), pos.getZ());
	}

	public static void dropItems(Level world, @Nonnull ItemStack stack, int i, int j, int k) {
		if (stack.isEmpty()) return;
		float f1 = 0.7F;
		double d = (world.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
		double d1 = (world.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
		double d2 = (world.getRandom().nextFloat() * f1) + (1.0F - f1) * 0.5D;
		ItemEntity entityitem = new ItemEntity(world, i + d, j + d1, k + d2, stack);
		entityitem.setPickUpDelay(10);
		world.addFreshEntity(entityitem);
	}

	@Override
	public int getContainerSize() {
		return _contents.length;
	}

	@Override
	@Deprecated
	@Nonnull
	public ItemStack getItem(int i) {
		if (_contents[i] == null) {
			return ItemStack.EMPTY;
		}
		return _contents[i].makeNormalStack();
	}

	@Override
	public ItemIdentifierStack getIDStackInSlot(int i) {
		return _contents[i];
	}

	@Override
	@Nonnull
	public ItemStack removeItem(int slot, int count) {
		if (_contents[slot] == null) {
			return ItemStack.EMPTY;
		}
		ItemStack ret = _contents[slot].makeNormalStack();
		if (_contents[slot].getStackSize() > count) {
			ret.setCount(count);
			_contents[slot].setStackSize(_contents[slot].getStackSize() - count);
		} else {
			_contents[slot] = null;
		}
		updateContents();
		return ret;
	}

	@Override
	public void setItem(int i, @Nonnull ItemStack itemstack) {
		if (itemstack.isEmpty()) {
			_contents[i] = null;
		} else {
			if (isInvalidStack(itemstack)) {
				if (LogisticsPipes.isDEBUG()) {
					new UnsupportedOperationException("Not valid for this Inventory: (" + itemstack + ")")
							.printStackTrace();
				}
				return;
			}
			_contents[i] = ItemIdentifierStack.getFromStack(itemstack);
		}
		updateContents();
	}

	@Override
	public void setItem(int i, ItemIdentifierStack itemstack) {
		if (itemstack == null) {
			_contents[i] = null;
		} else {
			if (!isValidStack(itemstack)) {
				if (LogisticsPipes.isDEBUG()) {
					new UnsupportedOperationException("Not valid for this Inventory: (" + itemstack + ")")
							.printStackTrace();
				}
				return;
			}
			_contents[i] = itemstack;
		}
		updateContents();
	}

	@Override
	public int getMaxStackSize() {
		return _stackLimit;
	}

	@Override
	public void setChanged() {
		updateContents();
		for (ISimpleInventoryEventHandler handler : _listener) {
			handler.InventoryChanged(this);
		}
	}

	@Override
	public boolean stillValid(@Nonnull Player entityplayer) {
		return true;
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

		Arrays.fill(_contents, null);
		for (int j = 0; j < nbttaglist.size(); ++j) {
			CompoundTag nbttagcompound2 = nbttaglist.getCompound(j);
			int index = nbttagcompound2.getInt("index");
			if (index < _contents.length) {
				ItemStack stack = ItemStackLoader.loadAndFixItemStackFromNBT(nbttagcompound2);
				ItemIdentifierStack itemstack = ItemIdentifierStack.getFromStack(stack);
				if (isValidStack(itemstack)) {
					_contents[index] = itemstack;
				}
			} else {
				LogisticsPipes.log.error("SimpleInventory: java.lang.ArrayIndexOutOfBoundsException: " + index + " of "
						+ _contents.length);
			}
		}
		updateContents();
	}

	@Override
	public void writeToNBT(@Nonnull CompoundTag nbttagcompound) {
		writeToNBT(nbttagcompound, "");
	}

	public void writeToNBT(CompoundTag nbttagcompound, String prefix) {
		ListTag nbttaglist = new ListTag();
		for (int j = 0; j < _contents.length; ++j) {
			if (_contents[j] != null && _contents[j].getStackSize() > 0) {
				CompoundTag nbttagcompound2 = new CompoundTag();
				nbttaglist.add(nbttagcompound2);
				nbttagcompound2.putInt("index", j);
				_contents[j].unsafeMakeNormalStack().save(logisticspipes.utils.RegistryAccessUtil.registries(), nbttagcompound2);
			}
		}
		nbttagcompound.put(prefix + "items", nbttaglist);
		nbttagcompound.putInt(prefix + "itemsCount", _contents.length);
	}

	public void dropContents(Level world, BlockPos pos) {
		dropContents(world, pos.getX(), pos.getY(), pos.getZ());
	}

	public void dropContents(Level world, int posX, int posY, int posZ) {
		if (MainProxy.isServer(world)) {
			for (int i = 0; i < _contents.length; i++) {
				while (_contents[i] != null) {
					ItemStack todrop = removeItem(i, _contents[i].getItem().getMaxStackSize());
					ItemIdentifierInventory.dropItems(world, todrop, posX, posY, posZ);
				}
			}
			updateContents();
		}
	}

	@Override
	public void addListener(@Nonnull ISimpleInventoryEventHandler listener) {
		if (!_listener.contains(listener)) {
			_listener.add(listener);
		}
	}

	@Override
	public void removeListener(@Nonnull ISimpleInventoryEventHandler listener) {
		_listener.remove(listener);
	}

	@Nonnull
	@Override
	public ItemStack removeItemNoUpdate(int i) {
		if (_contents[i] == null) {
			return ItemStack.EMPTY;
		}

		ItemStack stackToTake = _contents[i].makeNormalStack();
		_contents[i] = null;
		updateContents();
		return stackToTake;
	}

	@Override
	public void handleItemIdentifierList(Collection<ItemIdentifierStack> _allItems) {
		int i = 0;
		for (ItemIdentifierStack stack : _allItems) {
			if (_contents.length <= i) {
				break;
			}
			_contents[i] = stack;
			i++;
		}
		setChanged();
	}

	private int tryAddToSlot(int i, @Nonnull ItemStack stack, int realstacklimit) {
		if (isInvalidStack(stack)) {
			if (LogisticsPipes.isDEBUG()) {
				new UnsupportedOperationException("Not valid for this Inventory: (" + stack + ")").printStackTrace();
			}
			return 0;
		}
		ItemIdentifierStack slot = _contents[i];

		if (slot == null) {
			_contents[i] = ItemIdentifierStack.getFromStack(stack);
			_contents[i].setStackSize(Math.min(_contents[i].getStackSize(), realstacklimit));
			return _contents[i].getStackSize();
		}

		ItemIdentifier stackIdent = ItemIdentifier.get(stack);
		ItemIdentifier slotIdent = slot.getItem();

		if (slotIdent.equals(stackIdent)) {
			slot.setStackSize(slot.getStackSize() + stack.getCount());

			if (slot.getStackSize() > realstacklimit) {
				int ans = stack.getCount() - (slot.getStackSize() - realstacklimit);
				slot.setStackSize(realstacklimit);
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

		if (isInvalidStack(stack)) {
			if (LogisticsPipes.isDEBUG()) {
				new UnsupportedOperationException("Not valid for this Inventory: (" + stack + ")").printStackTrace();
			}
			return stack.getCount();
		}

		stack = stack.copy();

		ItemIdentifier stackIdent = ItemIdentifier.get(stack);
		int stacklimit = _stackLimit;

		if (!ignoreMaxStackSize) {
			stacklimit = Math.min(stacklimit, stackIdent.getMaxStackSize());
		}

		for (int i = 0; i < _contents.length; i++) {
			if (stack.getCount() <= 0) break;
			if (_contents[i] == null) continue; //Skip Empty Slots on first attempt.

			int added = tryAddToSlot(i, stack, stacklimit);
			stack.setCount(stack.getCount() - added);
		}

		for (int i = 0; i < _contents.length; i++) {
			if (stack.getCount() <= 0) break;

			int added = tryAddToSlot(i, stack, stacklimit);
			stack.setCount(stack.getCount() - added);
		}

		setChanged();
		return stack.getCount();
	}

	/* InventoryUtil-like functions */

	private void updateContents() {
		_contentsMap.clear();
		_contentsUndamagedSet.clear();
		_contentsNoNBTSet.clear();
		_contentsUndamagedNoNBTSet.clear();
		for (ItemIdentifierStack _content : _contents) {
			if (_content == null) continue;

			ItemIdentifier itemId = _content.getItem();
			_contentsMap.merge(itemId, _content.getStackSize(), Integer::sum);
			_contentsUndamagedSet.add(itemId
					.getUndamaged()); // add is cheaper than check then add; it just returns false if it is already there
			_contentsNoNBTSet.add(itemId
					.getIgnoringNBT()); // add is cheaper than check then add; it just returns false if it is already there
			_contentsUndamagedNoNBTSet.add(itemId.getIgnoringNBT()
					.getUndamaged()); // add is cheaper than check then add; it just returns false if it is already there
		}
	}

	@Override
	public int itemCount(@Nonnull final ItemIdentifier item) {
		return _contentsMap.getOrDefault(item, 0);
	}

	@Override
	@Nonnull
	public Map<ItemIdentifier, Integer> getItemsAndCount() {
		return _contentsMap;
	}

	@Override
	public boolean containsItem(final ItemIdentifier item) {
		return _contentsMap.containsKey(item);
	}

	@Override
	public boolean containsUndamagedItem(@Nonnull final ItemIdentifier item) {
		return _contentsUndamagedSet.contains(item);
	}

	@Override
	public boolean containsExcludeNBTItem(@Nonnull final ItemIdentifier item) {
		return _contentsNoNBTSet.contains(item);
	}

	@Override
	public boolean containsUndamagedExcludeNBTItem(@Nonnull final ItemIdentifier item) {
		return _contentsUndamagedNoNBTSet.contains(item);
	}

	@Override
	public boolean isEmpty() {
		return _contentsMap.isEmpty();
	}

	@Override
	public boolean canPlaceItem(int i, @Nonnull ItemStack itemstack) {
		return true;
	}

	public int getField(int id) {
		return 0;
	}

	public void setField(int id, int value) {}

	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clearContent() {
		clear();
	}

	public void clear() {
		Arrays.fill(_contents, null);
		updateContents();
	}

	@Override
	public void clearInventorySlotContents(int i) {
		_contents[i] = null;
		updateContents();
	}

	@Override
	public void recheckStackLimit() {
		for (ItemIdentifierStack _content : _contents) {
			if (_content != null) {
				_content.setStackSize(Math.min(_content.getStackSize(), _stackLimit));
			}
		}
	}

	private boolean isInvalidStack(@Nonnull ItemStack stack) {
		if (isLiquidInventory && !stack.isEmpty()) {
			return FluidIdentifier.get(stack) == null;
		}
		return false;
	}

	private boolean isValidStack(ItemIdentifierStack stack) {
		if (stack == null) return true;
		if (isLiquidInventory) {
			return FluidIdentifier.get(stack.getItem()) != null;
		}
		return true;
	}

	@Override
	@Nonnull
	public Iterator<Pair<ItemIdentifierStack, Integer>> iterator() {
		final Iterator<ItemIdentifierStack> iter = Arrays.asList(_contents).iterator();
		return new Iterator<Pair<ItemIdentifierStack, Integer>>() {

			int pos = -1;

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public Pair<ItemIdentifierStack, Integer> next() {
				pos++;
				return new Pair<>(iter.next(), pos);
			}
		};
	}

	public void clearGrid() {
		for (int i = 0; i < getContainerSize(); i++) {
			_contents[i] = null;
		}
		updateContents();
	}

	@Nonnull
	public String getName() {
		return _name;
	}

	public boolean hasCustomName() {
		return true;
	}

	@Nonnull
	public Component getDisplayName() {
		return Component.literal(getName());
	}

	public NonNullList<ItemStack> toNonNullList() {
		NonNullList<ItemStack> list = NonNullList.create();
		list.addAll(0, Arrays.stream(_contents)
				.filter(Objects::nonNull)
				.map(ItemIdentifierStack::makeNormalStack)
				.collect(Collectors.toList()));
		return list;
	}

	@Override
	public @Nonnull
	List<String> getClientInformation() {
		return Arrays.stream(_contents).filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
	}

	@Override
	public Object[] getTypeHolder() {
		return ccTypeHolder;
	}

	@Nonnull
	@Override
	public Iterable<Pair<ItemIdentifierStack, Integer>> contents() {
		return this;
	}

	@Nonnull
	@Override
	public SlotAccess getSlotAccess() {
		return slotAccess;
	}

}
