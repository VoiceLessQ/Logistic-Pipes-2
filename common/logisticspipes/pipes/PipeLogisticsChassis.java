/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

import net.minecraft.core.registries.BuiltInRegistries;

import lombok.Getter;

import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.config.Configs;
import logisticspipes.gui.GuiChassisPipe;
import logisticspipes.gui.hud.HudChassisPipe;
import logisticspipes.interfaces.*;
import logisticspipes.interfaces.routing.*;
import logisticspipes.items.ItemModule;
import logisticspipes.logisticspipes.ChassisTransportLayer;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.logisticspipes.TransportLayer;
import logisticspipes.modules.ChassisModule;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.hud.HUDStartWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopWatchingPacket;
import logisticspipes.network.packets.pipe.ChassisOrientationPacket;
import logisticspipes.network.packets.pipe.ChassisPipeModuleContent;
import logisticspipes.network.packets.pipe.RequestChassisOrientationPacket;
import logisticspipes.network.packets.pipe.SendQueueContent;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.ModuleUpgradeManager;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.IPromise;
import logisticspipes.request.RequestTree;
import logisticspipes.request.RequestTreeNode;
import logisticspipes.request.resources.DictResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.LogisticsPromise;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.routing.order.LogisticsItemOrder;
import logisticspipes.routing.order.LogisticsOrder;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.ticks.HudUpdateTick;
import logisticspipes.utils.DirectionUtil;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.connection.*;
import network.rs485.logisticspipes.module.PipeServiceProviderUtilKt;
import network.rs485.logisticspipes.pipes.IChassisPipe;
import network.rs485.logisticspipes.property.AdjacentProperty;
import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.property.PropertyHolder;
import network.rs485.logisticspipes.property.SlottedModule;

