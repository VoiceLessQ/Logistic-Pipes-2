/*
 * Copyright (c) Krapht, 2011
 * <p>
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.item;

import net.minecraft.core.registries.BuiltInRegistries;

import logisticspipes.LogisticsPipes;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.level.block.Block;
// net.minecraft.world.item.CreativeModeTab removed — use CreativeModeTab
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import net.neoforged.fml.ModList;

import lombok.AllArgsConstructor;

import logisticspipes.asm.addinfo.IAddInfo;
import logisticspipes.asm.addinfo.IAddInfoProvider;
import logisticspipes.items.LogisticsFluidContainer;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.utils.FinalCompoundTag;
import logisticspipes.utils.ReflectionHelper;

/**
 * @author Krapht I have no bloody clue what different mods use to differate
 * between items except for itemID, there is metadata, damage, and
 * whatnot. so..... to avoid having to change all my bloody code every
 * time I need to support a new item targeted that would make it a
 * "different" item, I made this cache here A ItemIdentifier is
 * immutable, singleton and most importantly UNIQUE!
 */
public final class ItemIdentifier implements Comparable<ItemIdentifier>, ILPCCTypeHolder {

	//a key to look up a ItemIdentifier by Item:damage:tag
	private static class ItemKey {

		public final Item item;
		public final int itemDamage;
		public final FinalCompoundTag tag;

		public ItemKey(Item i, int d, FinalCompoundTag t) {
			item = i;
			itemDamage = d;
			tag = t;
		}

		@Override
		public boolean equals(Object that) {
			if (!(that instanceof ItemKey)) {
				return false;
			}
			ItemKey i = (ItemKey) that;
			return item == i.item && itemDamage == i.itemDamage && tag.equals(i.tag);
		}

		@Override
		public int hashCode() {
			return item.hashCode() ^ itemDamage ^ tag.hashCode();
		}
	}

	//remember itemId/damage/tag so we can find GCed ItemIdentifiers
	private static class IDReference extends WeakReference<ItemIdentifier> {

		private final ItemKey key;
		private final int uniqueID;

		IDReference(ItemKey k, int u, ItemIdentifier id) {
			super(id, ItemIdentifier.keyRefQueue);
			key = k;
			uniqueID = u;
		}
	}

	private interface IDamagedIdentifierHolder {

		ItemIdentifier get(int damage);

		void set(int damage, ItemIdentifier ret);

		void ensureCapacity(int damage);
	}

	private static class MapDamagedItentifierHolder implements IDamagedIdentifierHolder {

		private final ConcurrentHashMap<Integer, ItemIdentifier> holder;

		public MapDamagedItentifierHolder() {
			holder = new ConcurrentHashMap<>(4096, 0.5f, 1);
		}

		@Override
		public ItemIdentifier get(int damage) {
			return holder.get(damage);
		}

		@Override
		public void set(int damage, ItemIdentifier item) {
			holder.put(damage, item);
		}

		@Override
		public void ensureCapacity(int damage) {}
	}

	private static class ArrayDamagedItentifierHolder implements IDamagedIdentifierHolder {

		private AtomicReferenceArray<ItemIdentifier> holder;

		public ArrayDamagedItentifierHolder(int damage) {
			//round to nearest superior power of 2
			int newlen = 1 << (32 - Integer.numberOfLeadingZeros(damage));
			holder = new AtomicReferenceArray<>(newlen);
		}

		@Override
		public ItemIdentifier get(int damage) {
			return holder.get(damage);
		}

		@Override
		public void set(int damage, ItemIdentifier ident) {
			holder.set(damage, ident);
		}

		@Override
		public void ensureCapacity(int damage) {
			if (holder.length() <= damage) {
				int newlen = 1 << (32 - Integer.numberOfLeadingZeros(damage));
				AtomicReferenceArray<ItemIdentifier> newdamages = new AtomicReferenceArray<>(newlen);
				for (int i = 0; i < holder.length(); i++) {
					newdamages.set(i, holder.get(i));
				}
				holder = newdamages;
			}
		}
	}

	private final Object[] ccTypeHolder = new Object[1];

	//array of ItemIdentifiers for damage=0,tag=null items
	private final static ConcurrentHashMap<Item, ItemIdentifier> simpleIdentifiers = new ConcurrentHashMap<>(4096, 0.5f, 1);

