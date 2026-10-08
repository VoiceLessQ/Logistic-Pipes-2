/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import kotlin.Unit;
import lombok.Getter;


import logisticspipes.LPItems;
import logisticspipes.gui.modules.ModuleBaseGui;
import logisticspipes.gui.popup.GuiSelectSatellitePopup;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.cpipe.CPipeCleanupImport;
import logisticspipes.network.packets.module.ModulePropertiesUpdate;
import logisticspipes.network.packets.pipe.CraftingPipeSetSatellitePacket;
import logisticspipes.pipes.upgrades.CraftingByproductUpgrade;
import logisticspipes.pipes.upgrades.CraftingCleanupUpgrade;
import logisticspipes.pipes.upgrades.FluidCraftingUpgrade;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.extension.GuiExtension;
import network.rs485.logisticspipes.gui.widget.Label;
import network.rs485.logisticspipes.gui.widget.VerticalLabel;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.IntListProperty;
import network.rs485.logisticspipes.property.IntegerProperty;
import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.property.layer.PropertyLayer;
import network.rs485.logisticspipes.property.layer.PropertyOverlay;
import network.rs485.logisticspipes.property.layer.ValuePropertyOverlay;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

public class GuiCraftingPipe extends ModuleBaseGui {

	private static final String PREFIX = "gui.crafting.";

	@Getter
	private final ModuleCrafter craftingModule;
	private final Player _player;
	private final net.minecraft.client.gui.components.AbstractButton[] normalButtonArray;
	private final net.minecraft.client.gui.components.AbstractButton[][] advancedSatButtonArray;
	private final net.minecraft.client.gui.components.AbstractButton[][] liquidGuiParts;
	private final boolean isAdvancedSat;
	private final int liquidCrafter;
	private final boolean hasByproductExtractor;
	private final int cleanupSize;
	private final int[] fluidSlotIDs;
	private final int byproductSlotID;
	private final int[] cleanupSlotIDs;
	private final PropertyLayer propertyLayer;
	private final ValuePropertyOverlay<Boolean, BooleanProperty> cleanupModeIsExcludeOverlay;
	private final ValuePropertyOverlay<Integer, IntegerProperty> craftingPriorityOverlay;
	private final PropertyOverlay<List<Integer>, IntListProperty> liquidAmountsOverlay;

	private net.minecraft.client.gui.components.AbstractButton cleanupModeButton;
	private final Label[] satellitePipeLabels;
	private Label satellitePipeLabel;