@CCType(name = "LogisticsChassiePipe")
public abstract class PipeLogisticsChassis extends CoreRoutedPipe
		implements ICraftItems, IBufferItems, ISimpleInventoryEventHandler, ISendRoutedItem, IProvideItems,
		IHeadUpDisplayRendererProvider, ISendQueueContentRecieiver, IChassisPipe, PropertyHolder {

	private final ChassisModule _module;
	private final ItemIdentifierInventory _moduleInventory;
	private boolean init = false;

	// HUD
	public final LinkedList<ItemIdentifierStack> displayList = new LinkedList<>();
	public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final HudChassisPipe hud;

	@Nonnull
	private final AdjacentProperty pointedAdjacentProperty = new AdjacentProperty(this, "pointedAdjacent");

	private final List<Property<?>> properties = Collections.singletonList(pointedAdjacentProperty);

	public PipeLogisticsChassis(Item item) {
		super(item);
		_moduleInventory = new ItemIdentifierInventory(getChassisSize(), "Chassis pipe", 1);
		_moduleInventory.addListener(this);
		_module = new ChassisModule(getChassisSize(), this);
		_module.registerHandler(this, this);
		hud = new HudChassisPipe(this, _moduleInventory);
	}

	@Nonnull
	@Override
	public List<Property<?>> getProperties() {
		return properties;
	}

	/**
	 * Returns the pointed adjacent Direction or null, if this chassis does not have an attached inventory.
	 */
	@Nullable
	@Override
	public Direction getPointedOrientation() {
		return pointedAdjacentProperty.getDirectionOrNull();
	}

	/**
	 * Returns just the adjacent this chassis points at or no adjacent.
	 */
	@Nonnull
	@Override
	public Adjacent getAvailableAdjacent() {
		return pointedAdjacentProperty.getValue();
	}

	/**
	 * Updates pointedAdjacent on {@link CoreRoutedPipe}.
	 */
	@Override
	protected void updateAdjacentCache() {
		super.updateAdjacentCache();
		final Adjacent adjacent = getAdjacent();
		if (adjacent instanceof SingleAdjacent) {
			pointedAdjacentProperty.setValue(adjacent);
		} else {
			final @Nullable Direction oldPointedDirection = pointedAdjacentProperty.getDirectionOrNull();
			SingleAdjacent newPointedAdjacent = null;
			if (oldPointedDirection != null) {
				// update pointed adjacent with connection type or reset it
				newPointedAdjacent = adjacent.optionalGet(oldPointedDirection)
					.map(connectionType -> new SingleAdjacent(this, oldPointedDirection, connectionType))
					.orElse(null);
			}
			if (newPointedAdjacent == null) {
				newPointedAdjacent = adjacent.neighbors().entrySet().stream().findAny()
					.map(connectedNeighbor -> new SingleAdjacent(this, connectedNeighbor.getKey().getDirection(), connectedNeighbor.getValue()))
					.orElse(null);
			}
			if (newPointedAdjacent == null) {
				pointedAdjacentProperty.setValue(NoAdjacent.INSTANCE);
			} else {
				pointedAdjacentProperty.setValue(newPointedAdjacent);
			}
		}
	}

	@Nullable
	private Pair<NeighborTileEntity<BlockEntity>, ConnectionType> nextPointedOrientation(@Nullable Direction previousDirection) {
		final Map<NeighborTileEntity<BlockEntity>, ConnectionType> neighbors = getAdjacent().neighbors();
		final Stream<NeighborTileEntity<BlockEntity>> sortedNeighborsStream = neighbors.keySet().stream()
				.sorted(Comparator.comparingInt(n -> n.getDirection().ordinal()));
		if (previousDirection == null) {
			return sortedNeighborsStream.findFirst().map(neighbor -> new Pair<>(neighbor, neighbors.get(neighbor))).orElse(null);
		} else {
			final List<NeighborTileEntity<BlockEntity>> sortedNeighbors = sortedNeighborsStream.collect(Collectors.toList());
			if (sortedNeighbors.size() == 0) return null;
			final Optional<NeighborTileEntity<BlockEntity>> nextNeighbor = sortedNeighbors.stream()
					.filter(neighbor -> neighbor.getDirection().ordinal() > previousDirection.ordinal())
					.findFirst();
			return nextNeighbor.map(neighbor -> new Pair<>(neighbor, neighbors.get(neighbor)))
					.orElse(new Pair<>(sortedNeighbors.get(0), neighbors.get(sortedNeighbors.get(0))));
		}
	}

	@Override
	public void nextOrientation() {
		final Direction pointedDirection = pointedAdjacentProperty.getDirectionOrNull();
		Pair<NeighborTileEntity<BlockEntity>, ConnectionType> newNeighbor = nextPointedOrientation(pointedDirection);
		final ChassisOrientationPacket packet = PacketHandler.getPacket(ChassisOrientationPacket.class);
		if (newNeighbor == null) {
			pointedAdjacentProperty.setValue(NoAdjacent.INSTANCE);
			packet.setDir(null);
		} else {
			pointedAdjacentProperty.setValue(
				new SingleAdjacent(this, newNeighbor.getValue1().getDirection(), newNeighbor.getValue2()));
			packet.setDir(newNeighbor.getValue1().getDirection());
		}
		MainProxy.sendPacketToAllWatchingChunk(_module, packet.setTilePos(container));
		refreshRender(true);
	}

	@Override
	public void setPointedOrientation(@Nullable Direction dir) {
		if (dir == null) {
			pointedAdjacentProperty.setValue(NoAdjacent.INSTANCE);
		} else {
			pointedAdjacentProperty.setValue(
				new SingleAdjacent(this, dir, ConnectionType.UNDEFINED));
		}
	}

	private void updateModuleInventory() {
		_module.slottedModules().forEach(slottedModule -> {
			if (slottedModule.isEmpty()) {
				_moduleInventory.clearInventorySlotContents(slottedModule.getSlot());
				return;
			}
			final LogisticsModule module = Objects.requireNonNull(slottedModule.getModule());
			final ItemIdentifierStack idStack = _moduleInventory.getIDStackInSlot(slottedModule.getSlot());
			ItemStack moduleStack;
			if (idStack != null) {
				moduleStack = idStack.getItem().makeNormalStack(1);
			} else {
				ResourceLocation resourceLocation = LPItems.modules.get(module.getLPName());
				Item item = BuiltInRegistries.ITEM.get(resourceLocation);
				if (item == null) return;
				moduleStack = new ItemStack(item);
			}
			ItemModuleInformationManager.saveInformation(moduleStack, module);
			_moduleInventory.setItem(slottedModule.getSlot(), moduleStack);
		});
	}

	@Override
	@Nonnull
	public Container getModuleInventory() {
		updateModuleInventory();
		return _moduleInventory;
	}

	@Nonnull
	public ModuleUpgradeManager getModuleUpgradeManager(int slot) {
		return _module.getModuleUpgradeManager(slot);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_TEXTURE;
	}

	@Override
	public TextureType getRoutedTexture(Direction connection) {
		if (getRouter().isSubPoweredExit(connection)) {
			return Textures.LOGISTICSPIPE_SUBPOWER_TEXTURE;
		}
		return Textures.LOGISTICSPIPE_CHASSI_ROUTED_TEXTURE;
	}

	@Override
	public TextureType getNonRoutedTexture(@Nullable Direction connection) {
		final @Nullable Direction pointedDirection = pointedAdjacentProperty.getDirectionOrNull();
		if (pointedDirection != null && pointedDirection.equals(connection)) {
			return Textures.LOGISTICSPIPE_CHASSI_DIRECTION_TEXTURE;
		}
		if (isPowerProvider(connection)) {
			return Textures.LOGISTICSPIPE_POWERED_TEXTURE;
		}
		return Textures.LOGISTICSPIPE_CHASSI_NOTROUTED_TEXTURE;
	}

	@Override
	public void readFromNBT(@Nonnull CompoundTag tag) {
		super.readFromNBT(tag);
		_moduleInventory.readFromNBT(tag, "chassi");
		_module.fitModuleCount(getChassisSize());

		// register slotted modules
		_module.slottedModules()
				.filter(slottedModule -> !slottedModule.isEmpty())
				.forEach(slottedModule -> {
					LogisticsModule logisticsModule = Objects.requireNonNull(slottedModule.getModule());
					// FIXME: rely on getModuleForItem instead
					logisticsModule.registerHandler(this, this);
					slottedModule.registerPosition();
				});
	}

	@Override
	public void writeToNBT(@Nonnull CompoundTag tag) {
		super.writeToNBT(tag);
		updateModuleInventory();
		_moduleInventory.writeToNBT(tag, "chassi");
	}

	@Override
	public void onAllowedRemoval() {
		_moduleInventory.removeListener(this);
		if (MainProxy.isServer(getWorld())) {
			for (int i = 0; i < getChassisSize(); i++) {
				LogisticsModule x = getSubModule(i);
				if (x instanceof ILegacyActiveModule) {
					ILegacyActiveModule y = (ILegacyActiveModule) x;
					y.onBlockRemoval();
				}
			}
			updateModuleInventory();
			_moduleInventory.dropContents(getWorld(), getX(), getY(), getZ());

			for (int i = 0; i < getChassisSize(); i++) {
				getModuleUpgradeManager(i).dropUpgrades();
			}
		}
	}

	@Override
	public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		if (MainProxy.isServer(getWorld())) {
			if (info instanceof ChassiTargetInformation) {
				ChassiTargetInformation target = (ChassiTargetInformation) info;
				LogisticsModule module = getSubModule(target.moduleSlot);
				if (module instanceof IRequireReliableTransport) {
					((IRequireReliableTransport) module).itemArrived(item, info);
				}
			} else {
				if (LogisticsPipes.isDEBUG() && info != null) {
					LogisticsPipes.log.warn("[ItemArrived] info not for chassis pipe: {}", item, new RuntimeException("stack trace"));
				}
			}
		}
	}

	@Override
	public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		if (MainProxy.isServer(getWorld())) {
			if (info instanceof ChassiTargetInformation) {
				ChassiTargetInformation target = (ChassiTargetInformation) info;
				LogisticsModule module = getSubModule(target.moduleSlot);
				if (module instanceof IRequireReliableTransport) {
					((IRequireReliableTransport) module).itemLost(item, info);
				}
			} else {
				if (LogisticsPipes.isDEBUG()) {
					LogisticsPipes.log.warn("[ItemLost] info not for chassis pipe: {}", item, new RuntimeException("stack trace"));
				}
			}
		}
	}

	@Override
	public int addToBuffer(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		if (MainProxy.isServer(getWorld())) {
			if (info instanceof ChassiTargetInformation) {
				ChassiTargetInformation target = (ChassiTargetInformation) info;
				LogisticsModule module = getSubModule(target.moduleSlot);
				if (module instanceof IBufferItems) {
					return ((IBufferItems) module).addToBuffer(item, info);
				}
			} else {
				if (LogisticsPipes.isDEBUG()) {
					LogisticsPipes.log.warn("[AddToBuffer] info not for chassis pipe: {}", item, new RuntimeException("stack trace"));
				}
			}
		}
		return item.getStackSize();
	}

	@Override
	public void InventoryChanged(Container inventory) {
		boolean reInitGui = false;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) {
				if (_module.hasModule(i)) {
					_module.removeModule(i);
					reInitGui = true;
				}
				continue;
			}

			final Item stackItem = stack.getItem();
			if (stackItem instanceof ItemModule) {
				final ItemModule moduleItem = (ItemModule) stackItem;
				LogisticsModule current = _module.getModule(i);
				LogisticsModule next = moduleItem.getModuleForItem(stack, current, this, this);
				Objects.requireNonNull(next, "getModuleForItem returned null for " + stack);
				next.registerPosition(ModulePositionType.SLOT, i);
				if (current != next) {
					_module.installModule(i, next);
					if (!MainProxy.isClient(getWorld())) {
						ItemModuleInformationManager.readInformation(stack, next);
					}
					next.finishInit();
				}
				inventory.setItem(i, stack);
			}
		}
		if (reInitGui) {
			if (MainProxy.isClient(getWorld())) {
				if (Minecraft.getInstance().screen instanceof GuiChassisPipe) {
					Minecraft.getInstance().setScreen(Minecraft.getInstance().screen); // re-init screen (1.20.1: init() is no longer public no-arg)
				}
			}
		}
		if (MainProxy.isServer(getWorld())) {
			if (!localModeWatchers.isEmpty()) {
				MainProxy.sendToPlayerList(PacketHandler.getPacket(ChassisPipeModuleContent.class)
						.setIdentList(ItemIdentifierStack.getListFromInventory(_moduleInventory))
						.setPosX(getX()).setPosY(getY()).setPosZ(getZ()),
						localModeWatchers);
			}
		}
	}

	@Override
	public void ignoreDisableUpdateEntity() {
		if (!init) {
			init = true;
			if (MainProxy.isClient(getWorld())) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestChassisOrientationPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
			}
		}
	}

	@Override
	public final @Nullable LogisticsModule getLogisticsModule() {
		return _module;
	}

	@Nonnull
	@Override
	public TransportLayer getTransportLayer() {
		if (_transportLayer == null) {
			_transportLayer = new ChassisTransportLayer(this);
		}
		return _transportLayer;
	}

	private boolean tryInsertingModule(Player entityplayer) {
		updateModuleInventory();
		for (int i = 0; i < _moduleInventory.getContainerSize(); i++) {
			if (_moduleInventory.getIDStackInSlot(i) == null) {
				_moduleInventory.setItem(i, entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).split(1));
				InventoryChanged(_moduleInventory);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean handleClick(Player entityplayer, SecuritySettings settings) {
		if (entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
			return false;
		}

		if (entityplayer.isCrouching() && SimpleServiceLocator.configToolHandler.canWrench(entityplayer, entityplayer.getItemBySlot(EquipmentSlot.MAINHAND), container)) {
			if (MainProxy.isServer(getWorld())) {
				if (settings == null || settings.openGui) {
					((PipeLogisticsChassis) container.pipe).nextOrientation();
				} else {
					entityplayer.sendSystemMessage(Component.translatable("lp.chat.permissiondenied"));
				}
			}
			SimpleServiceLocator.configToolHandler.wrenchUsed(entityplayer, entityplayer.getItemBySlot(EquipmentSlot.MAINHAND), container);
			return true;
		}

		if (!entityplayer.isCrouching() && entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).getItem() instanceof ItemModule) {
			if (MainProxy.isServer(getWorld())) {
				if (settings == null || settings.openGui) {
					return tryInsertingModule(entityplayer);
				} else {
					entityplayer.sendSystemMessage(Component.translatable("lp.chat.permissiondenied"));
				}
			}
			return true;
		}

		return false;
	}

	/*** IProvideItems ***/
	@Override
	public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters) {
		if (!isEnabled()) {
			return;
		}
		for (IFilter filter : filters) {
			if (filter.isBlocked() == filter.isFilteredItem(tree.getRequestType()) || filter.blockProvider()) {
				return;
			}
		}
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);
			if (x instanceof ILegacyActiveModule) {
				ILegacyActiveModule y = (ILegacyActiveModule) x;
				y.canProvide(tree, root, filters);
			}
		}
	}

	@Override
	public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination, IAdditionalTargetInformation info) {
		if (!isEnabled()) {
			return null;
		}
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);
			if (x instanceof ILegacyActiveModule) {
				ILegacyActiveModule y = (ILegacyActiveModule) x;
				LogisticsOrder result = y.fullFill(promise, destination, info);
				if (result != null) {
					spawnParticle(Particles.WhiteParticle, 2);
					return result;
				}
			}
		}
		return null;
	}

	@Override
	public void getAllItems(Map<ItemIdentifier, Integer> list, List<IFilter> filter) {
		if (!isEnabled()) {
			return;
		}
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);
			if (x instanceof ILegacyActiveModule) {
				ILegacyActiveModule y = (ILegacyActiveModule) x;
				y.getAllItems(list, filter);
			}
		}
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public net.minecraft.world.level.Level getLevelForHUD() {
		return getWorld();
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return hud;
	}

	@Override
	public void startWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartWatchingPacket.class).setInteger(1).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void stopWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopWatchingPacket.class).setInteger(1).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
		hud.stopWatching();
	}

	@Override
	public void playerStartWatching(Player player, int mode) {
		if (mode == 1) {
			updateModuleInventory();
			localModeWatchers.add(player);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ChassisPipeModuleContent.class).setIdentList(ItemIdentifierStack.getListFromInventory(_moduleInventory)).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), player);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SendQueueContent.class).setIdentList(ItemIdentifierStack.getListSendQueue(_sendQueue)).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), player);
		} else {
			super.playerStartWatching(player, mode);
		}
	}

	@Override
	public void playerStopWatching(Player player, int mode) {
		super.playerStopWatching(player, mode);
		localModeWatchers.remove(player);
	}

	public void handleModuleItemIdentifierList(Collection<ItemIdentifierStack> _allItems) {
		_moduleInventory.handleItemIdentifierList(_allItems);
	}

	@Override
	public int sendQueueChanged(boolean force) {
		if (MainProxy.isServer(getWorld())) {
			if (Configs.MULTI_THREAD_NUMBER > 0 && !force) {
				HudUpdateTick.add(getRouter());
			} else {
				if (localModeWatchers.size() > 0) {
					LinkedList<ItemIdentifierStack> items = ItemIdentifierStack.getListSendQueue(_sendQueue);
					MainProxy.sendToPlayerList(PacketHandler.getPacket(SendQueueContent.class).setIdentList(items).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), localModeWatchers);
					return items.size();
				}
			}
		}
		return 0;
	}

	@Override
	public void handleSendQueueItemIdentifierList(Collection<ItemIdentifierStack> _allItems) {
		displayList.clear();
		displayList.addAll(_allItems);
	}

	public ChassisModule getModules() {
		return _module;
	}

	@Override
	public void setTile(BlockEntity tile) {
		super.setTile(tile);
		_module.slottedModules().forEach(SlottedModule::registerPosition);
	}

	@Override
	public int getSourceID() {
		return getRouterId();
	}

	@Override
	public void collectSpecificInterests(@Nonnull Collection<ItemIdentifier> itemIdentifiers) {
		// if we don't have a pointed inventory we can't be interested in anything
		if (pointedAdjacentProperty.getValue().inventories().isEmpty()) {
			return;
		}

		for (int moduleIndex = 0; moduleIndex < getChassisSize(); moduleIndex++) {
			LogisticsModule module = getSubModule(moduleIndex);
			if (module != null && module.interestedInAttachedInventory()) {
				final ISlotUpgradeManager upgradeManager = getUpgradeManager(module.getSlot(), module.getPositionInt());
				IInventoryUtil inv = PipeServiceProviderUtilKt.availableSneakyInventories(this, upgradeManager).stream().findFirst().orElse(null);
				if (inv == null) {
					continue;
				}
				Set<ItemIdentifier> items = inv.getItems();
				itemIdentifiers.addAll(items);

				//also add tag-less variants ... we should probably add a module.interestedIgnoringNBT at some point
				items.stream().map(ItemIdentifier::getIgnoringNBT).forEach(itemIdentifiers::add);

				boolean modulesInterestedInUndamaged = false;
				for (int i = 0; i < getChassisSize(); i++) {
					if (getSubModule(moduleIndex).interestedInUndamagedID()) {
						modulesInterestedInUndamaged = true;
						break;
					}
				}
				if (modulesInterestedInUndamaged) {
					items.stream().map(ItemIdentifier::getUndamaged).forEach(itemIdentifiers::add);
				}
				break; // no need to check other modules for interest in the inventory, when we know that 1 already is.
			}
		}
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule module = getSubModule(i);
			if (module != null) {
				module.collectSpecificInterests(itemIdentifiers);
			}
		}
	}

	@Override
	public boolean hasGenericInterests() {
		if (pointedAdjacentProperty.getValue().inventories().isEmpty()) {
			return false;
		}
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);

			if (x != null && x.hasGenericInterests()) {
				return true;
			}
		}
		return false;
	}

	@CCCommand(description = "Returns the LogisticsModule for the given slot number starting by 1")
	public LogisticsModule getModuleInSlot(Double i) {
		return getSubModule((int) (i - 1));
	}

	@CCCommand(description = "Returns the size of this Chassis pipe")
	public Integer getChassieSize() {
		return getChassisSize();
	}

	/** ICraftItems */
	public final LinkedList<LogisticsOrder> _extras = new LinkedList<>();

	@Override
	public void registerExtras(IPromise promise) {
		if (!(promise instanceof LogisticsPromise)) {
			throw new UnsupportedOperationException("Extra has to be an item for a chassis pipe");
		}
		ItemIdentifierStack stack = new ItemIdentifierStack(((LogisticsPromise) promise).item, ((LogisticsPromise) promise).numberOfItems);
		_extras.add(new LogisticsItemOrder(new DictResource(stack, null), null, ResourceType.EXTRA, null));
	}

	@Override
	public ICraftingTemplate addCrafting(IResource toCraft) {
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);

			if (x instanceof ICraftItems) {
				if (((ICraftItems) x).canCraft(toCraft)) {
					return ((ICraftItems) x).addCrafting(toCraft);
				}
			}
		}
		return null;

		// trixy code goes here to ensure the right crafter answers the right request
	}

	@Override
	public List<ItemIdentifierStack> getCraftedItems() {
		List<ItemIdentifierStack> craftables = null;
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);

			if (x instanceof ICraftItems) {
				if (craftables == null) {
					craftables = new LinkedList<>();
				}
				craftables.addAll(((ICraftItems) x).getCraftedItems());
			}
		}
		return craftables;
	}

	@Override
	public boolean canCraft(IResource toCraft) {
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);

			if (x instanceof ICraftItems) {
				if (((ICraftItems) x).canCraft(toCraft)) {
					return true;
				}
			}
		}
		return false;
	}

	@Nonnull
	@Override
	public ISlotUpgradeManager getUpgradeManager(ModulePositionType slot, int positionInt) {
		if (slot != ModulePositionType.SLOT || positionInt >= getChassisSize()) {
			if (LogisticsPipes.isDEBUG()) {
				new UnsupportedOperationException("Position info aren't for a chassis pipe. (" + slot + "/" + positionInt + ")").printStackTrace();
			}
			return super.getUpgradeManager(slot, positionInt);
		}
		return _module.getModuleUpgradeManager(positionInt);
	}

	@Override
	public int getTodo() {
		// probably not needed, the chassis order manager handles the count, would need to store origin to specifically know this.
		return 0;
	}

	@Nullable
	public LogisticsModule getSubModule(int slot) {
		return _module.getModule(slot);
	}

	public static class ChassiTargetInformation implements IAdditionalTargetInformation {

		@Getter
		private final int moduleSlot;

		public ChassiTargetInformation(int slot) {
			moduleSlot = slot;
		}
	}
}
