package logisticspipes.gui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;




import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.LogicControllerPacket;
import logisticspipes.network.packets.gui.OpenUpgradePacket;
import logisticspipes.network.packets.pipe.PipeManagerWatchingPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.IPipeUpgrade;
import logisticspipes.pipes.upgrades.SneakyUpgradeConfig;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.LogisticsBaseTabGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;
import logisticspipes.utils.string.StringUtils;
import network.rs485.logisticspipes.util.TextUtil;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class GuiPipeController extends LogisticsBaseTabGuiScreen {

	private final String PREFIX = "gui.pipecontroller.";

	private final CoreRoutedPipe pipe;

	public GuiPipeController(final Player player, final CoreRoutedPipe pipe) {
		super(buildDummy(player, pipe), 180, 220);
		this.pipe = pipe;
		DummyContainer dummy = (DummyContainer) this.menu;

		//Order is important here: (Slot Server/Client sync)
		Upgrades upgrades = new Upgrades(dummy);
		Security security = new Security(dummy);
		Statistics statistics = new Statistics();
		//Logic logic = new Logic();
		addHiddenSlot(dummy.addRestrictedSlot(0, pipe.container.logicController.diskInv, 14, 36, LPItems.disk.get())); //Keep it for now, but hidden. Maybe it will be used again later
		Tasks tasks = new Tasks();

		//Here order doesn't matter/can be changed to reorganise tabs
		if (LogisticsPipes.isDEBUG()) {
			addTab(upgrades);
			addTab(security);
			addTab(statistics);
			//addTab(logic);
			addTab(tasks);
		} else {
			addTab(statistics);
			addTab(upgrades);
			addTab(security);
			//addTab(logic);
			addTab(tasks);
		}
	}
	private static DummyContainer buildDummy(final Player player, final CoreRoutedPipe pipe) {
		DummyContainer dummy = new DummyContainer(player, null, pipe.getOriginalUpgradeManager().getGuiController());
		dummy.addNormalSlotsForPlayerInventory(10, 135);
		return dummy;
	}


	private class Upgrades extends TabSubGui {

		private final List<Slot> TAB_SLOTS_SNEAKY_INV = new ArrayList<>();
		private final Slot[] upgradeSlot = new Slot[18];
		private net.minecraft.client.gui.components.AbstractButton[] upgradeConfig = new net.minecraft.client.gui.components.AbstractButton[18];

		private Upgrades(DummyContainer dummy) {
			for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
				addSlot(upgradeSlot[pipeSlot] = dummy.addUpgradeSlot(pipeSlot, pipe.getOriginalUpgradeManager(), pipeSlot, 10 + pipeSlot * 18, 42, itemStack ->
						!itemStack.isEmpty() && itemStack.getItem() instanceof ItemUpgrade && ((ItemUpgrade) itemStack.getItem()).getUpgradeForItem(itemStack, null).isAllowedForPipe(pipe)));
			}

			for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
				TAB_SLOTS_SNEAKY_INV.add(addSlot(upgradeSlot[pipeSlot + 9] = dummy.addSneakyUpgradeSlot(pipeSlot, pipe.getOriginalUpgradeManager(), pipeSlot + 9, 10 + pipeSlot * 18, 88, itemStack -> {
					if (itemStack.isEmpty()) {
						return false;
					}
					if (itemStack.getItem() instanceof ItemUpgrade) {
						IPipeUpgrade upgrade = ((ItemUpgrade) itemStack.getItem()).getUpgradeForItem(itemStack, null);
						return upgrade instanceof SneakyUpgradeConfig && upgrade.isAllowedForPipe(pipe);
					} else {
						return false;
					}
				})));
			}
		}

		@Override
		public void initTab() {
			int x = 0;
			int y = 0;
			for (int i = 0; i < upgradeConfig.length; i++) {
				upgradeConfig[i] = addRenderableWidget(new SmallGuiButton(20 + i, leftPos + 13 + x, topPos + 61 + y, 10, 10, "!"));
				upgradeConfig[i].visible = pipe.getOriginalUpgradeManager().hasGuiUpgrade(i);
				x += 18;
				if (x > 160 && y == 0) {
					x = 0;
					y = 46;
				}
			}
		}

		@Override
		public void checkButton(net.minecraft.client.gui.components.AbstractButton button, boolean isTabActive) {
			super.checkButton(button, isTabActive);
			for (int i = 0; i < upgradeConfig.length; i++) {
				upgradeConfig[i].visible &= pipe.getOriginalUpgradeManager().hasGuiUpgrade(i);
			}
		}

		@Override
		public boolean showSlot(Slot slot) {
			return pipe.getOriginalUpgradeManager().hasCombinedSneakyUpgrade() || !TAB_SLOTS_SNEAKY_INV.contains(slot);
		}

		@Override
		public void renderIcon(int x, int y) {
			// Deferred: tab icon requires an LP item texture selection; left blank for now.
		}

		@Override
		public void buttonClicked(net.minecraft.client.gui.components.AbstractButton button) {
			for (int i = 0; i < upgradeConfig.length; i++) {
				if (upgradeConfig[i] == button) {
					MainProxy.sendPacketToServer(PacketHandler.getPacket(OpenUpgradePacket.class).setSlot(upgradeSlot[i]));
				}
			}
		}

		@Override
		public void renderBackgroundContent() {
			for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
				LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 9 + pipeSlot * 18, topPos + 41);
			}
			if (pipe.getOriginalUpgradeManager().hasCombinedSneakyUpgrade()) {
				for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
					LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 9 + pipeSlot * 18, topPos + 87);
				}
			}
		}

		@Override
		public void renderForegroundContent() {
			guiGraphics.drawString(font, TextUtil.translate(PREFIX + "upgrade"), 10, 28, Color.getValue(Color.DARKER_GREY), false);
			if (pipe.getOriginalUpgradeManager().hasCombinedSneakyUpgrade()) {
				guiGraphics.drawString(font, TextUtil.translate(PREFIX + "sneakyUpgrades"), 10, 74, Color.getValue(Color.DARKER_GREY), false);
			}
		}
	}

	private class Security extends TabSubGui {

		public Security(DummyContainer dummy) {
			addSlot(dummy
					.addStaticRestrictedSlot(0, pipe.getOriginalUpgradeManager().secInv, 10, 42, itemStack -> {
						if (itemStack.isEmpty()) {
							return false;
						}
						if (itemStack.getItem() != LPItems.itemCard.get()) {
							return false;
						}
						if (LogisticsItemCard.getCardType(itemStack) != LogisticsItemCard.SEC_CARD) {
							return false;
						}
						return SimpleServiceLocator.securityStationManager
								.isAuthorized(UUID.fromString(logisticspipes.utils.item.StackTag.getTag(itemStack).getString("UUID")));
					}, 1));
		}

		@Override
		public void renderIcon(int x, int y) {
			LPGuiGraphics.drawLockBackground(minecraft, x + 1, y);
		}

		@Override
		public void renderBackgroundContent() {
			LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 9, topPos + 41);
		}

		@Override
		public void renderForegroundContent() {
			guiGraphics.drawString(font, TextUtil.translate(PREFIX + "security"), 10, 28, Color.getValue(Color.DARKER_GREY), false);
			ItemStack itemStack = pipe.getOriginalUpgradeManager().secInv.getItem(0);
			if (!itemStack.isEmpty()) {
				UUID id = UUID.fromString(logisticspipes.utils.item.StackTag.getTag(itemStack).getString("UUID"));
				guiGraphics.drawString(font, "Id: ", 10, 68, Color.getValue(Color.DARKER_GREY), false);
				guiGraphics.drawString(font, ChatColor.BLUE.toString() + id.toString(), 10, 80, Color.getValue(Color.DARKER_GREY), false);
				guiGraphics.drawString(font, "Authorization: " + (SimpleServiceLocator.securityStationManager.isAuthorized(id) ? ChatColor.GREEN + "Authorized" : ChatColor.RED + "Unauthorized"), 10, 94, Color.getValue(Color.DARKER_GREY), false);
			}
		}
	}

	private class Statistics extends TabSubGui {

		@Override
		public void renderIcon(int x, int y) {
			LPGuiGraphics.drawStatsBackground(minecraft, x, y);
		}

		@Override
		public void renderBackgroundContent() {

		}

		@Override
		public void renderForegroundContent() {
			String pipeName = ItemIdentifier.get(pipe.item, 0, null).getFriendlyName();
			guiGraphics.drawString(font, pipeName, (170 - font.width(pipeName)) / 2, 28, 0x83601c, false);

			int sessionXCenter = 85;
			int lifetimeXCenter = 140;
			String s;

			guiGraphics.drawString(font, TextUtil.translate(PREFIX + "Session"), sessionXCenter - font
					.width(TextUtil.translate(PREFIX + "Session")) / 2, 40, 0x303030, false);
			guiGraphics.drawString(font, TextUtil.translate(PREFIX + "Lifetime"), lifetimeXCenter - font
					.width(TextUtil.translate(PREFIX + "Lifetime")) / 2, 40, 0x303030, false);
			guiGraphics.drawString(font, TextUtil.translate(PREFIX + "Sent") + ":", 55 - font
					.width(TextUtil.translate(PREFIX + "Sent") + ":"), 55, 0x303030, false);
			guiGraphics.drawString(font, TextUtil.translate(PREFIX + "Recieved") + ":", 55 - font
					.width(TextUtil.translate(PREFIX + "Recieved") + ":"), 70, 0x303030, false);
			guiGraphics.drawString(font, TextUtil.translate(PREFIX + "Relayed") + ":", 55 - font
					.width(TextUtil.translate(PREFIX + "Relayed") + ":"), 85, 0x303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_session_sent);
			guiGraphics.drawString(font, s, sessionXCenter - font.width(s) / 2, 55, 0x303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_session_recieved);
			guiGraphics.drawString(font, s, sessionXCenter - font.width(s) / 2, 70, 0x303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_session_relayed);
			guiGraphics.drawString(font, s, sessionXCenter - font.width(s) / 2, 85, 0x303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_lifetime_sent);
			guiGraphics.drawString(font, s, lifetimeXCenter - font.width(s) / 2, 55, 0x303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_lifetime_recieved);
			guiGraphics.drawString(font, s, lifetimeXCenter - font.width(s) / 2, 70, 0x303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.stat_lifetime_relayed);
			guiGraphics.drawString(font, s, lifetimeXCenter - font.width(s) / 2, 85, 0x303030, false);

			guiGraphics.drawString(font, TextUtil.translate(PREFIX + "RoutingTableSize") + ":", 110 - font
					.width(TextUtil.translate(PREFIX + "RoutingTableSize") + ":"), 110, 0x303030, false);

			s = StringUtils.getStringWithSpacesFromLong(pipe.server_routing_table_size);
			guiGraphics.drawString(font, s, 130 - font.width(s) / 2, 110, 0x303030, false);
		}
	}

	private class Logic extends TabSubGui {

		private net.minecraft.client.gui.components.AbstractButton editButton;

		@Override
		public void initTab() {
			editButton = addRenderableWidget(new logisticspipes.utils.gui.SmallGuiButton(0, leftPos + 10, topPos + 70, 160, 20, "Edit Logic Controller"));
		}

		@Override
		public void renderIcon(int x, int y) {
			// Deferred: tab icon requires an LP item texture selection; left blank for now.
		}

		@Override
		public void buttonClicked(net.minecraft.client.gui.components.AbstractButton button) {
			if (button == editButton) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(LogicControllerPacket.class)
						.setTilePos(pipe.container));
			}
		}

		@Override
		public void renderBackgroundContent() {

			guiGraphics.fill(leftPos + 12, topPos + 34, leftPos + 32, topPos + 54, Color.getValue(Color.BLACK));
			guiGraphics.fill(leftPos + 14, topPos + 36, leftPos + 30, topPos + 52, Color.getValue(Color.DARKER_GREY));
		}

		@Override
		public void checkButton(net.minecraft.client.gui.components.AbstractButton button, boolean isTabActive) {
			if (isTabActive) {
				button.active = pipe.container.logicController.diskInv.getItem(0) != null;
			}
			super.checkButton(button, isTabActive);
		}

		@Override
		public void renderForegroundContent() {

		}
	}

	private class Tasks extends TabSubGui {

		private net.minecraft.client.gui.components.AbstractButton leftButton;
		private net.minecraft.client.gui.components.AbstractButton rightButton;
		private ItemDisplay _itemDisplay_5;
		private boolean managerWatching;

		@Override
		public void initTab() {
			

			leftButton = addRenderableWidget(new SmallGuiButton(1, leftPos + 95, topPos + 26, 10, 10, "<"));
			rightButton = addRenderableWidget(new SmallGuiButton(2, leftPos + 165, topPos + 26, 10, 10, ">"));
			if (_itemDisplay_5 == null) {
				_itemDisplay_5 = new ItemDisplay(null, font, GuiPipeController.this, null, 10, 40, 20, 60, 0, 0, 0, new int[] { 1, 1, 1, 1 }, true);
			}
			_itemDisplay_5.reposition(10, 40, 20, 60, 0, 0);
		}

		@Override
		public void renderIcon(int x, int y) {
			LPGuiGraphics.drawLinesBackground(minecraft, x, y);
		}

		@Override
		public void renderBackgroundContent() {

		}

		@Override
		public void leavingTab() {
			if (managerWatching) {
				managerWatching = false;
				MainProxy.sendPacketToServer(PacketHandler.getPacket(PipeManagerWatchingPacket.class).setStart(false).setTilePos(pipe.container));
			}
		}

		@Override
		public void enteringTab() {
			if (!managerWatching) {
				managerWatching = true;
				MainProxy.sendPacketToServer(PacketHandler.getPacket(PipeManagerWatchingPacket.class).setStart(true).setTilePos(pipe.container));
			}
		}

		@Override
		public void buttonClicked(net.minecraft.client.gui.components.AbstractButton button) {
			if (button == leftButton) {
				_itemDisplay_5.prevPage();
			} else if (button == rightButton) {
				_itemDisplay_5.nextPage();
			}
		}

		@Override
		public void renderForegroundContent() {
			List<ItemIdentifierStack> _allItems = pipe.getClientSideOrderManager().stream()
					.map(IOrderInfoProvider::getAsDisplayItem).collect(Collectors.toCollection(LinkedList::new));
			_itemDisplay_5.setItemList(_allItems);
			_itemDisplay_5.renderItemArea(0.0f);
			_itemDisplay_5.renderPageNumber(right - leftPos - 45, 28);
			int start = _itemDisplay_5.getPage() * 3;
			int stringPos = 40;
			for (int i = start; i < start + 3 && i < pipe.getClientSideOrderManager().size(); i++) {
				IOrderInfoProvider order = pipe.getClientSideOrderManager().get(i);
				ItemIdentifier target = order.getTargetType();
				String s;
				if (target != null) {
					s = target.getFriendlyName();
					guiGraphics.drawString(font, s, 35, stringPos, 0x303030, false);
				}
				s = Integer.toString(i + 1);
				stringPos += 6;
				guiGraphics.drawString(font, s, 3, stringPos, 0x303030, false);
				stringPos += 4;
				DoubleCoordinates pos = order.getTargetPosition();
				if (pos != null) {
					s = pos.toIntBasedString();
					guiGraphics.drawString(font, s, 40, stringPos, 0x303030, false);
				}
				stringPos += 10;
			}
		}
	}
}
