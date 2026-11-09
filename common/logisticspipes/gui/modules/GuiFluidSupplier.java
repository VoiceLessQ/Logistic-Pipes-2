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
import net.minecraft.resources.Identifier;



import logisticspipes.modules.ModuleFluidSupplier;
import logisticspipes.utils.gui.DummyContainer;
import javax.annotation.Nonnull;

public class GuiFluidSupplier extends ModuleBaseGui {

	private final ModuleFluidSupplier _liquidSupplier;

	public GuiFluidSupplier(Container playerInventory, ModuleFluidSupplier module) {
		super(buildDummy(playerInventory, module), module);
		_liquidSupplier = module;
		imageWidth = 175;
		imageHeight = 142;
	}
	private static DummyContainer buildDummy(Container playerInventory, ModuleFluidSupplier module) {
		DummyContainer dummy = new DummyContainer(playerInventory, module.getFilterInventory());
		dummy.addNormalSlotsForPlayerInventory(8, 60);

		//Pipe slots
		for (int pipeSlot = 0; pipeSlot < 9; pipeSlot++) {
			dummy.addDummySlot(pipeSlot, 8 + pipeSlot * 18, 18);
		}
		return dummy;
	}


	@Override
	protected void renderLabels(GuiGraphicsExtractor guiGraphics, int par1, int par2) {
		SimpleGraphics.drawString(guiGraphics, minecraft.font, ((network.rs485.logisticspipes.inventory.IItemIdentifierInventory) _liquidSupplier.getFilterInventory()).getName(), 8, 6, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, "Inventory", 8, imageHeight - 92, 0x404040, false);
	}

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/itemsink.png");

	@Override
	protected void renderBg(@Nonnull GuiGraphicsExtractor guiGraphics, float f, int x, int y) {
		SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		// texture: GuiFluidSupplier.TEXTURE
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiFluidSupplier.TEXTURE, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256, SimpleGraphics.color);
	}
}