	//array of arrays for items with damage>0 and tag==null
	private final static ConcurrentHashMap<Item, IDamagedIdentifierHolder> damageIdentifiers = new ConcurrentHashMap<>(4096, 0.5f, 1);

	//map for id+damage+tag -> ItemIdentifier lookup
	private final static HashMap<ItemKey, IDReference> keyRefMap = new HashMap<>(1024, 0.5f);
	//for tracking the tagUniqueIDs in use for a given Item
	private final static HashMap<Item, BitSet> tagIDsets = new HashMap<>(1024, 0.5f);
	//a referenceQueue to collect GCed identifier refs
	private final static ReferenceQueue<ItemIdentifier> keyRefQueue = new ReferenceQueue<>();
	//and locks to protect these
	private final static ReadWriteLock keyRefLock = new ReentrantReadWriteLock();
	private final static Lock keyRefRlock = ItemIdentifier.keyRefLock.readLock();
	private final static Lock keyRefWlock = ItemIdentifier.keyRefLock.writeLock();

	//helper thread to clean up references to GCed ItemIdentifiers
	private static final class ItemIdentifierCleanupThread extends Thread {

		public ItemIdentifierCleanupThread() {
			setName("LogisticsPipes ItemIdentifier Cleanup Thread");
			setDaemon(true);
			start();
		}

		@Override
		public void run() {
			while (true) {
				IDReference r;
				try {
					r = (IDReference) (ItemIdentifier.keyRefQueue.remove());
				} catch (InterruptedException e) {
					continue;
				}
				ItemIdentifier.keyRefWlock.lock();
				do {
					//value in the map might have been replaced in the meantime
					IDReference current = ItemIdentifier.keyRefMap.get(r.key);
					if (r == current) {
						ItemIdentifier.keyRefMap.remove(r.key);
						ItemIdentifier.tagIDsets.get(r.key.item).clear(r.uniqueID);
					}
					r = (IDReference) (ItemIdentifier.keyRefQueue.poll());
				} while (r != null);
				ItemIdentifier.keyRefWlock.unlock();
			}
		}
	}

	private static final ItemIdentifierCleanupThread cleanupThread = new ItemIdentifierCleanupThread();

	//Hide default constructor
	private ItemIdentifier(Item item, int itemDamage, FinalCompoundTag tag, int uniqueID) {
		this.item = item;
		this.itemDamage = itemDamage;
		this.tag = tag;
		this.uniqueID = uniqueID;
	}

	public final Item item;
	public final int itemDamage;
	public final FinalCompoundTag tag;
	public final int uniqueID;

	private int maxStackSize = 0;

	private ItemIdentifier _IDIgnoringNBT = null;
	private ItemIdentifier _IDIgnoringDamage = null;
	private ItemIdentifier _IDIgnoringData = null;
	private DictItemIdentifier _dict;
	private boolean canHaveDict = true;
	private String modName;
	private String creativeTabName;

	private static ItemIdentifier getOrCreateSimple(Item item, ItemIdentifier proposal) {
		if (proposal != null) {
			if (proposal.item == item && proposal.itemDamage == 0 && proposal.tag == null) {
				return proposal;
			}
		}
		//no locking here. if 2 threads race and create the same ItemIdentifier, they end up .equal() and one of them ends up in the map
		ItemIdentifier ret = ItemIdentifier.simpleIdentifiers.get(item);
		if (ret != null) {
			return ret;
		}
		ret = new ItemIdentifier(item, 0, null, 0);
		ItemIdentifier.simpleIdentifiers.put(item, ret);
		return ret;
	}

	private static ItemIdentifier getOrCreateDamage(Item item, int damage, ItemIdentifier proposal) {
		if (proposal != null) {
			if (proposal.item == item && proposal.itemDamage == damage && proposal.tag == null) {
				return proposal;
			}
		}
		//again no locking, we can end up removing or overwriting ItemIdentifiers concurrently added by another thread, but that doesn't affect anything.
		IDamagedIdentifierHolder damages = ItemIdentifier.damageIdentifiers.get(item);
		if (damages == null) {
			if (item.getDefaultInstance().getMaxDamage() < 32767) {
				damages = new ArrayDamagedItentifierHolder(damage);
			} else {
				damages = new MapDamagedItentifierHolder();
			}
			ItemIdentifier.damageIdentifiers.put(item, damages);
		} else {
			damages.ensureCapacity(damage);
		}
		ItemIdentifier ret = damages.get(damage);
		if (ret != null) {
			return ret;
		}
		ret = new ItemIdentifier(item, damage, null, 0);
		damages.set(damage, ret);
		return ret;
	}

