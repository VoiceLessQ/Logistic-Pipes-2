/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui;

import logisticspipes.utils.gui.SimpleGraphics;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import logisticspipes.config.Configs;
import logisticspipes.items.ItemModule;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.pipe.ChassisGuiProvider;
import logisticspipes.network.packets.chassis.ChassisGUI;
import logisticspipes.network.packets.gui.GuiClosePacket;
import logisticspipes.network.packets.gui.OpenUpgradePacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.upgrades.ModuleUpgradeManager;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.string.StringUtils;
import network.rs485.logisticspipes.module.Gui;
import javax.annotation.Nonnull;

public class GuiChassisPipe extends LogisticsBaseGuiScreen {

	private final PipeLogisticsChassis chassisPipe;
	private final Container _moduleInventory;
	//private final Screen _previousGui;
	private final List<SmallGuiButton> moduleConfigButtons = new LinkedList<>();

	private final Slot[] upgradeSlots = new Slot[2 * Configs.CHASSIS_SLOTS_ARRAY[4]];
	private net.minecraft.client.gui.components.AbstractButton[] upgradeConfig;

	private final boolean hasUpgradeModuleUpgrade;

	public GuiChassisPipe(Player player, PipeLogisticsChassis chassis, boolean hasUpgradeModuleUpgrade) { //, Screen previousGui) {
		super(buildDummy(player, chassis, hasUpgradeModuleUpgrade));
		chassisPipe = chassis;
		_moduleInventory = chassis.getModuleInventory();
		//_previousGui = previousGui;
		this.hasUpgradeModuleUpgrade = hasUpgradeModuleUpgrade;

		int playerInventoryWidth = 162;
		int playerInventoryHeight = 76;

		imageWidth = playerInventoryWidth + 26;
		imageHeight = playerInventoryHeight + 14 + (20 * chassisPipe.getChassisSize());

	}
	private static DummyContainer buildDummy(Player player, PipeLogisticsChassis chassis, boolean hasUpgradeModuleUpgrade) {
		Container moduleInventory = chassis.getModuleInventory();
		DummyContainer dummy = new DummyContainer(player.getInventory(), moduleInventory);
		dummy.addNormalSlotsForPlayerInventory(18, 9 + 20 * chassis.getChassisSize());
		for (int i = 0; i < chassis.getChassisSize(); i++)
			dummy.addModuleSlot(i, moduleInventory, 18, 9 + 20 * i, chassis);

		if (hasUpgradeModuleUpgrade) {
			for (int i = 0; i < chassis.getChassisSize(); i++) {
				final int fI = i;
				ModuleUpgradeManager upgradeManager = chassis.getModuleUpgradeManager(i);
				dummy.addUpgradeSlot(0, upgradeManager, 0, 145, 9 + i * 20, itemStack -> ChassisGuiProvider.checkStack(itemStack, chassis, fI));
				dummy.addUpgradeSlot(1, upgradeManager, 1, 165, 9 + i * 20, itemStack -> ChassisGuiProvider.checkStack(itemStack, chassis, fI));
			}
		}
		return dummy;
	}


	@Override
	public void init() {
		super.init();

		int left = width / 2 - imageWidth / 2;
		int top = height / 2 - imageHeight / 2;

		moduleConfigButtons.clear();
		upgradeConfig = new net.minecraft.client.gui.components.Button[chassisPipe.getChassisSize() * 2];
		for (int i = 0; i < chassisPipe.getChassisSize(); i++) {
			final int slot = i;
			SmallGuiButton cfgBtn = new SmallGuiButton(i, left + 5, top + 12 + 20 * i, 10, 10, "!");
			cfgBtn.setPressListener(b -> {
				LogisticsModule module = chassisPipe.getSubModule(slot);
				if (module != null) {
					MainProxy.sendPacketToServer(PacketHandler.getPacket(ChassisGUI.class).setButtonID(slot)
							.setPosX(chassisPipe.getX()).setPosY(chassisPipe.getY()).setPosZ(chassisPipe.getZ()));
				}
			});
			moduleConfigButtons.add(addRenderableWidget(cfgBtn));
			if (_moduleInventory == null) {
				continue;
			}
			updateModuleConfigButtonVisibility(i);

			if (hasUpgradeModuleUpgrade) {
				final int idxA = i * 2;
				SmallGuiButton upA = new SmallGuiButton(100 + i, leftPos + 134, topPos + 12 + i * 20, 10, 10, "!");
				upA.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(OpenUpgradePacket.class).setSlot(upgradeSlots[idxA])));
				upgradeConfig[idxA] = addRenderableWidget(upA);
				upgradeConfig[idxA].visible = chassisPipe.getModuleUpgradeManager(i).hasGuiUpgrade(0);
				final int idxB = i * 2 + 1;
				SmallGuiButton upB = new SmallGuiButton(120 + i, leftPos + 182, topPos + 12 + i * 20, 10, 10, "!");
				upB.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(OpenUpgradePacket.class).setSlot(upgradeSlots[idxB])));
				upgradeConfig[idxB] = addRenderableWidget(upB);
				upgradeConfig[idxB].visible = chassisPipe.getModuleUpgradeManager(i).hasGuiUpgrade(1);
			}
		}
	}

	private void updateModuleConfigButtonVisibility(int slot) {
		ItemStack module = _moduleInventory.getItem(slot);
		LogisticsModule subModule = chassisPipe.getSubModule(slot);
		if (module.isEmpty() || subModule == null) {
			moduleConfigButtons.get(slot).visible = false;
		} else {
			moduleConfigButtons.get(slot).visible = subModule instanceof Gui;
		}
	}

	@Override
	public void onClose() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(GuiClosePacket.class).setTilePos(chassisPipe.container));
		super.onClose();
	}

	@Override
	protected void renderLabels(GuiGraphicsExtractor guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		for (int i = 0; i < chassisPipe.getChassisSize(); i++) {
			updateModuleConfigButtonVisibility(i);
		}
		if (hasUpgradeModuleUpgrade) {
			for (int i = 0; i < upgradeConfig.length; i++) {
				upgradeConfig[i].visible = chassisPipe.getModuleUpgradeManager(i / 2).hasGuiUpgrade(i % 2);
			}
		}
		for (int i = 0; i < chassisPipe.getChassisSize(); i++)
			SimpleGraphics.drawString(guiGraphics, minecraft.font, getModuleName(i), 40, 14 + 20 * i, 0x404040, false);
	}

	private String getModuleName(int slot) {
		if (_moduleInventory == null) {
			return "";
		}
		if (_moduleInventory.getItem(slot).isEmpty()) {
			return "";
		}
		if (!(_moduleInventory.getItem(slot).getItem() instanceof ItemModule)) {
			return "";
		}
		String name = _moduleInventory.getItem(slot).getHoverName().getString();
		if (!hasUpgradeModuleUpgrade) {
			return name;
		}
		return StringUtils.getWithMaxWidth(name, 100, font);
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphicsExtractor guiGraphics, float f, int x, int y) {
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		for (int i = 0; i < chassisPipe.getChassisSize(); i++)
			LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 17, topPos + 8 + 20 * i);

		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 18, topPos + 9 + 20 * chassisPipe.getChassisSize());

		if (hasUpgradeModuleUpgrade) {
			for (int i = 0; i < chassisPipe.getChassisSize(); i++) {
				LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 144, topPos + 8 + i * 20);
				LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 164, topPos + 8 + i * 20);
			}
		}
	}
}