	public GuiCraftingPipe(Player player, ModuleCrafter module, boolean isAdvancedSat,
			int liquidCrafter, int[] amount, boolean hasByproductExtractor, boolean isFuzzy, int cleanupSize,
			boolean cleanupExclude) {
		super(buildDummy(player, module, isAdvancedSat, liquidCrafter, amount, hasByproductExtractor, isFuzzy, cleanupSize, cleanupExclude), module);
		craftingModule = module;
		_player = player;
		this.isAdvancedSat = isAdvancedSat;
		this.liquidCrafter = liquidCrafter;
		this.hasByproductExtractor = hasByproductExtractor;
		this.cleanupSize = cleanupSize;
		craftingModule.cleanupModeIsExclude.setValue(cleanupExclude);

		propertyLayer = new PropertyLayer(craftingModule.getProperties());
		cleanupModeIsExcludeOverlay = propertyLayer.overlay(craftingModule.cleanupModeIsExclude);
		propertyLayer.addObserver(craftingModule.cleanupModeIsExclude, this::updateCleanupModeButton);
		craftingPriorityOverlay = propertyLayer.overlay(craftingModule.priority);
		liquidAmountsOverlay = propertyLayer.overlay(craftingModule.liquidAmounts);

		if (!hasByproductExtractor) {
			imageWidth = 177;
		} else {
			imageWidth = 217;
		}

		if (!isAdvancedSat) {
			imageHeight = 187;
		} else {
			imageHeight = 187 + 30;
		}

		craftingModule.liquidAmounts.replaceContent(amount);
		normalButtonArray = new net.minecraft.client.gui.components.AbstractButton[7];
		advancedSatButtonArray = new net.minecraft.client.gui.components.AbstractButton[9][2];
		for (int i = 0; i < 9; i++) {
			advancedSatButtonArray[i] = new net.minecraft.client.gui.components.AbstractButton[2];
		}
		satellitePipeLabels = new Label[9];

		// Register controlled slots with extensionControllerLeft and store IDs.
		// Slot order in menu: 36 player inv + 9 input + 1 output = 46 base, then fluid, byproduct, cleanup.
		int slotBase = 36 + 9 + 1;
		liquidGuiParts = new net.minecraft.client.gui.components.AbstractButton[liquidCrafter][];
		fluidSlotIDs = new int[liquidCrafter];
		for (int i = 0; i < liquidCrafter; i++) {
			fluidSlotIDs[i] = extensionControllerLeft.registerControlledSlot(this.menu.slots.get(slotBase + i));
			liquidGuiParts[i] = new net.minecraft.client.gui.components.AbstractButton[10];
		}
		int byproductBase = slotBase + liquidCrafter;
		byproductSlotID = hasByproductExtractor
				? extensionControllerLeft.registerControlledSlot(this.menu.slots.get(byproductBase))
				: -1;
		cleanupSlotIDs = new int[cleanupSize * 3];
		int cleanupBase = byproductBase + (hasByproductExtractor ? 1 : 0);
		for (int i = 0; i < cleanupSize * 3; i++) {
			cleanupSlotIDs[i] = extensionControllerLeft.registerControlledSlot(this.menu.slots.get(cleanupBase + i));
		}
	}
	private static DummyContainer buildDummy(Player player, ModuleCrafter module, boolean isAdvancedSat,
			int liquidCrafter, int[] amount, boolean hasByproductExtractor, boolean isFuzzy, int cleanupSize,
			boolean cleanupExclude) {
		// Use module.dummyInventory directly; propertyLayer is not yet available during static construction
		DummyContainer dummy = new DummyContainer(player.getInventory(), module.dummyInventory);
		int computedImageHeight = isAdvancedSat ? 217 : 187;
		dummy.addNormalSlotsForPlayerInventory(8, computedImageHeight - 82);

		// Input slots
		for (int l = 0; l < 9; l++) {
			if (isFuzzy) {
				dummy.addFuzzyDummySlot(l, 8 + l * 18, 18, module.inputFuzzy(l));
			} else {
				dummy.addDummySlot(l, 8 + l * 18, 18);
			}
		}

		// Output slot
		int yPosOutput = 55;
		if (isAdvancedSat) yPosOutput = 105;
		if (isFuzzy) {
			dummy.addFuzzyDummySlot(9, 85, yPosOutput, module.outputFuzzy());
		} else {
			dummy.addDummySlot(9, 85, yPosOutput);
		}

		// Fluid slots (extensionControllerLeft registration happens in constructor after super())
		for (int i = 0; i < liquidCrafter; i++) {
			int liquidLeft;
			if (isAdvancedSat) {
				liquidLeft = -40;
			} else {
				liquidLeft = -(liquidCrafter * 40) + (i * 40);
			}
			dummy.addFluidSlot(i, module.liquidInventory, liquidLeft + 11, 24);
		}

		if (hasByproductExtractor) {
			dummy.addDummySlot(10, -26, 29);
		}

		for (int y = 0; y < cleanupSize; y++) {
			for (int x = 0; x < 3; x++) {
				dummy.addDummySlot(y * 3 + x, module.cleanupInventory, x * 18 - 57, y * 18 + 13);
			}
		}
		return dummy;
	}


