package logisticspipes.pipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;


import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;

import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.crafting.AutoCraftingInventory;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IRequestWatcher;
import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.TransportLayer;
import logisticspipes.network.GuiIDs;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.CraftingSetType;
import logisticspipes.network.packets.block.RequestRotationPacket;
import logisticspipes.network.packets.orderer.OrderWatchRemovePacket;
import logisticspipes.network.packets.orderer.OrdererWatchPacket;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LinkedLogisticsOrderList;
import logisticspipes.security.SecuritySettings;
import net.minecraft.network.chat.Component;

import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.CraftingUtil;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.SimpleStackInventory;
import logisticspipes.utils.tuples.Pair;

public class PipeBlockRequestTable extends PipeItemsRequestLogistics implements ISimpleInventoryEventHandler, IRequestWatcher, IGuiOpenControler, IRotationProvider {

	public SimpleStackInventory diskInv = new SimpleStackInventory(1, "Disk Slot", 1);
	public SimpleStackInventory inv = new SimpleStackInventory(27, "Crafting Resources", 64);
	public ItemIdentifierInventory matrix = new ItemIdentifierInventory(9, "Crafting Matrix", 1);
	public ItemIdentifierInventory resultInv = new ItemIdentifierInventory(1, "Crafting Result", 1);
	public SimpleStackInventory toSortInv = new SimpleStackInventory(1, "Sorting Slot", 64);
	private ResultContainer vanillaResult = new ResultContainer();
	private Recipe cache;
	private ServerPlayer fake;
	private int delay = 0;
	private int tick = 0;
	private int rotation;
	private boolean init = false;

	private PlayerCollectionList localGuiWatcher = new PlayerCollectionList();
	public Map<Integer, Pair<IResource, LinkedLogisticsOrderList>> watchedRequests = new HashMap<>();
	private int localLastUsedWatcherId = 0;

	public ItemIdentifier targetType = null;

	public PipeBlockRequestTable(Item item) {
		super(item);
		matrix.addListener(this);
	}

	@Override
	public boolean handleClick(Player entityplayer, SecuritySettings settings) {
		//allow using upgrade manager
		if (MainProxy.isPipeControllerEquipped(entityplayer) && !(entityplayer.isCrouching())) {
			return false;
		}
		if (MainProxy.isServer(getWorld())) {
			if (settings == null || settings.openGui) {
				openGui(entityplayer);
			} else {
				entityplayer.sendSystemMessage(Component.translatable("lp.chat.permissiondenied"));
			}
		}
		return true;
	}