	private static ItemIdentifier getOrCreateTag(Item item, int damage, FinalCompoundTag tag) {
		ItemKey k = new ItemKey(item, damage, tag);
		ItemIdentifier.keyRefRlock.lock();
		IDReference r = ItemIdentifier.keyRefMap.get(k);
		if (r != null) {
			ItemIdentifier ret = r.get();
			if (ret != null) {
				ItemIdentifier.keyRefRlock.unlock();
				return ret;
			}
		}
		ItemIdentifier.keyRefRlock.unlock();
		ItemIdentifier.keyRefWlock.lock();
		r = ItemIdentifier.keyRefMap.get(k);
		if (r != null) {
			ItemIdentifier ret = r.get();
			if (ret != null) {
				ItemIdentifier.keyRefWlock.unlock();
				return ret;
			}
		}
		if (ItemIdentifier.tagIDsets.get(item) == null) {
			ItemIdentifier.tagIDsets.put(item, new BitSet(16));
		}
		int nextUniqueID;
		if (r == null) {
			nextUniqueID = ItemIdentifier.tagIDsets.get(item).nextClearBit(1);
			ItemIdentifier.tagIDsets.get(item).set(nextUniqueID);
		} else {
			nextUniqueID = r.uniqueID;
		}
		FinalCompoundTag finaltag = new FinalCompoundTag(tag);
		ItemKey realKey = new ItemKey(item, damage, finaltag);
		ItemIdentifier ret = new ItemIdentifier(item, damage, finaltag, nextUniqueID);
		ItemIdentifier.keyRefMap.put(realKey, new IDReference(realKey, nextUniqueID, ret));
		ItemIdentifier.keyRefWlock.unlock();
		return ret;
	}

	public static ItemIdentifier get(Item item, int itemUndamagableDamage, CompoundTag tag) {
		return get(item, itemUndamagableDamage, tag, null);
	}

	private static ItemIdentifier get(Item item, int itemUndamagableDamage, CompoundTag tag, ItemIdentifier proposal) {
		if (itemUndamagableDamage < 0) {
			throw new IllegalArgumentException("Item Damage out of range");
		}
		if (tag == null && itemUndamagableDamage == 0) {
			//no tag, no damage
			return ItemIdentifier.getOrCreateSimple(item, proposal);
		} else if (tag == null) {
			//no tag, damage
			return ItemIdentifier.getOrCreateDamage(item, itemUndamagableDamage, proposal);
		} else {
			//tag
			return ItemIdentifier.getOrCreateTag(item, itemUndamagableDamage, new FinalCompoundTag(tag));
		}
	}

	@AllArgsConstructor
	public static class ItemStackAddInfo implements IAddInfo {

		private final ItemIdentifier ident;
	}

	@SuppressWarnings("ConstantConditions")
	@Nonnull
	public static ItemIdentifier get(@Nonnull ItemStack itemStack) {
		ItemIdentifier proposal = null;
		IAddInfoProvider prov = null;
		if (((Object) itemStack) instanceof IAddInfoProvider && !logisticspipes.utils.item.StackTag.hasTag(itemStack)) {
			prov = (IAddInfoProvider) (Object) itemStack;
			ItemStackAddInfo info = prov.getLogisticsPipesAddInfo(ItemStackAddInfo.class);
			if (info != null) {
				proposal = info.ident;
			}
		}
		ItemIdentifier ident = ItemIdentifier.get(itemStack.getItem(), itemStack.getDamageValue(), logisticspipes.utils.item.StackTag.getTag(itemStack), proposal);
		if (ident != proposal && prov != null && !logisticspipes.utils.item.StackTag.hasTag(itemStack)) {
			prov.setLogisticsPipesAddInfo(new ItemStackAddInfo(ident));
		}
		return ident;
	}

