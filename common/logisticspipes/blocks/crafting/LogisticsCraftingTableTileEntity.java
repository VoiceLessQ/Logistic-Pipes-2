package logisticspipes.blocks.crafting;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;



import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import logisticspipes.LPBlocks;
import logisticspipes.api.IRoutedPowerProvider;
import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.config.Configs;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.AutoCraftingGui;
import logisticspipes.network.packets.block.CraftingSetType;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.CraftingUtil;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.PlayerIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.property.BitSetProperty;
import network.rs485.logisticspipes.property.IBitSet;
import network.rs485.logisticspipes.util.FuzzyUtil;
import network.rs485.logisticspipes.util.items.ItemStackLoader;

public class LogisticsCraftingTableTileEntity extends LogisticsSolidTileEntity
		implements Container, IGuiTileEntity, ISimpleInventoryEventHandler, IGuiOpenControler {

	public final BitSetProperty fuzzyFlags = new BitSetProperty(new BitSet(4 * (9 + 1)), "fuzzyBitSet");
	public ItemIdentifierInventory inv = new ItemIdentifierInventory(18, "Crafting Resources", 64);
	public ItemIdentifierInventory matrix = new ItemIdentifierInventory(9, "Crafting Matrix", 1);
	public ItemIdentifierInventory resultInv = new ItemIdentifierInventory(1, "Crafting Result", 1);
	public ItemIdentifier targetType = null;

	private ResultContainer vanillaResult = new ResultContainer();
	private Recipe cache;
	private ServerPlayer fake;
	private PlayerIdentifier placedBy = null;

	private InvWrapper invWrapper = new InvWrapper(this);

	private PlayerCollectionList guiWatcher = new PlayerCollectionList();

	public LogisticsCraftingTableTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(logisticspipes.LPRegistries.BE_CRAFTING_TABLE.get(), pos, state);
		matrix.addListener(this);
	}

	public void cacheRecipe() {
		ItemIdentifier oldTargetType = targetType;
		cache = null;
		resultInv.setItem(0, ItemStack.EMPTY);
		AutoCraftingInventory craftInv = new AutoCraftingInventory(placedBy);
		for (int i = 0; i < 9; i++) {
			craftInv.setItem(i, matrix.getItem(i));
		}
		List<Recipe> list = new ArrayList<>();
		for (Recipe r : CraftingUtil.getRecipeList()) {
			if (r.matches(craftInv.asCraftInput(), getWorld())) {
				list.add(r);
			}
		}
		if (list.size() == 1) {
			cache = list.get(0);
			resultInv.setItem(0, cache.assemble(craftInv.asCraftInput(), getWorld().registryAccess()));
			targetType = null;
		} else if (list.size() > 1) {
			if (targetType != null) {
				for (Recipe recipe : list) {
					craftInv = new AutoCraftingInventory(placedBy);
					for (int i = 0; i < 9; i++) {
						craftInv.setItem(i, matrix.getItem(i));
					}
					ItemStack result = recipe.assemble(craftInv.asCraftInput(), getWorld().registryAccess());
					if (!result.isEmpty() && targetType.equals(ItemIdentifier.get(result))) {
						resultInv.setItem(0, result);
						cache = recipe;
						break;
					}
				}
			}
			if (cache == null) {
				for (Recipe r : list) {
					ItemStack result = r.assemble(craftInv.asCraftInput(), getWorld().registryAccess());
					if (!result.isEmpty()) {
						cache = r;
						resultInv.setItem(0, result);
						targetType = ItemIdentifier.get(result);
						break;
					}
				}
			}
		} else {
			targetType = null;
		}
		if (((targetType == null && oldTargetType != null) || (targetType != null && !targetType.equals(oldTargetType)))
				&& !guiWatcher.isEmpty() && MainProxy.isServer(getWorld())) {
			MainProxy.sendToPlayerList(
					PacketHandler.getPacket(CraftingSetType.class).setTargetType(targetType).setTilePos(this),
					guiWatcher);
		}
	}

	public void cycleRecipe(boolean down) {
		cacheRecipe();
		if (targetType == null) return;

		cache = null;
		AutoCraftingInventory craftInv = new AutoCraftingInventory(placedBy);

		for (int i = 0; i < 9; i++) {
			craftInv.setItem(i, matrix.getItem(i));
		}

		List<Recipe> list = new ArrayList<>();
		for (Recipe r : CraftingUtil.getRecipeList()) {
			if (r.matches(craftInv.asCraftInput(), getWorld())) {
				list.add(r);
			}
		}

		if (list.size() > 1) {
			boolean found = false;
			Recipe prev = null;
			for (Recipe recipe : list) {
				if (found) {
					cache = recipe;
					break;
				}
				craftInv = new AutoCraftingInventory(placedBy);
				for (int i = 0; i < 9; i++) {
					craftInv.setItem(i, matrix.getItem(i));
				}
				if (targetType != null && targetType.equals(ItemIdentifier.get(recipe.assemble(craftInv.asCraftInput(), getWorld().registryAccess())))) {
					if (down) {
						found = true;
					} else {
						if (prev == null) {
							cache = list.get(list.size() - 1);
						} else {
							cache = prev;
						}
						break;
					}
				}
				prev = recipe;
			}

			if (cache == null) {
				cache = list.get(0);
			}

			craftInv = new AutoCraftingInventory(placedBy);
			for (int i = 0; i < 9; i++) {
				craftInv.setItem(i, matrix.getItem(i));
			}

			targetType = ItemIdentifier.get(cache.assemble(craftInv.asCraftInput(), getWorld().registryAccess()));
		}

		if (!guiWatcher.isEmpty() && MainProxy.isServer(getWorld())) {
			MainProxy.sendToPlayerList(
					PacketHandler.getPacket(CraftingSetType.class).setTargetType(targetType).setTilePos(this),
					guiWatcher);
		}

		cacheRecipe();
	}

	public IBitSet outputFuzzy() {
		final int startIdx = 4 * 9; // after the 9th slot
		return fuzzyFlags.get(startIdx, startIdx + 3);
	}

	public IBitSet inputFuzzy(int slot) {
		final int startIdx = 4 * slot;
		return fuzzyFlags.get(startIdx, startIdx + 3);
	}

	@Nonnull
	public ItemStack getOutput(IResource wanted, IRoutedPowerProvider power) {
		boolean isFuzzy = isFuzzy();
		if (cache == null) {
			cacheRecipe();
			if (cache == null) {
				return ItemStack.EMPTY;
			}
		}
		int[] toUse = new int[9];
		int[] used = new int[inv.getContainerSize()];
		outer:
		for (int i = 0; i < 9; i++) {
			ItemIdentifierStack item = matrix.getIDStackInSlot(i);
			if (item == null) {
				toUse[i] = -1;
				continue;
			}
			ItemIdentifier ident = item.getItem();
			for (int j = 0; j < inv.getContainerSize(); j++) {
				item = inv.getIDStackInSlot(j);
				if (item == null) {
					continue;
				}

				final boolean doItemsEqual = isFuzzy ?
						(FuzzyUtil.INSTANCE
								.fuzzyMatches(FuzzyUtil.INSTANCE.getter(inputFuzzy(i)), ident, item.getItem())) :
						ident.equalsForCrafting(item.getItem());

				if (doItemsEqual && item.getStackSize() > used[j]) {
					used[j]++;
					toUse[i] = j;
					continue outer;
				}
			}
			//Not enough material
			return ItemStack.EMPTY;
		}
		AutoCraftingInventory crafter = new AutoCraftingInventory(placedBy);
		for (int i = 0; i < 9; i++) {
			int j = toUse[i];
			if (j != -1) {
				crafter.setItem(i, inv.getItem(j));
			}
		}
		Recipe recipe = cache;
		final ItemIdentifierStack outStack = Objects.requireNonNull(resultInv.getIDStackInSlot(0));
		if (!recipe.matches(crafter.asCraftInput(), getWorld())) {
			if (isFuzzy && outputFuzzy().nextSetBit(0) != -1) {
				recipe = null;
				for (Recipe r : CraftingUtil.getRecipeList()) {

					if (r.matches(crafter.asCraftInput(), getWorld()) && FuzzyUtil.INSTANCE
							.fuzzyMatches(FuzzyUtil.INSTANCE.getter(outputFuzzy()), outStack.getItem(),
									ItemIdentifier.get(r.getResultItem(getWorld().registryAccess())))) {
						recipe = r;
						break;
					}
				}
				if (recipe == null) {
					return ItemStack.EMPTY;
				}
			} else {
				return ItemStack.EMPTY; //Fix MystCraft
			}
		}
		ItemStack result = recipe.assemble(crafter.asCraftInput(), getWorld().registryAccess());
		if (result.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (isFuzzy && outputFuzzy().nextSetBit(0) != -1) {
			if (!FuzzyUtil.INSTANCE.fuzzyMatches(FuzzyUtil.INSTANCE.getter(outputFuzzy()), outStack.getItem(),
					ItemIdentifier.get(result))) {
				return ItemStack.EMPTY;
			}
			if (!FuzzyUtil.INSTANCE.fuzzyMatches(FuzzyUtil.INSTANCE.getter(outputFuzzy()), wanted.getAsItem(),
					ItemIdentifier.get(result))) {
				return ItemStack.EMPTY;
			}
		} else {
			if (!outStack.getItem().equalsWithoutNBT(ItemIdentifier.get(result))) {
				return ItemStack.EMPTY;
			}
			if (!wanted.matches(outStack.getItem(), IResource.MatchSettings.WITHOUT_NBT)) {
				return ItemStack.EMPTY;
			}
		}
		if (!power.useEnergy(Configs.LOGISTICS_CRAFTING_TABLE_POWER_USAGE)) {
			return ItemStack.EMPTY;
		}
		crafter = new AutoCraftingInventory(placedBy);
		for (int i = 0; i < 9; i++) {
			int j = toUse[i];
			if (j != -1) {
				crafter.setItem(i, inv.removeItem(j, 1));
			}
		}
		result = recipe.assemble(crafter.asCraftInput(), getWorld().registryAccess());
		if (fake == null) {
			fake = MainProxy.getFakePlayer(getWorld());
		}
		result = result.copy();
		result.onCraftedBy(getWorld(), fake, result.getCount());
		for (int i = 0; i < 9; i++) {
			ItemStack left = crafter.getItem(i);
			crafter.setItem(i, ItemStack.EMPTY);
			if (!left.isEmpty()) {
				left.setCount(inv.addCompressed(left, false));
				if (left.getCount() > 0) {
					ItemIdentifierInventory.dropItems(level, left, getBlockPos());
				}
			}
		}
		for (int i = 0; i < fake.getInventory().getContainerSize(); i++) {
			ItemStack left = fake.getInventory().getItem(i);
			fake.getInventory().setItem(i, ItemStack.EMPTY);
			if (!left.isEmpty()) {
				left.setCount(inv.addCompressed(left, false));
				if (left.getCount() > 0) {
					ItemIdentifierInventory.dropItems(level, left, getBlockPos());
				}
			}
		}
		return result;
	}

	@Override
	public void onBlockBreak() {
		inv.dropContents(level, getBlockPos());
	}

	@Override
	public void InventoryChanged(Container inventory) {
		if (inventory == matrix) {
			cacheRecipe();
		}
	}

	public void handleNEIRecipePacket(NonNullList<ItemStack> content) {
		if (matrix.getContainerSize() != content.size())
			throw new IllegalStateException("Different sizes of matrix and inventory from packet");
		for (int i = 0; i < content.size(); i++) {
			matrix.setItem(i, content.get(i));
		}
		cacheRecipe();
	}

	@Override
	protected void loadAdditional(CompoundTag par1nbtTagCompound, net.minecraft.core.HolderLookup.Provider registries) {
		super.loadAdditional(par1nbtTagCompound, registries);
		inv.readFromNBT(par1nbtTagCompound, "inv");
		matrix.readFromNBT(par1nbtTagCompound, "matrix");
		if (par1nbtTagCompound.contains("placedBy")) {
			String name = par1nbtTagCompound.getString("placedBy");
			placedBy = PlayerIdentifier.convertFromUsername(name);
		} else {
			placedBy = PlayerIdentifier.readFromNBT(par1nbtTagCompound, "placedBy");
		}
		fuzzyFlags.readFromNBT(par1nbtTagCompound);
		if (par1nbtTagCompound.contains("targetType")) {
			targetType = ItemIdentifier
					.get(ItemStackLoader.loadAndFixItemStackFromNBT(par1nbtTagCompound.getCompound("targetType")));
		}
		cacheRecipe();
	}

	@Override
	protected void saveAdditional(CompoundTag par1nbtTagCompound, net.minecraft.core.HolderLookup.Provider registries) {
		super.saveAdditional(par1nbtTagCompound, registries);
		inv.writeToNBT(par1nbtTagCompound, "inv");
		matrix.writeToNBT(par1nbtTagCompound, "matrix");
		if (placedBy != null) {
			placedBy.writeToNBT(par1nbtTagCompound, "placedBy");
		}
		fuzzyFlags.writeToNBT(par1nbtTagCompound);
		if (targetType != null) {
			// 1.20.5+ ItemStack.save returns the encoded tag; it does not mutate the argument.
			CompoundTag type = (CompoundTag) targetType.makeNormalStack(1).save(logisticspipes.utils.RegistryAccessUtil.registries(), new CompoundTag());
			par1nbtTagCompound.put("targetType", type);
		} else {
			par1nbtTagCompound.remove("targetType");
		}
	}

	/** Used by RegisterCapabilitiesEvent wiring in LPRegistries. */
	public net.neoforged.neoforge.items.IItemHandler getInvWrapper() {
		return invWrapper;
	}

	@Override
	public int getContainerSize() {
		return inv.getContainerSize();
	}

	@Override
	public boolean isEmpty() {
		return inv.isEmpty();
	}

	@Override
	@Nonnull
	public ItemStack getItem(int i) {
		return inv.getItem(i);
	}

	@Override
	@Nonnull
	public ItemStack removeItem(int i, int j) {
		return inv.removeItem(i, j);
	}

	@Override
	@Nonnull
	public ItemStack removeItemNoUpdate(int i) {
		return inv.removeItemNoUpdate(i);
	}

	@Override
	public void setItem(int i, @Nonnull ItemStack itemstack) {
		inv.setItem(i, itemstack);
	}

	@Override
	public int getMaxStackSize() {
		return inv.getMaxStackSize();
	}

	@Override
	public boolean stillValid(@Nonnull Player entityplayer) {
		return true;
	}

	@Override
	public void startOpen(@Nonnull Player player) {
	}

	@Override
	public void stopOpen(@Nonnull Player player) {
	}

	@Override
	public boolean canPlaceItem(int i, @Nonnull ItemStack itemstack) {
		if (i < 9 && i >= 0) {
			ItemIdentifierStack stack = matrix.getIDStackInSlot(i);
			if (stack != null && !itemstack.isEmpty()) {
				if (isFuzzy() && inputFuzzy(i).nextSetBit(0) != -1) {
					return FuzzyUtil.INSTANCE.fuzzyMatches(FuzzyUtil.INSTANCE.getter(inputFuzzy(i)),
							stack.getItem(),
							ItemIdentifier.get(itemstack));
				}
				return stack.getItem().equalsWithoutNBT(ItemIdentifier.get(itemstack));
			}
		}
		return true;
	}

	public int getField(int id) {
		return 0;
	}

	public void setField(int id, int value) {

	}

	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clearContent() {

	}

	public void placedBy(LivingEntity par5EntityLivingBase) {
		if (par5EntityLivingBase instanceof Player) {
			placedBy = PlayerIdentifier.get((Player) par5EntityLivingBase);
		}
	}

	public boolean isFuzzy() {
		return level.getBlockState(worldPosition).getBlock() == LPBlocks.crafterFuzzy.get();
	}

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(AutoCraftingGui.class).setCraftingTable(this);
	}

	@Override
	public void guiOpenedByPlayer(Player player) {
		guiWatcher.add(player);
	}

	@Override
	public void guiClosedByPlayer(Player player) {
		guiWatcher.remove(player);
	}

	@Nonnull
	public String getName() {
		return "LogisticsCraftingTable";
	}

	public boolean hasCustomName() {
		return true;
	}

	@Nullable
	public Component getDisplayName() {
		return null;
	}

	@SubscribeEvent
	public void onWorldUnload(LevelEvent.Unload worldEvent) {
		if (fake.level() == ((LevelEvent) worldEvent).getLevel()) fake = null;
	}
}