	@Override
	public void ignoreDisableUpdateEntity() {
		super.ignoreDisableUpdateEntity();
		if (tick++ == 5) {
			if (getWorld() != null) {
				net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(getX(), getY(), getZ());
				net.minecraft.world.level.block.state.BlockState state = getWorld().getBlockState(pos);
				getWorld().sendBlockUpdated(pos, state, state, 3);
			}
		}
		if (MainProxy.isClient(getWorld())) {
			if (!init) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestRotationPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
				init = true;
			}
			return;
		}
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		if (tick % 2 == 0 && !localGuiWatcher.isEmpty()) {
			checkForExpired();
			if (getUpgradeManager().hasCraftingMonitoringUpgrade()) {
				for (Entry<Integer, Pair<IResource, LinkedLogisticsOrderList>> entry : watchedRequests.entrySet()) {
					MainProxy.sendToPlayerList(PacketHandler.getPacket(OrdererWatchPacket.class).setOrders(entry.getValue().getValue2()).setStack(entry.getValue().getValue1()).putInt(entry.getKey()).setTilePos(container), localGuiWatcher);
				}
			}
		} else if (tick % 20 == 0) {
			checkForExpired();
		}
	}

	private void checkForExpired() {
		Iterator<Entry<Integer, Pair<IResource, LinkedLogisticsOrderList>>> iter = watchedRequests.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, Pair<IResource, LinkedLogisticsOrderList>> entry = iter.next();
			if (isDone(entry.getValue().getValue2())) {
				MainProxy.sendToPlayerList(PacketHandler.getPacket(OrderWatchRemovePacket.class).putInt(entry.getKey()).setTilePos(container), localGuiWatcher);
				iter.remove();
			}
		}
	}

	private boolean isDone(LinkedLogisticsOrderList orders) {
		boolean isDone = true;
		for (IOrderInfoProvider order : orders) {
			if (!order.isFinished()) {
				isDone = false;
			}
			if (!order.getProgresses().isEmpty()) {
				isDone = false;
			}
		}
		for (LinkedLogisticsOrderList orderList : orders.getSubOrders()) {
			if (!isDone(orderList)) {
				isDone = false;
			}
		}
		return isDone;
	}

	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		ItemStack stack = toSortInv.getItem(0);
		if (!stack.isEmpty()) {
			if (delay > 0) {
				delay--;
				return;
			}
			IRoutedItem itemToSend = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stack);
			SimpleServiceLocator.logisticsManager.assignDestinationFor(itemToSend, getRouter().getSimpleID(), false);
			if (itemToSend.getDestinationUUID() != null) {
				Direction dir = getRouteLayer().getOrientationForItem(itemToSend, null);
				super.queueRoutedItem(itemToSend, dir.getOpposite());
				spawnParticle(Particles.OrangeParticle, 4);
				toSortInv.clearInventorySlotContents(0);
			} else {
				delay = 100;
			}
		} else {
			delay = 0;
		}
	}

	@Override
	public void openGui(Player entityplayer) {
		boolean flag = true;
		if (!diskInv.getItem(0).isEmpty()) {
			if (!entityplayer.getMainHandItem().isEmpty() && entityplayer.getMainHandItem().getItem().equals(LPItems.disk.get())) {
				diskInv.setItem(0, entityplayer.getMainHandItem());
				entityplayer.getInventory().setItem(entityplayer.getInventory().selected, ItemStack.EMPTY);
				flag = false;
			}
		}
		if (flag) {
			logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.pipe.RequestTableGui.class)
					.setPosX(getX()).setPosY(getY()).setPosZ(getZ())
					.open(entityplayer);
		}
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.empty;
	}

	@Override
	public TextureType getRoutedTexture(Direction connection) {
		return Textures.empty_1;
	}

	@Override
	public TextureType getNonRoutedTexture(Direction connection) {
		return Textures.empty_2;
	}

	/*public TextureAtlasSprite getTextureFor(int l) {
		Direction dir = Direction.from3DDataValue(l);
		//if (LogisticsPipes.getClientPlayerConfig().isUseNewRenderer()) {
			switch (dir) {
				case UP:
				case DOWN:
					return Textures.LOGISTICS_REQUEST_TABLE_NEW_EMPTY;
				default:
					if (container.renderState.pipeConnectionMatrix.isConnected(dir)) {
						if (container.renderState.textureMatrix.getTextureIndex(dir) == 1) {
							return Textures.LOGISTICS_REQUEST_TABLE_NEW_ROUTED;
						} else {
							return Textures.LOGISTICS_REQUEST_TABLE_NEW_UNROUTED;
						}
					} else {
						return Textures.LOGISTICS_REQUEST_TABLE_NEW_EMPTY;
					}
			}
		} else {
			switch (dir) {
				case UP:
					return Textures.LOGISTICS_REQUEST_TABLE[0];
				case DOWN:
					return Textures.LOGISTICS_REQUEST_TABLE[1];
				default:
					if (container.renderState.pipeConnectionMatrix.isConnected(dir)) {
						if (container.renderState.textureMatrix.getTextureIndex(dir) == 1) {
							return Textures.LOGISTICS_REQUEST_TABLE[2];
						} else {
							return Textures.LOGISTICS_REQUEST_TABLE[3];
						}
					} else {
						return Textures.LOGISTICS_REQUEST_TABLE[4];
					}
			}
		}
	}*/

	@Override
	public void onAllowedRemoval() {
		if (MainProxy.isServer(getWorld())) {
			inv.dropContents(getWorld(), getPos());
			toSortInv.dropContents(getWorld(), getPos());
			diskInv.dropContents(getWorld(), getPos());
		}
	}

	public void cacheRecipe() {
		ItemIdentifier oldTargetType = targetType;
		cache = null;
		resultInv.clearInventorySlotContents(0);
		AutoCraftingInventory craftInv = new AutoCraftingInventory(null);
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
					craftInv = new AutoCraftingInventory(null);
					for (int i = 0; i < 9; i++) {
						craftInv.setItem(i, matrix.getItem(i));
					}
					ItemStack result = recipe.assemble(craftInv.asCraftInput(), getWorld().registryAccess());
					if (targetType == ItemIdentifier.get(result)) {
						resultInv.setItem(0, result);
						cache = recipe;
						break;
					}
				}
			}
			if (cache == null) {
				cache = list.get(0);
				ItemStack result = cache.assemble(craftInv.asCraftInput(), getWorld().registryAccess());
				resultInv.setItem(0, result);
				targetType = ItemIdentifier.get(result);
			}
		} else {
			targetType = null;
		}
		if (targetType != oldTargetType && !localGuiWatcher.isEmpty() && getWorld() != null && MainProxy.isServer(getWorld())) {
			MainProxy.sendToPlayerList(PacketHandler.getPacket(CraftingSetType.class).setTargetType(targetType).setTilePos(container), localGuiWatcher);
		}
	}

	public void cycleRecipe(boolean down) {
		cacheRecipe();
		if (targetType == null) {
			return;
		}
		cache = null;
		AutoCraftingInventory craftInv = new AutoCraftingInventory(null);
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
				craftInv = new AutoCraftingInventory(null);
				for (int i = 0; i < 9; i++) {
					craftInv.setItem(i, matrix.getItem(i));
				}
				if (targetType == ItemIdentifier.get(recipe.assemble(craftInv.asCraftInput(), getWorld().registryAccess()))) {
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
			craftInv = new AutoCraftingInventory(null);
			for (int i = 0; i < 9; i++) {
				craftInv.setItem(i, matrix.getItem(i));
			}
			targetType = ItemIdentifier.get(cache.assemble(craftInv.asCraftInput(), getWorld().registryAccess()));
		}
		if (!localGuiWatcher.isEmpty() && getWorld() != null && MainProxy.isServer(getWorld())) {
			MainProxy.sendToPlayerList(PacketHandler.getPacket(CraftingSetType.class).setTargetType(targetType).setTilePos(container), localGuiWatcher);
		}
		cacheRecipe();
	}

	@Nonnull
	public ItemStack getOutput(boolean oreDict) {
		if (cache == null) {
			cacheRecipe();
			if (cache == null) {
				return ItemStack.EMPTY;
			}
		}
		if (resultInv.getIDStackInSlot(0) == null) {
			return ItemStack.EMPTY;
		}

		int[] toUse = new int[9];
		int[] used = new int[inv.getContainerSize()];
		outer:
		for (int i = 0; i < 9; i++) {
			ItemStack item = matrix.getItem(i);
			if (item.isEmpty()) {
				toUse[i] = -1;
				continue;
			}
			ItemIdentifier ident = ItemIdentifier.get(item);
			for (int j = 0; j < inv.getContainerSize(); j++) {
				item = inv.getItem(j);
				if (item.isEmpty()) {
					continue;
				}
				ItemIdentifier withIdent = ItemIdentifier.get(item);
				if (ident.equalsForCrafting(withIdent)) {
					if (item.getCount() > used[j]) {
						used[j]++;
						toUse[i] = j;
						continue outer;
					}
				}
				if (oreDict) {
					if (ident.getDictIdentifiers() != null && withIdent.getDictIdentifiers() != null && ident.getDictIdentifiers().canMatch(withIdent.getDictIdentifiers(), true, false)) {
						if (item.getCount() > used[j]) {
							used[j]++;
							toUse[i] = j;
							continue outer;
						}
					}
				}
			}
			//Not enough material
			return ItemStack.EMPTY;
		}
		AutoCraftingInventory crafter = new AutoCraftingInventory(null);
		for (int i = 0; i < 9; i++) {
			int j = toUse[i];
			if (j != -1) {
				crafter.setItem(i, inv.getItem(j));
			}
		}
		if (!cache.matches(crafter.asCraftInput(), getWorld())) {
			return ItemStack.EMPTY; //Fix MystCraft
		}
		ItemStack result = cache.assemble(crafter.asCraftInput(), getWorld().registryAccess());
		if (result.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (!resultInv.getIDStackInSlot(0).getItem().equalsWithoutNBT(ItemIdentifier.get(result))) {
			return ItemStack.EMPTY;
		}
		crafter = new AutoCraftingInventory(null);
		for (int i = 0; i < 9; i++) {
			int j = toUse[i];
			if (j != -1) {
				crafter.setItem(i, inv.removeItem(j, 1));
			}
		}
		result = cache.assemble(crafter.asCraftInput(), getWorld().registryAccess());
		if (fake == null) {
			fake = MainProxy.getFakePlayer(getWorld());
		}
		result = result.copy();

		ResultSlot craftingSlot = new ResultSlot(fake, crafter, resultInv, 0, 0, 0);
		vanillaResult.setRecipeUsed(null); // RecipeHolder no longer tracked; fake-player take needs no unlock
		craftingSlot.onTake(fake, result);
		for (int i = 0; i < 9; i++) {
			ItemStack left = crafter.getItem(i);
			crafter.setItem(i, ItemStack.EMPTY);
			if (!left.isEmpty()) {
				left.setCount(inv.addCompressed(left, false));
				if (left.getCount() > 0) {
					ItemIdentifierInventory.dropItems(getWorld(), left, getX(), getY(), getZ());
				}
			}
		}
		for (int i = 0; i < fake.getInventory().getContainerSize(); i++) {
			ItemStack left = fake.getInventory().getItem(i);
			fake.getInventory().setItem(i, ItemStack.EMPTY);
			if (!left.isEmpty()) {
				left.setCount(inv.addCompressed(left, false));
				if (left.getCount() > 0) {
					ItemIdentifierInventory.dropItems(getWorld(), left, getX(), getY(), getZ());
				}
			}
		}
		return result;
	}

	@Nonnull
	public ItemStack getResultForClick() {
		if (MainProxy.isServer(getWorld())) {
			ItemStack result = getOutput(true);
			if (result.isEmpty()) {
				result = getOutput(false);
			}
			if (result.isEmpty()) {
				return ItemStack.EMPTY;
			}
			result.setCount(inv.addCompressed(result, false));
			if (result.getCount() > 0) {
				return result;
			}
			return ItemStack.EMPTY;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void InventoryChanged(Container inventory) {
		if (inventory == matrix) {
			cacheRecipe();
		}
	}

	public void handleNEIRecipePacket(NonNullList<ItemStack> content) {
		if (matrix.getContainerSize() != content.size()) throw new IllegalStateException("Different sizes of matrix and inventory from packet");
		for (int i = 0; i < content.size(); i++) {
			matrix.setItem(i, content.get(i));
		}
		cacheRecipe();
	}

	@Override
	public void readFromNBT(CompoundTag par1nbtTagCompound) {
		super.readFromNBT(par1nbtTagCompound);
		inv.readFromNBT(par1nbtTagCompound, "inv");
		matrix.readFromNBT(par1nbtTagCompound, "matrix");
		toSortInv.readFromNBT(par1nbtTagCompound, "toSortInv");
		diskInv.readFromNBT(par1nbtTagCompound, "diskInv");
		rotation = par1nbtTagCompound.getInt("blockRotation");
		// cacheRecipe() skipped here — world/registry not available during NBT load; cache is rebuilt lazily on first use
		//cacheRecipe();
	}

	@Override
	public void writeToNBT(CompoundTag par1nbtTagCompound) {
		super.writeToNBT(par1nbtTagCompound);
		inv.writeToNBT(par1nbtTagCompound, "inv");
		matrix.writeToNBT(par1nbtTagCompound, "matrix");
		toSortInv.writeToNBT(par1nbtTagCompound, "toSortInv");
		diskInv.writeToNBT(par1nbtTagCompound, "diskInv");
		par1nbtTagCompound.putInt("blockRotation", rotation);
	}

	@Override
	public boolean isOnSameContainer(CoreRoutedPipe other) {
		return false;
	}

	@Nonnull
	@Override
	public TransportLayer getTransportLayer() {
		if (_transportLayer == null) {
			_transportLayer = new TransportLayer() {

				@Override
				public void handleItem(IRoutedItem item) {
					PipeBlockRequestTable.this.notifyOfItemArival(item.getInfo());
					if (item.getItemIdentifierStack() != null) {
						ItemIdentifierStack stack = item.getItemIdentifierStack();
						stack.setStackSize(inv.addCompressed(stack.makeNormalStack(), false));
					}
				}

				@Override
				public Direction itemArrived(IRoutedItem item, Direction denied) {
					return null;
				}

				@Override
				public boolean stillWantItem(IRoutedItem item) {
					return false;
				}
			};
		}
		return _transportLayer;
	}

	@Override
	public void handleOrderList(IResource stack, LinkedLogisticsOrderList orders) {
		if (!getUpgradeManager().hasCraftingMonitoringUpgrade()) {
			return;
		}
		orders.setWatched();
		watchedRequests.put(++localLastUsedWatcherId, new Pair<>(stack, orders));
		MainProxy.sendToPlayerList(PacketHandler.getPacket(OrdererWatchPacket.class).setOrders(orders).setStack(stack).putInt(localLastUsedWatcherId).setTilePos(container), localGuiWatcher);
	}

	@Override
	public void guiOpenedByPlayer(Player player) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrderWatchRemovePacket.class).putInt(-1).setTilePos(container), player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(CraftingSetType.class).setTargetType(targetType).setTilePos(container), player);
		localGuiWatcher.add(player);
		for (Entry<Integer, Pair<IResource, LinkedLogisticsOrderList>> entry : watchedRequests.entrySet()) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrdererWatchPacket.class).setOrders(entry.getValue().getValue2()).setStack(entry.getValue().getValue1()).putInt(entry.getKey()).setTilePos(container), player);
		}
	}

	@Override
	public void guiClosedByPlayer(Player player) {
		localGuiWatcher.remove(player);
	}

	@Override
	public void handleClientSideListInfo(int id, IResource stack, LinkedLogisticsOrderList orders) {
		if (MainProxy.isClient(getWorld())) {
			watchedRequests.put(id, new Pair<>(stack, orders));
		}
	}

	@Override
	public void handleClientSideRemove(int id) {
		if (MainProxy.isClient(getWorld())) {
			if (id == -1) {
				watchedRequests.clear();
			} else {
				watchedRequests.remove(id);
			}
		}
	}

	@Nonnull
	public ItemStack getDisk() {
		return diskInv.getItem(0);
	}

	@Override
	public int getRotation() {
		return rotation;
	}

	@Override
	public void setRotation(int rotation) {
		this.rotation = rotation;
	}

	@Override
	public boolean isMultipartAllowedInPipe() {
		return false;
	}

	@SubscribeEvent
	public void onWorldUnload(LevelEvent.Unload worldEvent) {
		if (fake != null && fake.level() == worldEvent.getLevel())
			fake = null;
	}

	@Override
	public boolean isPipeBlock() {
		return true;
	}
}
