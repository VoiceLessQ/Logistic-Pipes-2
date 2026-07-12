/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.gui;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IFuzzySlot;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.ISlotCheck;
import logisticspipes.interfaces.ISlotClick;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.items.ItemModule;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.ChassisModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.FuzzySlotSettingsPacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.upgrades.UpgradeManager;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.MinecraftColor;
import logisticspipes.utils.ReflectionHelper;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.property.IBitSet;

public class DummyContainer extends AbstractContainerMenu {

	@OnlyIn(Dist.CLIENT)
	public LogisticsBaseGuiScreen guiHolderForJEI; // This is not set for every GUI. Only for the one needed by JEI.

	public List<BitSet> slotsFuzzyFlags = new ArrayList<>();
	protected Container _playerInventory;
	protected Container _dummyInventory;
	protected IGuiOpenControler[] _controler;
	boolean wasDummyLookup;
	boolean overrideMCAntiSend;
	private List<Slot> transferTop = new ArrayList<>();
	private List<Slot> transferBottom = new ArrayList<>();
	private long lastClicked;
	private long lastDragnDropLockup;
	// LP-maintained listener list (containerListeners is private in 1.20.1 AbstractContainerMenu)
	private final List<ContainerListener> lpListeners = new ArrayList<>();

	public DummyContainer(Container playerInventory, Container dummyInventory) {
		super(null, 0);
		_playerInventory = playerInventory;
		_dummyInventory = dummyInventory;
		_controler = null;
	}

	public DummyContainer(Player player, Container dummyInventory, IGuiOpenControler... controler) {
		super(null, 0);
		_playerInventory = player.getInventory();
		_dummyInventory = dummyInventory;
		_controler = controler;
		if (MainProxy.isServer(player.level())) {
			for (IGuiOpenControler element : _controler) {
				element.guiOpenedByPlayer(player);
			}
		}
	}

	@Override
	public boolean stillValid(@Nonnull Player entityplayer) {
		return true;
	}

