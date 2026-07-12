/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;


import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.ResourceLocation;



import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleActiveSupplier.PatternMode;
import logisticspipes.modules.ModuleActiveSupplier.SupplyMode;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.module.ModulePropertiesUpdate;
import logisticspipes.network.packets.pipe.SlotFinderOpenGuiPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.EnumProperty;
import network.rs485.logisticspipes.property.IntListProperty;
import network.rs485.logisticspipes.property.layer.PropertyLayer;
import network.rs485.logisticspipes.property.layer.PropertyOverlay;
import network.rs485.logisticspipes.property.layer.ValuePropertyOverlay;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiSupplierPipe extends LogisticsBaseGuiScreen {

	private static final String PREFIX = "gui.supplierpipe.";
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/gui/supplier.png");
	private final boolean hasPatternUpgrade;
	private final PropertyLayer propertyLayer;
	private final ModuleActiveSupplier supplierModule;
	private final PropertyOverlay<List<Integer>, IntListProperty> slotAssignmentPatternOverlay;
	private final ValuePropertyOverlay<SupplyMode, EnumProperty<SupplyMode>> requestModeOverlay;
	private final ValuePropertyOverlay<PatternMode, EnumProperty<PatternMode>> patternModeOverlay;
	private ValuePropertyOverlay<Boolean, BooleanProperty> limitedPropertyOverlay;
	private SmallGuiButton modeBtn;
	private SmallGuiButton limBtn;

	public GuiSupplierPipe(Container playerInventory, Container dummyInventory, ModuleActiveSupplier module,
			Boolean flag, int[] slots) {
		super(buildDummy(playerInventory, dummyInventory, module, flag, slots));
		hasPatternUpgrade = flag;
		supplierModule = module;

		propertyLayer = new PropertyLayer(supplierModule.getProperties());

		slotAssignmentPatternOverlay = propertyLayer.overlay(supplierModule.slotAssignmentPattern);
		slotAssignmentPatternOverlay.write((p) -> p.replaceContent(slots));
		imageWidth = 194;
		imageHeight = 186;
		patternModeOverlay = propertyLayer
				.overlay(supplierModule.patternMode);
		requestModeOverlay = propertyLayer.overlay(supplierModule.requestMode);
		limitedPropertyOverlay = propertyLayer.overlay(supplierModule.isLimited);
	}
	private static DummyContainer buildDummy(Container playerInventory, Container dummyInventory, ModuleActiveSupplier module,
			Boolean flag, int[] slots) {
		DummyContainer dummy = new DummyContainer(playerInventory, dummyInventory);
		dummy.addNormalSlotsForPlayerInventory(18, 97);

		if (flag) {
			for (int i = 0; i < 9; i++) {
				dummy.addDummySlot(i, 18 + i * 18, 20);
			}
		} else {
			int xOffset = 72;
			int yOffset = 18;
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 3; column++) {
					dummy.addDummySlot(column + row * 3, xOffset + column * 18, yOffset + row * 18);
				}
			}
		}
		return dummy;
	}


	@Override
	public void onClose() {
		super.onClose();
		propertyLayer.unregister();
		if (this.minecraft.player != null && !propertyLayer.getProperties().isEmpty()) {
			// send update to server, when there are changed properties
			MainProxy.sendPacketToServer(
					ModulePropertiesUpdate.fromPropertyHolder(propertyLayer).setModulePos(supplierModule));
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		String name;
		if (hasPatternUpgrade) {
			name = TextUtil.translate(GuiSupplierPipe.PREFIX + "TargetInvPattern");
		} else {
			name = TextUtil.translate(GuiSupplierPipe.PREFIX + "TargetInv");
		}
		guiGraphics.drawString(minecraft.font, name, imageWidth / 2 - minecraft.font.width(name) / 2, 6, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiSupplierPipe.PREFIX + "Inventory"), 18, imageHeight - 102, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiSupplierPipe.PREFIX + "RequestMode"), imageWidth - 140, imageHeight - 112, 0x404040, false);
		if (hasPatternUpgrade) {
			slotAssignmentPatternOverlay.read((slotAssignments) -> {
				for (int i = 0; i < slotAssignments.size(); i++) {
					guiGraphics.drawString(minecraft.font, Integer.toString(slotAssignments.get(i)), 22 + i * 18, 55, 0x404040, false);
				}
				return null;
			});
		}
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float f, int x, int y) {
		if (!hasPatternUpgrade) {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			// texture: GuiSupplierPipe.TEXTURE
			int j = leftPos;
			int k = topPos;
			guiGraphics.blit(GuiSupplierPipe.TEXTURE, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256);
		} else {
			LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
			for (int i = 0; i < 9; i++) {
				LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 17 + i * 18, topPos + 19);
				Slot slot = getMenu().getSlot(36 + i);
				if (slot.hasItem() && slot.getItem().getCount() > 64) {
					guiGraphics.fill(leftPos + 18 + i * 18, topPos + 20, leftPos + 34 + i * 18, topPos + 36, Color.getValue(Color.RED));
				}
			}
			LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 18, topPos + 97);
		}
	}

	@Override
	public void init() {
		super.init();
		modeBtn = new SmallGuiButton(0, width / 2 + 35, height / 2 - 25, 50, 20, getModeText());
		modeBtn.setPressListener(b -> {
			if (hasPatternUpgrade) {
				final PatternMode newMode = patternModeOverlay.write(EnumProperty::next);
				modeBtn.setMessage(net.minecraft.network.chat.Component.literal(newMode.toString()));
			} else {
				final SupplyMode newMode = requestModeOverlay.write(EnumProperty::next);
				modeBtn.setMessage(net.minecraft.network.chat.Component.literal(newMode.toString()));
			}
		});
		addRenderableWidget(modeBtn);
		if (hasPatternUpgrade) {
			limBtn = new SmallGuiButton(1, leftPos + 5, topPos + 68, 45, 10, getLimitationText());
			limBtn.setPressListener(b -> {
				limitedPropertyOverlay.write(BooleanProperty::toggle);
				limBtn.setMessage(net.minecraft.network.chat.Component.literal(getLimitationText()));
			});
			addRenderableWidget(limBtn);
			for (int i = 0; i < 9; i++) {
				final int slot = i;
				SmallGuiButton setBtn = new SmallGuiButton(i + 2, leftPos + 18 + i * 18, topPos + 40, 17, 10, "Set");
				setBtn.setPressListener(b -> MainProxy.sendPacketToServer(
						PacketHandler.getPacket(SlotFinderOpenGuiPacket.class).setSlot(slot).setModulePos(supplierModule)));
				addRenderableWidget(setBtn);
			}
		}
	}

	public void refreshMode() {
		if (modeBtn != null) modeBtn.setMessage(net.minecraft.network.chat.Component.literal(getModeText()));
		if (hasPatternUpgrade) {
			limitedPropertyOverlay = propertyLayer.overlay(supplierModule.isLimited);
			if (limBtn != null) limBtn.setMessage(net.minecraft.network.chat.Component.literal(getLimitationText()));
		}
	}

	@Nonnull
	private String getLimitationText() {
		return limitedPropertyOverlay.get() ? "Limited" : "Unlimited";
	}

	private String getModeText() {
		return (hasPatternUpgrade ? patternModeOverlay : requestModeOverlay).get().toString();
	}

}
