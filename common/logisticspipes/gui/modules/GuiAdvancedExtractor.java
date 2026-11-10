/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui.modules;

import net.minecraft.client.renderer.RenderPipelines;
import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.world.Container;



import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.module.AdvancedExtractorSneakyGuiPacket;
import logisticspipes.network.packets.module.ModulePropertiesUpdate;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiStringHandlerButton;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.layer.PropertyLayer;
import network.rs485.logisticspipes.property.layer.ValuePropertyOverlay;
import javax.annotation.Nonnull;

public class GuiAdvancedExtractor extends ModuleBaseGui {

	private final AsyncAdvancedExtractor _advancedExtractor;
	private final PropertyLayer propertyLayer;
	private final ValuePropertyOverlay<Boolean, BooleanProperty> itemsIncludedOverlay;

	public GuiAdvancedExtractor(Container playerInventory, AsyncAdvancedExtractor advancedExtractor) {
		super(buildDummy(playerInventory, advancedExtractor), advancedExtractor);
		_advancedExtractor = advancedExtractor;

		propertyLayer = new PropertyLayer(_advancedExtractor.getProperties());

		itemsIncludedOverlay = propertyLayer.overlay(_advancedExtractor.getItemsIncluded());

		imageWidth = 175;
		imageHeight = 142;
	}
	private static DummyContainer buildDummy(Container playerInventory, AsyncAdvancedExtractor advancedExtractor) {
		DummyContainer dummy = new DummyContainer(playerInventory, advancedExtractor.getFilterInventory());
		dummy.addNormalSlotsForPlayerInventory(8, 60);

		//Pipe slots
		for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
			dummy.addDummySlot(pipeSlot, 8 + pipeSlot * 18, 18);
		}
		return dummy;
	}


	@Override
	public void init() {
		super.init();
		//Default item toggle:
		addRenderableWidget(new GuiStringHandlerButton(0, width / 2 + 20, height / 2 - 34, 60, 20,
				() -> itemsIncludedOverlay.get() ? "Included" : "Excluded",
				() -> itemsIncludedOverlay.write(BooleanProperty::toggle)));

		logisticspipes.utils.gui.SmallGuiButton sneaky = new logisticspipes.utils.gui.SmallGuiButton(1, width / 2 - 25, height / 2 - 34, 40, 20, "Sneaky");
		sneaky.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(AdvancedExtractorSneakyGuiPacket.class).setModulePos(_advancedExtractor)));
		addRenderableWidget(sneaky);
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
	protected void renderLabels(GuiGraphicsExtractor guiGraphics, int par1, int par2) {
		SimpleGraphics.drawString(guiGraphics, minecraft.font, _advancedExtractor.getFilterInventory().getName(), 8, 6, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, "Inventory", 8, imageHeight - 92, 0x404040, false);
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphicsExtractor guiGraphics, float f, int x, int y) {
		SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		// texture: LogisticsBaseGuiScreen.ITEMSINK
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LogisticsBaseGuiScreen.ITEMSINK, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256, SimpleGraphics.color);
	}

}