	public static List<ItemIdentifier> getMatchingNBTIdentifier(Item item, int itemData) {
		//inefficient, we'll have to add another map if this becomes a bottleneck
		ArrayList<ItemIdentifier> resultlist = new ArrayList<>(16);
		ItemIdentifier.keyRefRlock.lock();
		for (IDReference r : ItemIdentifier.keyRefMap.values()) {
			ItemIdentifier t = r.get();
			if (t != null && t.item == item && t.itemDamage == itemData) {
				resultlist.add(t);
			}
		}
		ItemIdentifier.keyRefRlock.unlock();
		return resultlist;
	}

	/* Instance Methods */

	public ItemIdentifier getUndamaged() {
		if (_IDIgnoringDamage == null) {
			if (!unsafeMakeNormalStack(1).isDamageableItem()) {
				_IDIgnoringDamage = this;
			} else {
				ItemStack tstack = makeNormalStack(1);
				tstack.setDamageValue(0);
				_IDIgnoringDamage = ItemIdentifier.get(tstack);
			}
		}
		return _IDIgnoringDamage;
	}

	public ItemIdentifier getIgnoringNBT() {
		if (_IDIgnoringNBT == null) {
			if (tag == null) {
				_IDIgnoringNBT = this;
			} else {
				_IDIgnoringNBT = ItemIdentifier.get(item, itemDamage, null, null);
			}
		}
		return _IDIgnoringNBT;
	}

	public ItemIdentifier getIgnoringData() {
		if (_IDIgnoringData == null) {
			if (itemDamage == 0) {
				_IDIgnoringData = this;
			} else {
				_IDIgnoringData = ItemIdentifier.get(item, 0, tag, null);
			}
		}
		return _IDIgnoringData;
	}

	public String getDebugName() {
		return item.getDescriptionId() + "(ID: " + BuiltInRegistries.ITEM.getId(item) + ", Damage: " + itemDamage + ")";
	}

	@Nonnull
	private String getName(@Nonnull ItemStack stack) {
		return stack.getHoverName().getString();
	}

	@Nonnull
	public String getFriendlyName() {
		return getName(unsafeMakeNormalStack(1));
	}

	public String getFriendlyNameCC() {
		return MainProxy.proxy.getName(this);
	}

	public String getModName() {
		if (modName == null) {
			ResourceLocation rl = BuiltInRegistries.ITEM.getKey(item);
			if (rl != null) {
				modName = ModList.get().getModContainerById(rl.getNamespace())
						.map(mc -> mc.getModInfo().getDisplayName())
						.orElse("UNKNOWN");
			} else {
				modName = "UNKNOWN";
			}
		}
		return modName;
	}

	// Item -> registry-key path of the FIRST CATEGORY tab containing it, built once on demand.
	// Vanilla only builds tab contents from the client creative-inventory screen, so on a
	// dedicated server (and on survival clients) getDisplayItems() stays empty forever; we
	// build the contents ourselves instead of depending on that screen having been opened.
	private static volatile Map<Item, String> creativeTabNameByItem = null;

	private static Map<Item, String> getCreativeTabNameMap() {
		Map<Item, String> map = ItemIdentifier.creativeTabNameByItem;
		if (map != null) {
			return map;
		}
		synchronized (ItemIdentifier.class) {
			if (ItemIdentifier.creativeTabNameByItem != null) {
				return ItemIdentifier.creativeTabNameByItem;
			}
			map = new HashMap<>();
			net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters params =
					new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
							net.minecraft.world.flag.FeatureFlags.REGISTRY.allFlags(), false,
							net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
			for (net.minecraft.world.item.CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
				// SEARCH aggregates every other tab's items and HOTBAR/INVENTORY are synthetic;
				// only CATEGORY tabs correspond to LP1's per-item CreativeTabs#tabLabel.
				if (tab == null || tab.getType() != net.minecraft.world.item.CreativeModeTab.Type.CATEGORY) {
					continue;
				}
				ResourceLocation key = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
				String tabName = key != null ? key.getPath() : tab.getDisplayName().getString();
				// The client's creative screen may rebuild tab contents concurrently (integrated
				// server thread vs render thread), so guard the whole per-tab read.
				try {
					java.util.Collection<ItemStack> displayItems = tab.getDisplayItems();
					if (displayItems.isEmpty()) {
						tab.buildContents(params);
						displayItems = tab.getDisplayItems();
					}
					for (ItemStack stack : displayItems) {
						if (stack != null && !stack.isEmpty()) {
							map.putIfAbsent(stack.getItem(), tabName);
						}
					}
				} catch (Exception e) {
					LogisticsPipes.log.warn("Failed to read creative tab contents for {}", tabName, e);
				}
			}
			ItemIdentifier.creativeTabNameByItem = map;
			return map;
		}
	}

