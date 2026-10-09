package logisticspipes.proxy.specialinventoryhandler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.inventory.ProviderMode;

public class AEInterfaceInventoryHandler extends SpecialInventoryHandler implements SpecialInventoryHandler.Factory {

	// AE2 15 registers Capability<MEStorage> (appeng.capabilities.Capabilities.STORAGE, not API)
	private static final Capability<MEStorage> ME_STORAGE = CapabilityManager.get(new CapabilityToken<>() {});

	private final BlockEntity tile;
	private final Direction dir;
	private final IActionSource source;
	private LinkedList<Entry<ItemIdentifier, Integer>> cached;
	private final boolean hideOne;

	private AEInterfaceInventoryHandler(BlockEntity tile, Direction dir, ProviderMode mode) {
		this.tile = tile;
		this.dir = dir;
		hideOne = mode.getHideOnePerStack() || mode.getHideOnePerType();
		source = tile instanceof IActionHost host ? IActionSource.ofMachine(host) : IActionSource.empty();
	}

	public AEInterfaceInventoryHandler() {
		tile = null;
		dir = null;
		source = null;
		hideOne = false;
	}

	@Nullable
	private static MEStorage getStorage(@Nonnull BlockEntity tile, @Nullable Direction dir) {
		return tile.getCapability(ME_STORAGE, dir).resolve().orElse(null);
	}

	// unconfigured interfaces expose the whole network, configured ones their own slots
	@Nullable
	private MEStorage storage() {
		return getStorage(tile, dir);
	}

	private static int clamp(long amount) {
		return (int) Math.min(amount, Integer.MAX_VALUE);
	}

	@Override
	public boolean isType(@Nonnull BlockEntity tile, @Nullable Direction dir) {
		return getStorage(tile, dir) != null;
	}

	@Override
	public boolean init() {
		return true;
	}

	@Nonnull
	@Override
	public SpecialInventoryHandler getUtilForTile(@Nonnull BlockEntity tile, @Nullable Direction direction, @Nonnull ProviderMode mode) {
		return new AEInterfaceInventoryHandler(tile, direction, mode);
	}

	@Override
	@Nonnull
	public Map<ItemIdentifier, Integer> getItemsAndCount() {
		return getItemsAndCount(false);
	}

	private Map<ItemIdentifier, Integer> getItemsAndCount(boolean linked) {
		Map<ItemIdentifier, Integer> result = linked ? new LinkedHashMap<>() : new HashMap<>();
		MEStorage storage = storage();
		if (storage == null) {
			return result;
		}
		for (Object2LongMap.Entry<AEKey> entry : storage.getAvailableStacks()) {
			if (!(entry.getKey() instanceof AEItemKey key)) continue;
			ItemIdentifier ident = ItemIdentifier.get(key.toStack());
			int count = clamp(entry.getLongValue() - (hideOne ? 1 : 0));
			result.merge(ident, count, (a, b) -> clamp((long) a + b));
		}
		return result;
	}

	@Override
	@Nonnull
	public ItemStack getSingleItem(ItemIdentifier item) {
		return extract(item, 1);
	}

	@Override
	@Nonnull
	public ItemStack getMultipleItems(@Nonnull ItemIdentifier itemIdent, int count) {
		if (itemCount(itemIdent) < count) {
			return ItemStack.EMPTY;
		}
		return extract(itemIdent, count);
	}

	@Nonnull
	private ItemStack extract(ItemIdentifier itemIdent, int count) {
		MEStorage storage = storage();
		AEItemKey key = AEItemKey.of(itemIdent.makeNormalStack(1));
		if (storage == null || key == null) {
			return ItemStack.EMPTY;
		}
		long extracted = storage.extract(key, count, Actionable.MODULATE, source);
		if (extracted <= 0) {
			return ItemStack.EMPTY;
		}
		return key.toStack(clamp(extracted));
	}

	@Override
	public boolean containsUndamagedItem(@Nonnull ItemIdentifier itemIdent) {
		return getItems().contains(itemIdent);
	}

	@Override
	public int roomForItem(@Nonnull ItemStack itemStack) {
		MEStorage storage = storage();
		AEItemKey key = AEItemKey.of(itemStack);
		if (storage == null || key == null) {
			return 0;
		}
		return clamp(storage.insert(key, itemStack.getCount(), Actionable.SIMULATE, source));
	}

	@Override
	@Nonnull
	public Set<ItemIdentifier> getItems() {
		Set<ItemIdentifier> result = new TreeSet<>();
		MEStorage storage = storage();
		if (storage == null) {
			return result;
		}
		for (AEKey key : storage.getAvailableStacks().keySet()) {
			if (key instanceof AEItemKey itemKey) {
				result.add(ItemIdentifier.get(itemKey.toStack()));
			}
		}
		return result;
	}

	@Override
	public int getContainerSize() {
		if (cached == null) {
			initCache();
		}
		// allow LP putting items into AE
		return cached.size() + 1;
	}

	private void initCache() {
		cached = new LinkedList<>(getItemsAndCount(true).entrySet());
	}

	@Override
	@Nonnull
	public ItemStack getItem(int slot) {
		if (cached == null) {
			initCache();
		}
		if (slot >= cached.size()) {
			return ItemStack.EMPTY;
		}
		Entry<ItemIdentifier, Integer> entry = cached.get(slot);
		if (entry.getValue() == 0) {
			return ItemStack.EMPTY;
		}
		return entry.getKey().makeNormalStack(entry.getValue());
	}

	@Override
	@Nonnull
	public ItemStack removeItem(int slot, int amount) {
		if (cached == null) {
			initCache();
		}
		if (slot >= cached.size()) {
			return ItemStack.EMPTY;
		}
		return getMultipleItems(cached.get(slot).getKey(), amount);
	}

	@Override
	@Nonnull
	public ItemStack add(@Nonnull ItemStack stack, Direction from, boolean doAdd) {
		ItemStack st = stack.copy();
		MEStorage storage = storage();
		AEItemKey key = AEItemKey.of(stack);
		if (storage == null || key == null) {
			st.setCount(0);
			return st;
		}
		st.setCount(clamp(storage.insert(key, stack.getCount(), doAdd ? Actionable.MODULATE : Actionable.SIMULATE, source)));
		return st;
	}
}