	@Override
	public void init() {
		super.init();
		extensionControllerLeft.clear();
		FluidCraftingExtension extension = null;
		if (!isAdvancedSat) {
			if (liquidCrafter != 0) {
				extension = new FluidCraftingExtension(0);
			}
			addRenderableWidget(normalButtonArray[0] = new SmallGuiButton(0, (width - imageWidth) / 2 + 125, (height - imageHeight) / 2 + 57, 37, 10, TextUtil.translate(PREFIX + "Select")));
			normalButtonArray[0].active = craftingModule.getSlot().isInWorld();
			addRenderableWidget(normalButtonArray[1] = new SmallGuiButton(3, (width - imageWidth) / 2 + 39, (height - imageHeight) / 2 + 50, 37, 10, TextUtil.translate(GuiCraftingPipe.PREFIX + "Import")));
			addRenderableWidget(normalButtonArray[2] = new SmallGuiButton(4, (width - imageWidth) / 2 + 6, (height - imageHeight) / 2 + 50, 28, 10, TextUtil.translate(GuiCraftingPipe.PREFIX + "Open")));
			addRenderableWidget(normalButtonArray[3] = new SmallGuiButton(20, (width - imageWidth) / 2 + 155, (height - imageHeight) / 2 + 85, 10, 10, ">"));
			addRenderableWidget(normalButtonArray[4] = new SmallGuiButton(21, (width - imageWidth) / 2 + 120, (height - imageHeight) / 2 + 85, 10, 10, "<"));
			if (liquidCrafter != 0) {
				extension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(normalButtonArray[5] = new SmallGuiButton(22, leftPos - (liquidCrafter * 40) / 2 - 18, topPos + 158, 37, 10, TextUtil.translate(PREFIX + "Select")))));
			}
			satellitePipeLabel = new Label(craftingModule.clientSideSatelliteNames.satelliteName, 115, 43, 55, 0x404040, 0xff8b8b8b);
		} else {
			for (int i = 0; i < 9; i++) {
				addRenderableWidget(advancedSatButtonArray[i][0] = new SmallGuiButton(30 + i, (width - imageWidth) / 2 + 9 + 18 * i, (height - imageHeight) / 2 + 75, 17, 10, TextUtil.translate(PREFIX + "Sel")));
				satellitePipeLabels[i] = new VerticalLabel(craftingModule.clientSideSatelliteNames.advancedSatelliteNameArray[i], 11 + (i * 18), 37, 36, 0x404040, 0xffc6c6c6);
			}
			addRenderableWidget(normalButtonArray[1] = new SmallGuiButton(3, (width - imageWidth) / 2 + 39, (height - imageHeight) / 2 + 100, 37, 10, TextUtil.translate(GuiCraftingPipe.PREFIX + "Import")));
			addRenderableWidget(normalButtonArray[2] = new SmallGuiButton(4, (width - imageWidth) / 2 + 6, (height - imageHeight) / 2 + 100, 28, 10, TextUtil
					.translate(GuiCraftingPipe.PREFIX + "Open")));
			addRenderableWidget(normalButtonArray[3] = new SmallGuiButton(20, (width - imageWidth) / 2 + 155, (height - imageHeight) / 2 + 105, 10, 10, ">"));
			addRenderableWidget(normalButtonArray[4] = new SmallGuiButton(21, (width - imageWidth) / 2 + 120, (height - imageHeight) / 2 + 105, 10, 10, "<"));
		}
		for (int i = 0; i < liquidCrafter; i++) {
			if (isAdvancedSat) {
				extension = new FluidCraftingExtension(i);
			}
			int liquidLeft;
			if (isAdvancedSat) {
				liquidLeft = leftPos - 40;
			} else {
				liquidLeft = leftPos - (liquidCrafter * 40) + (i * 40);
			}
			liquidGuiParts[i] = new net.minecraft.client.gui.components.AbstractButton[10];
			extension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(liquidGuiParts[i][0] = new SmallGuiButton(100 + 10 * i + 0, liquidLeft + 22, topPos + 65, 10, 10, "+"))));
			extension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(liquidGuiParts[i][1] = new SmallGuiButton(100 + 10 * i + 1, liquidLeft + 22, topPos + 85, 10, 10, "+"))));
			extension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(liquidGuiParts[i][2] = new SmallGuiButton(100 + 10 * i + 2, liquidLeft + 22, topPos + 105, 10, 10, "+"))));
			extension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(liquidGuiParts[i][3] = new SmallGuiButton(100 + 10 * i + 3, liquidLeft + 22, topPos + 125, 10, 10, "+"))));
			extension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(liquidGuiParts[i][4] = new SmallGuiButton(100 + 10 * i + 4, liquidLeft + 8, topPos + 65, 10, 10, "-"))));
			extension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(liquidGuiParts[i][5] = new SmallGuiButton(100 + 10 * i + 5, liquidLeft + 8, topPos + 85, 10, 10, "-"))));
			extension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(liquidGuiParts[i][6] = new SmallGuiButton(100 + 10 * i + 6, liquidLeft + 8, topPos + 105, 10, 10, "-"))));
			extension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(liquidGuiParts[i][7] = new SmallGuiButton(100 + 10 * i + 7, liquidLeft + 8, topPos + 125, 10, 10, "-"))));
			if (isAdvancedSat) {
				final SmallGuiButton advancedSatelliteSelector = new SmallGuiButton(100 + 10 * i + 8, liquidLeft + 2, topPos + 160, 37, 10, TextUtil.translate(PREFIX + "Select"));
				advancedSatelliteSelector.active = craftingModule.getSlot().isInWorld();
				extension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(liquidGuiParts[i][8] = advancedSatelliteSelector)));
				extensionControllerLeft.addExtension(extension);
			}
			extension.registerSlot(fluidSlotIDs[i]);
		}
		if (!isAdvancedSat && liquidCrafter != 0) {
			extensionControllerLeft.addExtension(extension);
		}
		if (hasByproductExtractor) {
			ByproductExtension byproductExtension = new ByproductExtension();
			byproductExtension.registerSlot(byproductSlotID);
			extensionControllerLeft.addExtension(byproductExtension);
		}
		if (cleanupSize > 0) {
			CleanupExtension cleanupExtension = new CleanupExtension();
			cleanupExtension.registerButton(extensionControllerLeft.registerControlledButton(addRenderableWidget(cleanupModeButton = new SmallGuiButton(24, leftPos - 56, topPos + 18 + (18 * cleanupSize), 50, 10, TextUtil.translate(GuiCraftingPipe.PREFIX + (
					cleanupModeIsExcludeOverlay.get() ? "Exclude" : "Include"))))));
			cleanupExtension.registerButton(
				extensionControllerLeft.registerControlledButton(addRenderableWidget(new SmallGuiButton(25, leftPos - 56, topPos + 32 + (18 * cleanupSize), 50, 10, TextUtil.translate(GuiCraftingPipe.PREFIX + "Import")))));
			for (int i = 0; i < cleanupSize * 3; i++) {
				cleanupExtension.registerSlot(cleanupSlotIDs[i]);
			}
			extensionControllerLeft.addExtension(cleanupExtension);
		}
		for (var w : this.children()) {
			if (w instanceof SmallGuiButton sgb) {
				sgb.setPressListener(b -> handleButton(sgb.id));
			}
		}
	}

	private void handleButton(int id) {
		if (30 <= id && id < 40) {
			openSubGuiForSatelliteSelection(10 + (id - 30), false);
		}
		if (100 <= id && id < 200) {
			int i = id - 100;
			int action = i % 10;
			i -= action;
			i /= 10;
			if (action < 8) {
				int amount = 0;
				switch (action) {
					case 0:
						amount = 1;
						break;
					case 1:
						amount = 10;
						break;
					case 2:
						amount = 100;
						break;
					case 3:
						amount = 1000;
						break;
					case 4:
						amount = -1;
						break;
					case 5:
						amount = -10;
						break;
					case 6:
						amount = -100;
						break;
					case 7:
						amount = -1000;
						break;
					default:
						break;
				}
				craftingModule.changeFluidAmount(amount, i, _player);
			} else if (action == 8) {
				openSubGuiForSatelliteSelection(110 + i, true);
			}
		}
		switch (id) {
			case 0:
				openSubGuiForSatelliteSelection(0, false);
				break;
			case 3:
				craftingModule.importFromCraftingTable(_player);
				break;
			case 4:
				craftingModule.openAttachedGui(_player);
				break;
			case 20:
				craftingPriorityOverlay.write(prop -> prop.increase(1));
				break;
			case 21:
				craftingPriorityOverlay.write(prop -> prop.increase(-1));
				break;
			case 22:
				openSubGuiForSatelliteSelection(100, true);
				break;
			case 24:
				cleanupModeIsExcludeOverlay.write(BooleanProperty::toggle);
				break;
			case 25:
				cleanupModeIsExcludeOverlay.set(false);
				MainProxy.sendPacketToServer(PacketHandler.getPacket(CPipeCleanupImport.class).setModulePos(craftingModule));
				break;
		}
	}

	private void openSubGuiForSatelliteSelection(int id, boolean fluidSatellite) {
		if (module.getSlot().isInWorld()) {
			this.setSubGui(new GuiSelectSatellitePopup(module.getBlockPos(), fluidSatellite, uuid ->
					MainProxy.sendPacketToServer(PacketHandler.getPacket(CraftingPipeSetSatellitePacket.class).setPipeID(uuid).putInt(id).setModulePos(module))));
		}
	}

	@Override
	public void onClose() {
		super.onClose();
		propertyLayer.unregister();
		if (this.minecraft.player != null && !propertyLayer.getProperties().isEmpty()) {
			// send update to server, when there are changed properties
			MainProxy.sendPacketToServer(ModulePropertiesUpdate.fromPropertyHolder(propertyLayer).setModulePos(module));
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderLabels(guiGraphics, mouseX, mouseY);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Inputs"), 18, 7, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Inventory"), 10, imageHeight - 93, 0x404040, false);

		if (!isAdvancedSat) {
			guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Output"), 77, 40, 0x404040, false);
			guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Satellite"), 123, 7, 0x404040, false);
			if (craftingModule.clientSideSatelliteNames.satelliteName.isEmpty()) {
				guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Off"), 135, 43, 0x404040, false);
			} else {
				if (!satellitePipeLabel.isTextEqual(craftingModule.clientSideSatelliteNames.satelliteName)) {
					satellitePipeLabel.setText(craftingModule.clientSideSatelliteNames.satelliteName);
				}
				satellitePipeLabel.draw(mouseX - leftPos, mouseY - topPos);
			}
			guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Priority") + ":", 123, 75, 0x404040, false);
			guiGraphics.drawString(minecraft.font, String.valueOf(craftingPriorityOverlay.get()), 143 - (minecraft.font.width(String.valueOf(craftingPriorityOverlay.get())) / 2), 87, 0x404040, false);
		} else {
			for (int i = 0; i < 9; i++) {
				if (craftingModule.clientSideSatelliteNames.advancedSatelliteNameArray[i].isEmpty()) {
					guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Off"), 9 + (i * 18), 57, 0x404040, false);
				} else {
					if (!satellitePipeLabels[i].isTextEqual(craftingModule.clientSideSatelliteNames.advancedSatelliteNameArray[i])) {
						satellitePipeLabels[i].setText(craftingModule.clientSideSatelliteNames.advancedSatelliteNameArray[i]);
					}
					satellitePipeLabels[i].draw(mouseX - leftPos, mouseY - topPos);
				}
			}
			guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Output"), 77, 90, 0x404040, false);
			guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Priority") + ":", 123, 95, 0x404040, false);
			guiGraphics.drawString(minecraft.font, String.valueOf(craftingPriorityOverlay.get()), 143 - (minecraft.font.width(String.valueOf(craftingPriorityOverlay.get())) / 2), 107, 0x404040, false);
		}
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float f, int x, int y) {
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, leftPos + imageWidth - (hasByproductExtractor ? 40 : 0), topPos + imageHeight, 0.0f, true, true, true, true, true);

		if (!isAdvancedSat) {
			guiGraphics.fill(leftPos + 115, topPos + 4, leftPos + 170, topPos + 70, 0xff8B8B8B);
		}

		for (int i = 0; i < 9; i++) {
			LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 7 + (18 * i), topPos + 17);
		}
		if (!isAdvancedSat) {
			LPGuiGraphics.drawBigSlotBackground(minecraft, leftPos + 80, topPos + 50);
		} else {
			LPGuiGraphics.drawBigSlotBackground(minecraft, leftPos + 80, topPos + 100);
		}
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 8, topPos + imageHeight - 82);

		super.renderExtensions();
	}

	private Unit updateCleanupModeButton(Property<Boolean> prop) {
		cleanupModeButton.setMessage(net.minecraft.network.chat.Component.literal(TextUtil.translate(
				GuiCraftingPipe.PREFIX + (prop.copyValue() ? "Exclude" : "Include"))));
		return Unit.INSTANCE;
	}

	private final class FluidCraftingExtension extends GuiExtension {

		private final int id;

		public FluidCraftingExtension(int id) {
			this.id = id;
		}

		@Override
		public int getFinalWidth() {
			if (isAdvancedSat) {
				return 42;
			} else {
				return 2 + liquidCrafter * 40;
			}
		}

		@Override
		public int getFinalHeight() {
			return 175;
		}

		@Override
		public void renderForeground(int left, int top) {
			if (!isFullyExtended()) {
				renderUpgradeIcon(FluidCraftingUpgrade.getName(), left, top);
				if (!isAdvancedSat && liquidCrafter > 1) {
					String s = Integer.toString(liquidCrafter);
					guiGraphics.pose().pushPose();
					guiGraphics.pose().translate(0, 0, 200);
					guiGraphics.drawString(minecraft.font, s, left + 22 - minecraft.font.width(s), top + 14, 0xFFFFFF, true);
					guiGraphics.pose().popPose();
				}
			}
			if (isFullyExtended()) {
				if (liquidCrafter > 1 && !isAdvancedSat) {
					for (int i = 1; i < liquidCrafter; i++) {
						int xLine = left + 2 + (i * 40);
						guiGraphics.fill(xLine, top + 3, xLine + 1, top + 138, 0xff8B8B8B);
					}
				}
				if (!isAdvancedSat) {
					guiGraphics.fill(left + 3, top + 138, left + 2 + (liquidCrafter * 40), top + 139, 0xff8B8B8B);
				}
				if (!isAdvancedSat) {
					for (int i = 0; i < liquidCrafter; i++) {
						int liquidLeft = left + i * 40;
						renderFluidText(liquidLeft, top, i);
					}
					if (craftingModule.clientSideSatelliteNames.liquidSatelliteName.isEmpty()) {
						guiGraphics.fill(left + 3, top + 3, left + 3 + (liquidCrafter * 40), top + 138, 0xAA8B8B8B);
						guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Off"), left + (liquidCrafter * 40) / 2 - 5, top + 145, 0x404040, false);
						for (int i = 0; i < liquidCrafter; i++) {
							for (int j = 0; j < 8; j++) {
								liquidGuiParts[i][j].active = false;
							}
						}
					} else {
						guiGraphics.drawString(minecraft.font, craftingModule.clientSideSatelliteNames.liquidSatelliteName, left + (liquidCrafter * 40) / 2 + 3 - (font.width(craftingModule.clientSideSatelliteNames.liquidSatelliteName) / 2), top + 145, 0x404040, false);
						for (int i = 0; i < liquidCrafter; i++) {
							for (int j = 0; j < 8; j++) {
								liquidGuiParts[i][j].active = true;
							}
						}
					}
				} else {
					renderFluidText(left, top, id);
				}
			}
		}

		private void renderFluidText(int left, int top, int i) {
			LPGuiGraphics.drawSlotBackground(minecraft, left + 12, top + 19);
			final String liquidAmount = liquidAmountsOverlay.read(intList -> intList.get(i).toString());
			guiGraphics.drawString(minecraft.font, liquidAmount, left + 22 - (font.width(liquidAmount) / 2), top + 40, 0x404040, false);
			guiGraphics.drawString(minecraft.font, "1", left + 19, top + 53, 0x404040, false);
			guiGraphics.drawString(minecraft.font, "10", left + 16, top + 73, 0x404040, false);
			guiGraphics.drawString(minecraft.font, "100", left + 13, top + 93, 0x404040, false);
			guiGraphics.drawString(minecraft.font, "1000", left + 10, top + 113, 0x404040, false);
			if (isAdvancedSat) {
				if (craftingModule.clientSideSatelliteNames.liquidSatelliteNameArray[i].isEmpty()) {
					guiGraphics.fill(left + 3, top + 3, left + 42, top + 138, 0xAA8B8B8B);
					guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiCraftingPipe.PREFIX + "Off"), left + 15, top + 146, 0x404040, false);
					for (int j = 0; j < 8; j++) {
						liquidGuiParts[i][j].active = false;
					}
				} else {
					String name = craftingModule.clientSideSatelliteNames.liquidSatelliteNameArray[i];
					name = TextUtil.getTrimmedString(name, 40, minecraft.font, "...");
					guiGraphics.drawString(minecraft.font, name, left + 22 - (font.width(name) / 2), top + 146, 0x404040, false);
					for (int j = 0; j < 8; j++) {
						liquidGuiParts[i][j].active = true;
					}
				}
				guiGraphics.fill(left + 3, top + 138, left + 42, top + 139, 0xff8B8B8B);
			}
			if (((IItemIdentifierInventory) craftingModule.liquidInventory).getItem(i).isEmpty() && !((!isAdvancedSat && craftingModule.clientSideSatelliteNames.liquidSatelliteName.isEmpty()) || (isAdvancedSat && craftingModule.clientSideSatelliteNames.liquidSatelliteNameArray[i].isEmpty()))) {
				guiGraphics.fill(left + 3, top + 50, left + 42, top + 138, 0xAA8B8B8B);
				for (int j = 0; j < 8; j++) {
					liquidGuiParts[i][j].active = false;
				}
			}
		}

		@Override
		public boolean renderSelectSlot(int slotId) {
			if ((isAdvancedSat && craftingModule.clientSideSatelliteNames.liquidSatelliteNameArray[id].isEmpty()) || (!isAdvancedSat && craftingModule.clientSideSatelliteNames.liquidSatelliteName.isEmpty())) {
				return false;
			}
			return super.renderSelectSlot(slotId);
		}
	}

	private void renderUpgradeIcon(String upgradeName, int left, int top) {
		guiGraphics.renderItem(new ItemStack(ItemUpgrade.getAndCheckUpgrade(LPItems.upgrades.get(upgradeName))), left + 5, top + 5);
	}

	private final class ByproductExtension extends GuiExtension {

		@Override
		public int getFinalWidth() {
			return 40;
		}

		@Override
		public int getFinalHeight() {
			return 55;
		}

		@Override
		public void renderForeground(int left, int top) {
			if (!isFullyExtended()) {
				renderUpgradeIcon(CraftingByproductUpgrade.getName(), left, top);
			}
			if (isFullyExtended()) {
				LPGuiGraphics.drawBigSlotBackground(minecraft, left + 9, top + 20);
			}
		}
	}

	private final class CleanupExtension extends GuiExtension {

		@Override
		public int getFinalWidth() {
			return 66;
		}

		@Override
		public int getFinalHeight() {
			return cleanupSize * 18 + 16 + 30;
		}

		@Override
		public void renderForeground(int left, int top) {
			if (!isFullyExtended()) {
				renderUpgradeIcon(CraftingCleanupUpgrade.getName(), left, top);
			}
			if (isFullyExtended()) {
				for (int y = 0; y < cleanupSize; y++) {
					for (int x = 0; x < 3; x++) {
						LPGuiGraphics.drawSlotBackground(minecraft, left + 8 + x * 18, top + 8 + y * 18);
					}
				}
			}
		}
	}
}