	/***
	 * Adds all slots for the player inventory and hotbar
	 */
	public void addNormalSlotsForPlayerInventory(int xOffset, int yOffset) {
		if (_playerInventory == null) {
			return;
		}
		// Player "backpack"
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				Slot slot = new Slot(_playerInventory, column + row * 9 + 9, xOffset + column * 18, yOffset + row * 18);
				addSlot(slot);
				transferBottom.add(slot);
			}
		}

		// Player "hotbar"
		for (int i1 = 0; i1 < 9; i1++) {
			Slot slot = new Slot(_playerInventory, i1, xOffset + i1 * 18, yOffset + 58);
			addSlot(slot);
			transferBottom.add(slot);
		}
	}

	/**
	 * Add a dummy slot that will not consume players items
	 *
	 * @param slotId
	 *            The slot number in the dummy AbstractContainerMenu this slot should map
	 * @param xCoord
	 *            xCoord of TopLeft corner of where the slot should be rendered
	 * @param yCoord
	 *            yCoord of TopLeft corner of where the slot should be rendered
	 */
	public Slot addDummySlot(int slotId, int xCoord, int yCoord) {
		return addSlot(new DummySlot(_dummyInventory, slotId, xCoord, yCoord));
	}

	public Slot addDummySlot(int slotId, Container dummy, int xCoord, int yCoord) {
		return addSlot(new DummySlot(dummy, slotId, xCoord, yCoord));
	}

	public void addNormalSlot(int slotId, Container inventory, int xCoord, int yCoord) {
		transferTop.add(addSlot(new Slot(inventory, slotId, xCoord, yCoord)));
	}

	public Slot addRestrictedSlot(int slotId, Container inventory, int xCoord, int yCoord, Class<? extends Item> itemClass) {
		return addSlot(new RestrictedSlot(inventory, slotId, xCoord, yCoord, itemClass));
	}

	public Slot addRestrictedSlot(int slotId, Container inventory, int xCoord, int yCoord, Item item) {
		return addSlot(new RestrictedSlot(inventory, slotId, xCoord, yCoord, item));
	}

	public Slot addStaticRestrictedSlot(int slotId, Container inventory, int xCoord, int yCoord, Item item, int stackLimit) {
		return addSlot(new StaticRestrictedSlot(inventory, slotId, xCoord, yCoord, item, stackLimit));
	}

	public Slot addRestrictedSlot(int slotId, Container inventory, int xCoord, int yCoord, ISlotCheck slotCheck) {
		return addSlot(new RestrictedSlot(inventory, slotId, xCoord, yCoord, slotCheck));
	}

	public Slot addStaticRestrictedSlot(int slotId, Container inventory, int xCoord, int yCoord, ISlotCheck slotCheck, int stackLimit) {
		return addSlot(new StaticRestrictedSlot(inventory, slotId, xCoord, yCoord, slotCheck, stackLimit));
	}

	public void addModuleSlot(int slotId, Container inventory, int xCoord, int yCoord, PipeLogisticsChassis pipe) {
		transferTop.add(addSlot(new ModuleSlot(inventory, slotId, xCoord, yCoord, pipe)));
	}

	public Slot addFluidSlot(int slotId, int xCoord, int yCoord) {
		return addSlot(new FluidSlot(_dummyInventory, slotId, xCoord, yCoord));
	}

	public Slot addFluidSlot(int slotId, Container inventory, int xCoord, int yCoord) {
		return addSlot(new FluidSlot(inventory, slotId, xCoord, yCoord));
	}

	public Slot addColorSlot(int slotId, Container inventory, int xCoord, int yCoord) {
		return addSlot(new ColorSlot(inventory, slotId, xCoord, yCoord));
	}

	public Slot addUnmodifiableSlot(int slotId, Container inventory, int xCoord, int yCoord) {
		return addSlot(new UnmodifiableSlot(inventory, slotId, xCoord, yCoord));
	}

	public Slot addCallableSlotHandler(int slotId, Container inventory, int xCoord, int yCoord, ISlotClick handler) {
		return addSlot(new HandelableSlot(inventory, slotId, xCoord, yCoord, handler));
	}

	public Slot addFuzzyDummySlot(int slotId, int xCoord, int yCoord, IBitSet fuzzyFlags) {
		return addSlot(new FuzzyDummySlot(_dummyInventory, slotId, xCoord, yCoord, fuzzyFlags));
	}

	public Slot addFuzzyUnmodifiableSlot(int slotId, Container inventory, int xCoord, int yCoord, IBitSet fuzzyFlags) {
		return addSlot(new FuzzyUnmodifiableSlot(inventory, slotId, xCoord, yCoord, fuzzyFlags));
	}

	public Slot addUpgradeSlot(int slotId, ISlotUpgradeManager manager, int upgradeSlotId, int xCoord, int yCoord, ISlotCheck slotCheck) {
		Slot slot = addSlot(new UpgradeSlot(manager, upgradeSlotId, slotId, xCoord, yCoord, slotCheck));
		transferTop.add(slot);
		return slot;
	}

	public Slot addSneakyUpgradeSlot(int slotId, UpgradeManager manager, int upgradeSlotId, int xCoord, int yCoord, ISlotCheck slotCheck) {
		Slot slot = addSlot(new SneakyUpgradeSlot(manager, upgradeSlotId, slotId, xCoord, yCoord, slotCheck));
		transferTop.add(slot);
		return slot;
	}

	@Nonnull
	@Override
	public ItemStack quickMoveStack(@Nonnull Player player, int i) {
		if (transferTop.isEmpty() || transferBottom.isEmpty()) {
			return ItemStack.EMPTY;
		}
		Slot slot = this.slots.get(i);
		if (slot == null || slot instanceof DummySlot || slot instanceof UnmodifiableSlot || slot instanceof FluidSlot || slot instanceof ColorSlot || slot instanceof HandelableSlot || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		if (transferTop.contains(slot)) {
			handleShiftClickLists(slot, transferBottom, true, player);
			handleShiftClickLists(slot, transferBottom, false, player);
		} else if (transferBottom.contains(slot)) {
			handleShiftClickLists(slot, transferTop, true, player);
			handleShiftClickLists(slot, transferTop, false, player);
		}
		return ItemStack.EMPTY;
	}

	private void handleShiftClickLists(Slot from, List<Slot> toList, boolean ignoreEmpty, Player player) {
		if (!from.hasItem()) {
			return;
		}
		for (Slot to : toList) {
			if (handleShiftClickForSlots(from, to, ignoreEmpty, player)) {
				return;
			}
		}
	}

	private boolean handleShiftClickForSlots(Slot from, Slot to, boolean ignoreEmpty, Player player) {
		if (!from.hasItem()) {
			return true;
		}
		ItemStack out = from.getItem();
		if (!to.hasItem() && !ignoreEmpty && to.mayPlace(out)) {
			boolean remove = true;
			if (out.getCount() > to.getMaxStackSize()) {
				out = from.remove(to.getMaxStackSize());
				remove = false;
			}
			from.onTake(player, out);
			to.set(out);
			if (remove) {
				from.set(ItemStack.EMPTY);
			}
			return true;
		}
		if (from instanceof ModuleSlot || to instanceof ModuleSlot) {
			return false;
		}
		from.onTake(player, out);
		if (to.hasItem() && ItemStack.isSameItem(to.getItem(), out) && ItemStack.isSameItemSameComponents(to.getItem(), from.getItem())) {
			int free = Math.min(to.getMaxStackSize(), to.getItem().getMaxStackSize()) - to.getItem().getCount();
			if (free > 0) {
				ItemStack toInsert = from.remove(free);
				from.onTake(player, toInsert);
				ItemStack toStack = to.getItem();
				if (!toInsert.isEmpty() && !toStack.isEmpty()) {
					toStack.grow(toInsert.getCount());
					to.set(toStack);
					return !from.hasItem();
				}
			}
		}
		return false;
	}

	public void superSlotClick(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		super.clicked(slotId, dragType, clickTypeIn, player);
	}

private void handleSwitch(Slot slot2, @Nonnull ItemStack out, @Nonnull ItemStack in, Player player) {
		if (slot2 instanceof ModuleSlot) {
			ChassisModule chassis = (ChassisModule) ((ModuleSlot) slot2).get_pipe().getLogisticsModule();
			int moduleIndex = ((ModuleSlot) slot2).get_moduleIndex();
			if (out.getItem() instanceof ItemModule) {
				if (chassis.hasModule(moduleIndex)) {
					ItemModuleInformationManager.saveInformation(out, chassis.getModule(moduleIndex));
					chassis.removeModule(moduleIndex);
				}
			}
		}
	}

	/**
	 * Clone/clear itemstacks for items
	 */
	@Override
	public void clicked(int slotId, int mouseButton, @Nonnull ClickType shiftMode, @Nonnull Player player) {
		lastClicked = System.currentTimeMillis();
		if (slotId < 0) {
			superSlotClick(slotId, mouseButton, shiftMode, player);
			return;
		}
		Slot slot = this.slots.get(slotId);
		//debug dump
		if (LogisticsPipes.isDEBUG() && slot != null) {
			ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				ItemIdentifier.get(stack).debugDumpData(player.level().isClientSide);
			}
		}
		if (slot == null) return;
		if ((!(slot instanceof DummySlot) && !(slot instanceof UnmodifiableSlot) && !(slot instanceof FluidSlot) && !(slot instanceof ColorSlot) && !(slot instanceof HandelableSlot))) {
			superSlotClick(slotId, mouseButton, shiftMode, player);
			ItemStack stack2 = slot.getItem();
			if (!stack2.isEmpty() && stack2.getItem() instanceof ItemModule) {
				if (player instanceof ServerPlayer && MainProxy.isServer(player.level())) {
					((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(
						this.containerId, this.incrementStateId(), slotId, stack2));
				}
			}
			return;
		}

		ItemStack currentlyEquippedStack = this.getCarried();

		// we get a leftclick *and* a doubleclick message if there's a doubleclick with no item on the pointer, filter it out
		if (currentlyEquippedStack.isEmpty() && shiftMode == ClickType.PICKUP_ALL) {
			return;
		}

		if (slot instanceof HandelableSlot) {
			overrideMCAntiSend = true;
			if (currentlyEquippedStack.isEmpty()) {
				this.setCarried(((HandelableSlot) slot).getProvidedStack());
			}
			return;
		}

		if (slot instanceof UnmodifiableSlot) {
			return;
		}

		handleDummyClick(slot, slotId, currentlyEquippedStack, mouseButton, shiftMode, player);
	}

	public void handleDummyClick(Slot slot, int slotId, @Nonnull ItemStack currentlyEquippedStack, int mouseButton, ClickType shiftMode, Player entityplayer) {
		if (slot instanceof FluidSlot) {
			if (!currentlyEquippedStack.isEmpty()) {
				FluidIdentifier ident = FluidIdentifier.get(currentlyEquippedStack);
				if (ident != null) {
					if (mouseButton == 0) {
						slot.set(ident.getItemIdentifier().unsafeMakeNormalStack(1));
					} else {
						slot.set(ItemStack.EMPTY);
					}
					return;
				}
			}
			FluidIdentifier ident = null;
			if (!slot.getItem().isEmpty()) {
				ident = FluidIdentifier.get(ItemIdentifier.get(slot.getItem()));
			}
			if (ident == null) {
				if (MainProxy.isClient(entityplayer.level())) {
					MainProxy.proxy.openFluidSelectGui(slotId);
				}
			}
			slot.set(ItemStack.EMPTY);
			return;
		}

		if (slot instanceof ColorSlot) {
			MinecraftColor equipped = MinecraftColor.getColor(currentlyEquippedStack);
			MinecraftColor color = MinecraftColor.getColor(slot.getItem());
			if (MinecraftColor.BLANK.equals(equipped)) {
				if (mouseButton == 0) {
					color = color.getNext();
				} else if (mouseButton == 1) {
					color = color.getPrev();
				} else {
					color = MinecraftColor.BLANK;
				}
				slot.set(color.getItemStack());
			} else {
				if (mouseButton == 1) {
					slot.set(MinecraftColor.BLANK.getItemStack());
				} else {
					slot.set(equipped.getItemStack());
				}
			}
			if (entityplayer instanceof ServerPlayer && MainProxy.isServer(entityplayer.level())) {
				((ServerPlayer) entityplayer).connection.send(new ClientboundContainerSetSlotPacket(
					this.containerId, this.incrementStateId(), slotId, slot.getItem()));
			}
			return;
		}

		if (slot instanceof DummySlot) {
			((DummySlot) slot).setRedirectCall(true);
		}

		if (mouseButton >= 1000) {
			if (mouseButton <= 1001) {
				if (slot.hasItem()) {
					ItemStack stack = slot.getItem().copy();
					if (mouseButton == 1000) {
						stack.grow(1);
					} else if (stack.getCount() > 1) {
						stack.shrink(1);
					}
					stack.setCount(Math.min(slot.getMaxStackSize(), Math.max(1, stack.getCount())));
					slot.set(stack);
				}
				if (slot instanceof DummySlot) {
					((DummySlot) slot).setRedirectCall(false);
				}
				return;
			}
		}

		if (currentlyEquippedStack.isEmpty()) {
			if (!slot.getItem().isEmpty() && mouseButton == 1) {
				ItemStack tstack = slot.getItem();
				if (shiftMode == ClickType.QUICK_MOVE) {
					tstack.setCount(Math.min(slot.getMaxStackSize(), tstack.getCount() * 2));
				} else {
					tstack.setCount(tstack.getCount() / 2);
				}
				slot.set(tstack);
			} else {
				slot.set(ItemStack.EMPTY);
			}
			if (slot instanceof DummySlot) {
				((DummySlot) slot).setRedirectCall(false);
			}
			return;
		}

		if (!slot.hasItem()) {
			ItemStack tstack = currentlyEquippedStack.copy();
			if (mouseButton == 1) {
				tstack.setCount(1);
			}
			if (tstack.getCount() > slot.getMaxStackSize()) {
				tstack.setCount(slot.getMaxStackSize());
			}
			slot.set(tstack);
			if (slot instanceof DummySlot) {
				((DummySlot) slot).setRedirectCall(false);
			}
			return;
		}

		ItemIdentifier currentItem = ItemIdentifier.get(currentlyEquippedStack);
		ItemIdentifier slotItem = ItemIdentifier.get(slot.getItem());
		if (currentItem.equals(slotItem)) {
			ItemStack tstack = slot.getItem();
			// Do manual shift-checking to play nice with NEI
			int counter = shiftMode == ClickType.QUICK_MOVE ? 10 : 1;
			if (mouseButton == 1) {
				if (tstack.getCount() + counter <= slot.getMaxStackSize()) {
					tstack.grow(counter);
				} else {
					tstack.setCount(slot.getMaxStackSize());
				}
				slot.set(tstack);
			} else if (mouseButton == 0) {
				tstack.shrink(counter);
				slot.set(tstack);
			}
			if (slot instanceof DummySlot) {
				((DummySlot) slot).setRedirectCall(false);
			}
			return;
		}

		ItemStack tstack = currentlyEquippedStack.copy();
		if (tstack.getCount() > slot.getMaxStackSize()) {
			tstack.setCount(slot.getMaxStackSize());
		}
		slot.set(tstack);
		if (slot instanceof DummySlot) {
			((DummySlot) slot).setRedirectCall(false);
		}
	}

	@Override
	public void removed(@Nonnull Player player) {
		if (_controler != null) {
			for (IGuiOpenControler element : _controler) {
				element.guiClosedByPlayer(player);
			}
		}
		super.removed(player);
	}

	public void addRestrictedHotbarForPlayerInventory(int xOffset, int yOffset) {
		if (_playerInventory == null) {
			return;
		}
		// Player "hotbar"
		for (int i1 = 0; i1 < 9; i1++) {
			addSlot(new UnmodifiableSlot(_playerInventory, i1, xOffset + i1 * 18, yOffset));
		}
	}

	public void addRestrictedArmorForPlayerInventory(int xOffset, int yOffset) {
		if (_playerInventory == null) {
			return;
		}
		for (int i1 = 0; i1 < 4; i1++) {
			addSlot(new UnmodifiableSlot(_playerInventory, i1 + 36, xOffset, yOffset - i1 * 18));
		}
	}

	// @Override // canDragIntoSlot may not be in AbstractContainerMenu in 1.20.1
	public boolean canDragIntoSlot(@Nonnull Slot slot) {
		if (slot instanceof UnmodifiableSlot || slot instanceof FluidSlot || slot instanceof ColorSlot || slot instanceof HandelableSlot) {
			return false;
		}
		if (lastDragnDropLockup <= lastClicked) { // Slot was clicked after last lookup
			lastDragnDropLockup = System.currentTimeMillis();
			if (slot instanceof DummySlot) {
				wasDummyLookup = true;
				return true;
			}
			wasDummyLookup = false;
			return true;
		} else { // Still lookingUp (during drag'n'drop)
			lastDragnDropLockup = System.currentTimeMillis();
			if (slot instanceof DummySlot) {
				return wasDummyLookup;
			}
			return !wasDummyLookup;
		}
	}

	// getSlotFromInventory removed in 1.20.1 — no override needed

	@Override
	public void setItem(int par1, int stateId, @Nonnull ItemStack par2ItemStack) {
		if (this.slots.isEmpty()) {
			_playerInventory.setItem(par1, par2ItemStack);
			_playerInventory.setChanged();
			return;
		}
		super.setItem(par1, stateId, par2ItemStack);
	}

	@Override
	public void broadcastChanges() {
		// Sync fuzzy slot flags to listeners before letting vanilla handle item sync
		for (int i = 0; i < this.slots.size(); ++i) {
			if (this.slots.get(i) instanceof IFuzzySlot) {
				IFuzzySlot fuzzySlot = (IFuzzySlot) this.slots.get(i);
				BitSet slotFlags = fuzzySlot.getFuzzyFlags().copyValue();
				BitSet savedFlags = slotsFuzzyFlags.get(i);
				if (savedFlags == null || !savedFlags.equals(slotFlags)) {
					MainProxy.sendToPlayerList(
							PacketHandler.getPacket(FuzzySlotSettingsPacket.class)
									.setSlotNumber(fuzzySlot.getSlotId())
									.setFlags(slotFlags),
							getListeners().stream().filter(o -> o instanceof Player).map(o -> (Player) o));
					slotsFuzzyFlags.set(i, slotFlags);
				}
			}
		}
		// isChangingQuantityOnly hack removed — field no longer exists in 1.20.1 ServerPlayer
		overrideMCAntiSend = false;
		super.broadcastChanges();
	}

	@Nonnull
	@Override
	protected Slot addSlot(@Nonnull Slot slotIn) {
		this.slotsFuzzyFlags.add(null);
		return super.addSlot(slotIn);
	}

	@Override
	public void addSlotListener(@Nonnull ContainerListener listener) {
		super.addSlotListener(listener);
		lpListeners.add(listener);
	}

	@Override
	public void removeSlotListener(@Nonnull ContainerListener listener) {
		super.removeSlotListener(listener);
		lpListeners.remove(listener);
	}

	protected List<ContainerListener> getListeners() {
		return lpListeners;
	}
}
