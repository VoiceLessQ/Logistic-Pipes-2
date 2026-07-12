package logisticspipes.pipes.upgrades;

import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import logisticspipes.LPItems;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IPipeUpgradeManager;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.power.BCPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.IC2PowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.power.RFPowerSupplierUpgrade;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.SimpleStackInventory;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class UpgradeManager implements ISimpleInventoryEventHandler, ISlotUpgradeManager, IPipeUpgradeManager {

	public final SimpleStackInventory inv = new SimpleStackInventory(9, "UpgradeInventory", 16);
	public final SimpleStackInventory sneakyInv = new SimpleStackInventory(9, "SneakyUpgradeInventory", 1);
	public final SimpleStackInventory secInv = new SimpleStackInventory(1, "SecurityInventory", 16);
	private IPipeUpgrade[] upgrades = new IPipeUpgrade[9];
	private IPipeUpgrade[] sneakyUpgrades = new IPipeUpgrade[9];
	private CoreRoutedPipe pipe;
	private int securityDelay = 0;

	/* cached attributes */
	private Direction sneakyOrientation = null;
	private Direction[] combinedSneakyOrientation = new Direction[9];
	private int speedUpgradeCount = 0;
	private final EnumSet<Direction> disconnectedSides = EnumSet.noneOf(Direction.class);
	private boolean isAdvancedCrafter = false;
	private boolean isFuzzyUpgrade = false;
	private boolean isCombinedSneakyUpgrade = false;
	private int liquidCrafter = 0;
	private boolean hasByproductExtractor = false;
	private UUID uuid = null;
	private String uuidS = null;
	private boolean hasPatternUpgrade = false;
	private boolean hasPowerPassUpgrade = false;
	private boolean hasRFPowerUpgrade = false;
	private boolean hasBCPowerUpgrade = false;
	private int getIC2PowerLevel = 0;
	private boolean hasCCRemoteControlUpgrade = false;
	private boolean hasCraftingMonitoringUpgrade = false;
	private boolean hasOpaqueUpgrade = false;
	private int craftingCleanup = 0;
	private boolean hasLogicControll = false;
	private boolean hasUpgradeModuleUpgarde = false;
	private int actionSpeedUpgrade = 0;
	private int itemExtractionUpgrade = 0;
	private int itemStackExtractionUpgrade = 0;

	private boolean[] guiUpgrades = new boolean[18];

	private boolean needsContainerPositionUpdate = false;

	public UpgradeManager(CoreRoutedPipe pipe) {
		this.pipe = pipe;
		inv.addListener(this);
		sneakyInv.addListener(this);
		secInv.addListener(this);
	}

	public void readFromNBT(CompoundTag nbttagcompound) {
		inv.readFromNBT(nbttagcompound, "UpgradeInventory_");
		sneakyInv.readFromNBT(nbttagcompound, "SneakyUpgradeInventory_");
		secInv.readFromNBT(nbttagcompound, "SecurityInventory_");

		if (!sneakyInv.getItem(8).isEmpty()) {
			if (sneakyInv.getItem(8).getItem() == LPItems.itemCard.get() && sneakyInv.getItem(8).getDamageValue() == LogisticsItemCard.SEC_CARD) {
				secInv.setItem(0, sneakyInv.getItem(8));
				sneakyInv.setItem(8, ItemStack.EMPTY);
			}
		}

		InventoryChanged(inv);
	}

	public void writeToNBT(CompoundTag nbttagcompound) {
		inv.writeToNBT(nbttagcompound, "UpgradeInventory_");
		sneakyInv.writeToNBT(nbttagcompound, "SneakyUpgradeInventory_");
		secInv.writeToNBT(nbttagcompound, "SecurityInventory_");
		InventoryChanged(inv);
	}

	private boolean updateModule(int slot, IPipeUpgrade[] upgrades, Container inv) {
		ItemStack stack = inv.getItem(slot);
		if (stack.getItem() instanceof ItemUpgrade) {
			upgrades[slot] = ((ItemUpgrade) stack.getItem()).getUpgradeForItem(stack, upgrades[slot]);
		} else {
			upgrades[slot] = null;
		}
		if (upgrades[slot] == null) {
			inv.setItem(slot, ItemStack.EMPTY);
			return false;
		} else {
			return upgrades[slot].needsUpdate();
		}
	}

	private boolean removeUpgrade(int slot, IPipeUpgrade[] upgrades) {
		boolean needUpdate = upgrades[slot].needsUpdate();
		upgrades[slot] = null;
		return needUpdate;
	}

	@Override
	public void InventoryChanged(Container inventory) {
		boolean needUpdate = false;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack item = inv.getItem(i);
			if (!item.isEmpty()) {
				needUpdate |= updateModule(i, upgrades, inv);
			} else if (upgrades[i] != null) {
				needUpdate |= removeUpgrade(i, upgrades);
			}
		}
		//update sneaky direction, speed upgrade count and disconnection
		sneakyOrientation = null;
		speedUpgradeCount = 0;
		isAdvancedCrafter = false;
		isFuzzyUpgrade = false;
		boolean combinedBuffer = isCombinedSneakyUpgrade;
		isCombinedSneakyUpgrade = false;
		liquidCrafter = 0;
		disconnectedSides.clear();
		hasByproductExtractor = false;
		hasPatternUpgrade = false;
		hasPowerPassUpgrade = false;
		hasRFPowerUpgrade = false;
		hasBCPowerUpgrade = false;
		getIC2PowerLevel = 0;
		hasCCRemoteControlUpgrade = false;
		hasCraftingMonitoringUpgrade = false;
		hasOpaqueUpgrade = false;
		craftingCleanup = 0;
		hasLogicControll = false;
		hasUpgradeModuleUpgarde = false;
		actionSpeedUpgrade = 0;
		itemExtractionUpgrade = 0;
		itemStackExtractionUpgrade = 0;

		guiUpgrades = new boolean[18];
		for (int i = 0; i < upgrades.length; i++) {
			IPipeUpgrade upgrade = upgrades[i];
			if (upgrade instanceof SneakyUpgradeConfig && sneakyOrientation == null && !isCombinedSneakyUpgrade) {
				sneakyOrientation = ((SneakyUpgradeConfig) upgrade).getSide(getInv().getItem(i));
			} else if (upgrade instanceof SpeedUpgrade) {
				speedUpgradeCount += inv.getItem(i).getCount();
			} else if (upgrade instanceof ConnectionUpgradeConfig) {
				((ConnectionUpgradeConfig) upgrade).getSides(getInv().getItem(i)).forEach(disconnectedSides::add);
			} else if (upgrade instanceof AdvancedSatelliteUpgrade) {
				isAdvancedCrafter = true;
			} else if (upgrade instanceof FuzzyUpgrade) {
				isFuzzyUpgrade = true;
			} else if (upgrade instanceof CombinedSneakyUpgrade && sneakyOrientation == null) {
				isCombinedSneakyUpgrade = true;
			} else if (upgrade instanceof FluidCraftingUpgrade) {
				liquidCrafter += inv.getItem(i).getCount();
			} else if (upgrade instanceof CraftingByproductUpgrade) {
				hasByproductExtractor = true;
			} else if (upgrade instanceof PatternUpgrade) {
				hasPatternUpgrade = true;
			} else if (upgrade instanceof PowerTransportationUpgrade) {
				hasPowerPassUpgrade = true;
			} else if (upgrade instanceof RFPowerSupplierUpgrade) {
				hasRFPowerUpgrade = true;
			} else if (upgrade instanceof BCPowerSupplierUpgrade) {
				hasBCPowerUpgrade = true;
			} else if (upgrade instanceof IC2PowerSupplierUpgrade) {
				getIC2PowerLevel = Math.max(getIC2PowerLevel, ((IC2PowerSupplierUpgrade) upgrade).getPowerLevel());
			} else if (upgrade instanceof CCRemoteControlUpgrade) {
				hasCCRemoteControlUpgrade = true;
			} else if (upgrade instanceof CraftingMonitoringUpgrade) {
				hasCraftingMonitoringUpgrade = true;
			} else if (upgrade instanceof OpaqueUpgrade) {
				hasOpaqueUpgrade = true;
			} else if (upgrade instanceof CraftingCleanupUpgrade) {
				craftingCleanup += inv.getItem(i).getCount();
			} else if (upgrade instanceof LogicControllerUpgrade) {
				hasLogicControll = true;
			} else if (upgrade instanceof UpgradeModuleUpgrade) {
				hasUpgradeModuleUpgarde = true;
			} else if (upgrade instanceof ActionSpeedUpgrade) {
				actionSpeedUpgrade += inv.getItem(i).getCount();
			} else if (upgrade instanceof ItemExtractionUpgrade) {
				itemExtractionUpgrade += inv.getItem(i).getCount();
			} else if (upgrade instanceof ItemStackExtractionUpgrade) {
				itemStackExtractionUpgrade += inv.getItem(i).getCount();
			}
			if (upgrade instanceof IConfigPipeUpgrade) {
				guiUpgrades[i] = true;
			}
		}
		liquidCrafter = Math.min(liquidCrafter, ItemUpgrade.MAX_LIQUID_CRAFTER);
		craftingCleanup = Math.min(craftingCleanup, ItemUpgrade.MAX_CRAFTING_CLEANUP);
		itemExtractionUpgrade = Math.min(itemExtractionUpgrade, ItemUpgrade.MAX_ITEM_EXTRACTION);
		itemStackExtractionUpgrade = Math.min(itemStackExtractionUpgrade, ItemUpgrade.MAX_ITEM_STACK_EXTRACTION);
		if (combinedBuffer != isCombinedSneakyUpgrade) {
			needsContainerPositionUpdate = true;
		}
		for (int i = 0; i < sneakyInv.getContainerSize(); i++) {
			ItemStack item = sneakyInv.getItem(i);
			if (!item.isEmpty()) {
				needUpdate |= updateModule(i, sneakyUpgrades, sneakyInv);
			} else if (sneakyUpgrades[i] != null) {
				needUpdate |= removeUpgrade(i, sneakyUpgrades);
			}
		}
		for (int i = 0; i < sneakyUpgrades.length; i++) {
			IPipeUpgrade upgrade = sneakyUpgrades[i];
			if (upgrade instanceof SneakyUpgradeConfig) {
				ItemStack stack = sneakyInv.getItem(i);
				combinedSneakyOrientation[i] = ((SneakyUpgradeConfig) upgrade).getSide(stack);
			}
			if (upgrade instanceof IConfigPipeUpgrade) {
				guiUpgrades[i + 9] = true;
			}
		}
		if (needUpdate) {
			MainProxy.runOnServer(null, () -> () -> {
				pipe.connectionUpdate();
				if (pipe.container != null) {
					pipe.container.sendUpdateToClient();
				}
			});
		}
		uuid = null;
		uuidS = null;
		ItemStack stack = secInv.getItem(0);
		if (stack.isEmpty()) {
			return;
		}
		if (stack.getItem() != LPItems.itemCard.get() || stack.getDamageValue() != LogisticsItemCard.SEC_CARD) {
			return;
		}
		if (!logisticspipes.utils.item.StackTag.hasTag(stack)) {
			return;
		}
		if (!logisticspipes.utils.item.StackTag.getTag(stack).contains("UUID")) {
			return;
		}
		uuid = UUID.fromString(logisticspipes.utils.item.StackTag.getTag(stack).getString("UUID"));
		uuidS = uuid.toString();
	}

	/* Special implementations */

	@Override
	public boolean hasSneakyUpgrade() {
		return sneakyOrientation != null;
	}

	@Override
	public Direction getSneakyOrientation() {
		return sneakyOrientation;
	}

	@Override
	public int getSpeedUpgradeCount() {
		return speedUpgradeCount;
	}

	@Override
	public boolean hasCombinedSneakyUpgrade() {
		return isCombinedSneakyUpgrade;
	}

	@Override
	public Direction[] getCombinedSneakyOrientation() {
		return combinedSneakyOrientation;
	}

	public IGuiOpenControler getGuiController() {
		return new IGuiOpenControler() {

			PlayerCollectionList players = new PlayerCollectionList();

			@Override
			public void guiOpenedByPlayer(Player player) {
				players.add(player);
			}

			@Override
			public void guiClosedByPlayer(Player player) {
				players.remove(player);
				if (players.isEmpty() && !isCombinedSneakyUpgrade) {
					sneakyInv.dropContents(pipe.getWorld(), pipe.getPos());
				}
			}
		};
	}

	public boolean isNeedingContainerUpdate() {
		boolean tmp = needsContainerPositionUpdate;
		needsContainerPositionUpdate = false;
		return tmp;
	}

	public void dropUpgrades() {
		inv.dropContents(pipe.getWorld(), pipe.getPos());
		sneakyInv.dropContents(pipe.getWorld(), pipe.getPos());
	}

	@Override
	public boolean isSideDisconnected(Direction side) {
		return disconnectedSides.contains(side);
	}

	public boolean tryIserting(Level world, Player entityplayer) {
		ItemStack itemStackInMainHand = entityplayer.getItemBySlot(EquipmentSlot.MAINHAND);
		if (!itemStackInMainHand.isEmpty() && itemStackInMainHand.getItem() instanceof ItemUpgrade) {
			if (MainProxy.isClient(world)) {
				return true;
			}
			IPipeUpgrade upgrade = ((ItemUpgrade) itemStackInMainHand.getItem()).getUpgradeForItem(itemStackInMainHand, null);
			if (upgrade.isAllowedForPipe(pipe)) {
				if (isCombinedSneakyUpgrade) {
					if (upgrade instanceof SneakyUpgradeConfig) {
						if (insertIntInv(entityplayer, sneakyInv)) {
							return true;
						}
					}
				}
				if (insertIntInv(entityplayer, inv)) {
					return true;
				}
			}
		}
		if (!itemStackInMainHand.isEmpty() && itemStackInMainHand.getItem() == LPItems.itemCard.get() && itemStackInMainHand.getDamageValue() == LogisticsItemCard.SEC_CARD) {
			if (MainProxy.isClient(world)) {
				return true;
			}
			if (secInv.getItem(0).isEmpty()) {
				ItemStack newItem = itemStackInMainHand.split(1);
				secInv.setItem(0, newItem);
				InventoryChanged(secInv);
				return true;
			}
		}
		return false;
	}

	private boolean insertIntInv(Player entityplayer, SimpleStackInventory inv) {
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack item = inv.getItem(i);
			if (item.isEmpty()) {
				inv.setItem(i, entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).split(1));
				InventoryChanged(inv);
				return true;
			} else if (ItemIdentifier.get(item).equals(ItemIdentifier.get(entityplayer.getItemBySlot(EquipmentSlot.MAINHAND)))) {
				if (item.getCount() < inv.getMaxStackSize()) {
					item.grow(1);
					entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).split(1);
					inv.setItem(i, item);
					InventoryChanged(inv);
					return true;
				}
			}
		}
		return false;
	}

	public UUID getSecurityID() {
		return uuid;
	}

	public void insetSecurityID(UUID id) {
		ItemStack stack = new ItemStack(LPItems.itemCard.get(), 1);
		logisticspipes.utils.item.StackTag.setTag(stack, new CompoundTag());
		final CompoundTag tag = Objects.requireNonNull(logisticspipes.utils.item.StackTag.getTag(stack));
		tag.putString("UUID", id.toString());
		secInv.setItem(0, stack);
		InventoryChanged(secInv);
	}

	public void securityTick() {
		if ((getSecurityID()) != null) {
			if (!SimpleServiceLocator.securityStationManager.isAuthorized(uuidS)) {
				securityDelay++;
			} else {
				securityDelay = 0;
			}
			if (securityDelay > 20) {
				secInv.clearInventorySlotContents(0);
				InventoryChanged(secInv);
			}
		}
	}

	@Override
	public boolean isAdvancedSatelliteCrafter() {
		return isAdvancedCrafter;
	}

	@Override
	public boolean isFuzzyUpgrade() {
		return isFuzzyUpgrade;
	}

	@Override
	public int getFluidCrafter() {
		return liquidCrafter;
	}

	@Override
	public boolean hasByproductExtractor() {
		return hasByproductExtractor;
	}

	@Override
	public boolean hasPatternUpgrade() {
		return hasPatternUpgrade;
	}

	@Override
	public boolean hasPowerPassUpgrade() {
		return hasPowerPassUpgrade || hasRFPowerUpgrade || hasBCPowerUpgrade || getIC2PowerLevel > 0;
	}

	@Override
	public boolean hasRFPowerSupplierUpgrade() {
		return hasRFPowerUpgrade;
	}

	@Override
	public boolean hasBCPowerSupplierUpgrade() {
		return hasBCPowerUpgrade;
	}

	@Override
	public int getIC2PowerLevel() {
		return getIC2PowerLevel;
	}

	@Override
	public boolean hasCCRemoteControlUpgrade() {
		return hasCCRemoteControlUpgrade;
	}

	@Override
	public boolean hasCraftingMonitoringUpgrade() {
		return hasCraftingMonitoringUpgrade;
	}

	@Override
	public boolean isOpaque() {
		return hasOpaqueUpgrade;
	}

	@Override
	public int getCrafterCleanup() {
		return craftingCleanup;
	}

	public boolean hasLogicControll() {
		return hasLogicControll;
	}

	@Override
	public boolean hasUpgradeModuleUpgrade() {
		return hasUpgradeModuleUpgarde;
	}

	@Override
	public boolean hasOwnSneakyUpgrade() {
		return false;
	}

	public boolean hasGuiUpgrade(int i) {
		return guiUpgrades[i];
	}

	public IPipeUpgrade getUpgrade(int i) {
		if (i < upgrades.length) {
			return upgrades[i];
		} else {
			return sneakyUpgrades[i - upgrades.length];
		}
	}

	@Override
	public DoubleCoordinates getPipePosition() {
		return pipe.getLPPosition();
	}

	@Override
	public int getActionSpeedUpgrade() {
		return actionSpeedUpgrade;
	}

	@Override
	public int getItemExtractionUpgrade() {
		return itemExtractionUpgrade;
	}

	@Override
	public int getItemStackExtractionUpgrade() {
		return itemStackExtractionUpgrade;
	}

	@Override
	public SimpleStackInventory getInv() {
		return this.inv;
	}

}