	public String getCreativeTabName() {
		// In 1.20.1 items are no longer bound to a single creative tab (Item#getCreativeTab
		// was removed). We resolve the FIRST CATEGORY tab containing this item, mirroring
		// LP1's CreativeTabs#tabLabel behaviour. The same string is both stored (via the GUI's
		// getStringForItem -> getCreativeTabName) and compared (ModuleCreativeTabBasedItemSink
		// #sinksItem -> tabList.contains(getCreativeTabName())), so any stable form keeps them
		// matched. We use the tab's registry key path which is stable across sessions.
		if (creativeTabName == null) {
			creativeTabName = ItemIdentifier.getCreativeTabNameMap().get(item);
		}
		return creativeTabName;
	}

	@Nonnull
	public ItemIdentifierStack makeStack(int stackSize) {
		return new ItemIdentifierStack(this, stackSize);
	}

	@Nonnull
	public ItemStack unsafeMakeNormalStack(int stackSize) {
		ItemStack stack = new ItemStack(item, stackSize);
		if (itemDamage != 0) stack.setDamageValue(itemDamage);
		logisticspipes.utils.item.StackTag.setTag(stack, tag);
		return stack;
	}

	@Nonnull
	public ItemStack makeNormalStack(int stackSize) {
		ItemStack stack = new ItemStack(item, stackSize);
		if (itemDamage != 0) stack.setDamageValue(itemDamage);
		if (tag != null) {
			logisticspipes.utils.item.StackTag.setTag(stack, tag.copy());
		}
		return stack;
	}

	@Nonnull
	public ItemEntity makeEntityItem(int stackSize, Level world, double x, double y, double z) {
		return new ItemEntity(world, x, y, z, makeNormalStack(stackSize));
	}

	public int getMaxStackSize() {
		if (maxStackSize == 0) {
			ItemStack tstack = unsafeMakeNormalStack(1);
			int tstacksize = tstack.getMaxStackSize();
			if (tstack.isDamageableItem() && tstack.isDamaged()) {
				tstacksize = 1;
			}
			tstacksize = Math.max(1, Math.min(64, tstacksize));
			maxStackSize = tstacksize;
		}
		return maxStackSize;
	}

	private static Map<Integer, Object> getArrayAsMap(int[] array) {
		HashMap<Integer, Object> map = new HashMap<>();
		int i = 0;
		for (int object : array) {
			map.put(i, object);
			i++;
		}
		return map;
	}

	private static Map<Integer, Object> getArrayAsMap(byte[] array) {
		HashMap<Integer, Object> map = new HashMap<>();
		int i = 1;
		for (byte object : array) {
			map.put(i, object);
			i++;
		}
		return map;
	}

