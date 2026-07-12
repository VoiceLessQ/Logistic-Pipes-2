/**
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.Container;
import net.minecraft.resources.ResourceLocation;



import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.FluidSupplierMode;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

public class GuiFluidSupplierPipe extends LogisticsBaseGuiScreen {

	private static final String PREFIX = "gui.fluidsupplier.";

	private PipeItemsFluidSupplier logic;

	public GuiFluidSupplierPipe(Container playerInventory, Container dummyInventory, PipeItemsFluidSupplier logic) {
		super(buildDummy(playerInventory, dummyInventory, logic));

		this.logic = logic;
		imageWidth = 194;
		imageHeight = 186;
	}
	private static DummyContainer buildDummy(Container playerInventory, Container dummyInventory, PipeItemsFluidSupplier logic) {
		DummyContainer dummy = new DummyContainer(playerInventory, dummyInventory);
		dummy.addNormalSlotsForPlayerInventory(18, 97);

		int xOffset = 72;
		int yOffset = 18;

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				dummy.addDummySlot(column + row * 3, xOffset + column * 18, yOffset + row * 18);
			}
		}
		return dummy;
	}


	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "TargetInv"), imageWidth / 2 - minecraft.font.width(TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "TargetInv")) / 2, 6, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "Inventory"), 18, imageHeight - 102, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "Partialrequests") + ":", imageWidth - 140, imageHeight - 112, 0x404040, false);
	}

	protected static final ResourceLocation SUPPLIER = ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/gui/supplier.png");

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float f, int x, int y) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		// texture: GuiFluidSupplierPipe.SUPPLIER
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(GuiFluidSupplierPipe.SUPPLIER, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256);
	}

	@Override
	public void init() {
		super.init();
		logisticspipes.utils.gui.SmallGuiButton partialsBtn = new logisticspipes.utils.gui.SmallGuiButton(0, width / 2 + 45, height / 2 - 25, 30, 20, logic.isRequestingPartials() ? TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "Yes") : TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "No"));
		partialsBtn.setPressListener(b -> {
			logic.setRequestingPartials(!logic.isRequestingPartials());
			b.setMessage(net.minecraft.network.chat.Component.literal(logic.isRequestingPartials() ? TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "Yes") : TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "No")));
			MainProxy.sendPacketToServer(PacketHandler.getPacket(FluidSupplierMode.class).putInt((logic.isRequestingPartials() ? 1 : 0)).setPosX(logic.getX()).setPosY(logic.getY()).setPosZ(logic.getZ()));
		});
		addRenderableWidget(partialsBtn);
	}

	@Override
	public void onClose() {
		super.onClose();
	}
}