	public static Map<Object, Object> getNBTBaseAsMap(net.minecraft.nbt.Tag nbt) throws SecurityException, IllegalArgumentException {
		if (nbt == null) return null;

		if (nbt instanceof ByteTag) {
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "ByteTag");
			map.put("value", ((ByteTag) nbt).getAsByte());
			return map;
		} else if (nbt instanceof ByteArrayTag) {
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "ByteArrayTag");
			map.put("value", ItemIdentifier.getArrayAsMap(((ByteArrayTag) nbt).getAsByteArray()));
			return map;
		} else if (nbt instanceof DoubleTag) {
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "DoubleTag");
			map.put("value", ((DoubleTag) nbt).getAsDouble());
			return map;
		} else if (nbt instanceof FloatTag) {
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "FloatTag");
			map.put("value", ((FloatTag) nbt).getAsFloat());
			return map;
		} else if (nbt instanceof IntTag) {
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "IntTag");
			map.put("value", ((IntTag) nbt).getAsInt());
			return map;
		} else if (nbt instanceof IntArrayTag) {
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "IntArrayTag");
			map.put("value", ItemIdentifier.getArrayAsMap(((IntArrayTag) nbt).getAsIntArray()));
			return map;
		} else if (nbt instanceof ListTag) {
			HashMap<Integer, Object> content = new HashMap<>();
			int i = 1;
			for (Object object : ((ListTag) nbt)) {
				if (object instanceof net.minecraft.nbt.Tag) {
					content.put(i, ItemIdentifier.getNBTBaseAsMap((net.minecraft.nbt.Tag) object));
				}
				i++;
			}
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "ListTag");
			map.put("value", content);
			return map;
		} else if (nbt instanceof CompoundTag) {
			HashMap<Object, Object> content = new HashMap<>();
			HashMap<Integer, Object> keys = new HashMap<>();
			int i = 1;
			for (String key : ((CompoundTag) nbt).getAllKeys()) {
				net.minecraft.nbt.Tag value = ((CompoundTag) nbt).get(key);
				content.put(key, ItemIdentifier.getNBTBaseAsMap(value));
				keys.put(i, key);
				i++;
			}
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "CompoundTag");
			map.put("value", content);
			map.put("keys", keys);
			return map;
		} else if (nbt instanceof LongTag) {
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "LongTag");
			map.put("value", ((LongTag) nbt).getAsLong());
			return map;
		} else if (nbt instanceof ShortTag) {
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "ShortTag");
			map.put("value", ((ShortTag) nbt).getAsShort());
			return map;
		} else if (nbt instanceof StringTag) {
			HashMap<Object, Object> map = new HashMap<>();
			map.put("type", "StringTag");
			map.put("value", ((StringTag) nbt).getAsString());
			return map;
		} else {
			throw new UnsupportedOperationException("Unsupported net.minecraft.nbt.Tag of type:" + nbt.getClass().getName());
		}
	}

	@Override
	public String toString() {
		return getModName() + ":" + getFriendlyName() + ", " + BuiltInRegistries.ITEM.getId(item) + ":" + itemDamage;
	}

	@Override
	public int compareTo(ItemIdentifier o) {
		int c = BuiltInRegistries.ITEM.getId(item) - BuiltInRegistries.ITEM.getId(o.item);
		if (c != 0) {
			return c;
		}
		c = itemDamage - o.itemDamage;
		if (c != 0) {
			return c;
		}
		c = uniqueID - o.uniqueID;
		return c;
	}

	@Override
	public boolean equals(Object that) {
		if (that instanceof ItemIdentifierStack) {
			throw new IllegalStateException("Comparison between ItemIdentifierStack and ItemIdentifier -- did you forget a .getItem() in your code?");
		}
		if (!(that instanceof ItemIdentifier)) {
			return false;
		}
		ItemIdentifier i = (ItemIdentifier) that;
		return this.equals(i);

	}

	public boolean equals(ItemIdentifier that) {
		if (that == null) return false;
		return item == that.item && itemDamage == that.itemDamage && uniqueID == that.uniqueID;
	}

	@Override
	public int hashCode() {
		if (tag == null) {
			return item.hashCode() + itemDamage;
		} else {
			return (item.hashCode() + itemDamage) ^ tag.hashCode();
		}
	}

	public boolean equalsForCrafting(ItemIdentifier item) {
		return this.item == item.item && (item.isDamageable() || (itemDamage == item.itemDamage));
	}

	public boolean equalsWithoutNBT(@Nonnull ItemStack stack) {
		return item == stack.getItem() && itemDamage == stack.getDamageValue();
	}

	public boolean equalsWithoutNBT(ItemIdentifier item) {
		return this.item == item.item && itemDamage == item.itemDamage;
	}

	public boolean isDamageable() {
		return unsafeMakeNormalStack(1).isDamageableItem();
	}

	public boolean isFluidContainer() {
		return item instanceof LogisticsFluidContainer;
	}

	@Nullable
	public DictItemIdentifier getDictIdentifiers() {
		if (_dict == null && canHaveDict) {
			_dict = DictItemIdentifier.getDictItemIdentifier(this);
			canHaveDict = false;
		}
		return _dict;
	}

	public void debugDumpData(boolean isClient) {
		StringBuilder sb = new StringBuilder();
		sb.append(isClient ? "Client" : "Server").append(" Item: ")
			.append(BuiltInRegistries.ITEM.getId(item)).append(':').append(itemDamage)
			.append(" uniqueID ").append(uniqueID).append('\n');
		sb.append("Tag: ");
		debugDumpTag(tag, sb);
		sb.append('\n');
		sb.append("Damageable: ").append(isDamageable()).append('\n');
		sb.append("MaxStackSize: ").append(getMaxStackSize()).append('\n');
		if (getUndamaged() == this) {
			sb.append("Undamaged: this\n");
		} else {
			sb.append("Undamaged: (see recursive dump)\n");
			getUndamaged().debugDumpData(isClient);
		}
		sb.append("Mod: ").append(getModName()).append('\n');
		sb.append("CreativeTab: ").append(getCreativeTabName());
		LogisticsPipes.log.info("{}", sb);
		if (getDictIdentifiers() != null) {
			getDictIdentifiers().debugDumpData(isClient);
		}
	}

	private void debugDumpTag(net.minecraft.nbt.Tag nbt, StringBuilder sb) {
		if (nbt == null) {
			sb.append("null");
			return;
		}
		if (nbt instanceof ByteTag) {
			sb.append("TagByte(data=").append(((ByteTag) nbt).getAsByte()).append(")");
		} else if (nbt instanceof ShortTag) {
			sb.append("TagShort(data=").append(((ShortTag) nbt).getAsShort()).append(")");
		} else if (nbt instanceof IntTag) {
			sb.append("TagInt(data=").append(((IntTag) nbt).getAsInt()).append(")");
		} else if (nbt instanceof LongTag) {
			sb.append("TagLong(data=").append(((LongTag) nbt).getAsLong()).append(")");
		} else if (nbt instanceof FloatTag) {
			sb.append("TagFloat(data=").append(((FloatTag) nbt).getAsFloat()).append(")");
		} else if (nbt instanceof DoubleTag) {
			sb.append("TagDouble(data=").append(((DoubleTag) nbt).getAsDouble()).append(")");
		} else if (nbt instanceof StringTag) {
			sb.append("TagString(data=\"").append(((StringTag) nbt).getAsString()).append("\")");
		} else if (nbt instanceof ByteArrayTag) {
			sb.append("TagByteArray(data=");
			for (int i = 0; i < ((ByteArrayTag) nbt).getAsByteArray().length; i++) {
				sb.append(((ByteArrayTag) nbt).getAsByteArray()[i]);
				if (i < ((ByteArrayTag) nbt).getAsByteArray().length - 1) {
					sb.append(",");
				}
			}
			sb.append(")");
		} else if (nbt instanceof IntArrayTag) {
			sb.append("TagIntArray(data=");
			for (int i = 0; i < ((IntArrayTag) nbt).getAsIntArray().length; i++) {
				sb.append(((IntArrayTag) nbt).getAsIntArray()[i]);
				if (i < ((IntArrayTag) nbt).getAsIntArray().length - 1) {
					sb.append(",");
				}
			}
			sb.append(")");
		} else if (nbt instanceof ListTag) {
			sb.append("TagList(data=");
			for (int i = 0; i < ((ListTag) nbt).size(); i++) {
				debugDumpTag((((ListTag) nbt).get(i)), sb);
				if (i < ((ListTag) nbt).size() - 1) {
					sb.append(",");
				}
			}
			sb.append(")");
		} else if (nbt instanceof CompoundTag) {
			sb.append("TagCompound(data=");
			for (Iterator<String> iter = ((CompoundTag) nbt).getAllKeys().iterator(); iter.hasNext(); ) {
				String key = iter.next();
				net.minecraft.nbt.Tag value = ((CompoundTag) nbt).get(key);
				sb.append("\"").append(key).append("\"=");
				debugDumpTag((value), sb);
				if (iter.hasNext()) {
					sb.append(",");
				}
			}
			sb.append(")");
		} else {
			sb.append(nbt.getClass().getName()).append("(?)");
		}
	}

	@Override
	public Object[] getTypeHolder() {
		return ccTypeHolder;
	}
}
