/**
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui;

import logisticspipes.utils.gui.SimpleGraphics;
import java.io.IOException;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.Container;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.FluidSupplierAmount;
import logisticspipes.network.packets.pipe.FluidSupplierMinMode;
import logisticspipes.network.packets.pipe.FluidSupplierMode;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeFluidSupplierMk2.MinMode;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

public class GuiFluidSupplierMk2Pipe extends LogisticsBaseGuiScreen {

	private static final String PREFIX = "gui.fluidsuppliermk2.";

	private PipeFluidSupplierMk2 logic;

	public GuiFluidSupplierMk2Pipe(Container playerInventory, Container dummyInventory, PipeFluidSupplierMk2 logic) {
		super(buildDummy(playerInventory, dummyInventory, logic));

		this.logic = logic;
		imageWidth = 184;
		imageHeight = 176;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FluidSupplierAmount.class).putInt(0).setPosX(this.logic.getX()).setPosY(this.logic.getY()).setPosZ(this.logic.getZ()));
	}
	private static DummyContainer buildDummy(Container playerInventory, Container dummyInventory, PipeFluidSupplierMk2 logic) {
		DummyContainer dummy = new DummyContainer(playerInventory, dummyInventory);
		dummy.addNormalSlotsForPlayerInventory(13, 92);

		dummy.addFluidSlot(0, dummyInventory, 60, 18);
		return dummy;
	}


	@Override
	protected void renderLabels(GuiGraphicsExtractor guiGraphics, int par1, int par2) {
		SimpleGraphics.drawString(guiGraphics, minecraft.font, TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + "TargetInv"), imageWidth / 2 - minecraft.font.width(TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + "TargetInv")) / 2, 6, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + "Inventory"), 15, imageHeight - 95, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + "Fluid") + ":", 25, 22, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + "Partial") + ":", imageWidth - 176, imageHeight - 109, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + "minMode") + ":", imageWidth - 108, imageHeight - 109, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, Integer.toString(logic.getAmount()), imageWidth / 2, 22, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, "+", 32, 39, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, "-", 32, 50, 0x404040, false);
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphicsExtractor guiGraphics, float f, int x, int y) {
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 13, topPos + 92);
		LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 59, topPos + 17);
		//SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		//minecraft.renderEngine.func_110577_a("/logisticspipes/gui/supplier.png");
		//int j = leftPos;
		//int k = topPos;
		//drawTexturedModalRect(j, k, 0, 0, imageWidth, imageHeight);
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton partialsBtn = new SmallGuiButton(0, width / 2 - 48, topPos + imageHeight - 115, 30, 20, logic.isRequestingPartials() ? TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + "Yes") : TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + "No"));
		partialsBtn.setPressListener(b -> {
			logic.setRequestingPartials(!logic.isRequestingPartials());
			b.setMessage(net.minecraft.network.chat.Component.literal(logic.isRequestingPartials() ? TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + "Yes") : TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + "No")));
			MainProxy.sendPacketToServer(PacketHandler.getPacket(FluidSupplierMode.class).putInt((logic.isRequestingPartials() ? 1 : 0)).setPosX(logic.getX()).setPosY(logic.getY()).setPosZ(logic.getZ()));
		});
		addRenderableWidget(partialsBtn);
		SmallGuiButton minModeBtn = new SmallGuiButton(1, width / 2 + 30, topPos + imageHeight - 115, 55, 20, TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + logic.getMinMode().name()));
		minModeBtn.setPressListener(b -> {
			int index = logic.getMinMode().ordinal() + 1;
			if (index >= MinMode.values().length) {
				index = 0;
			}
			logic.setMinMode(MinMode.values()[index]);
			b.setMessage(net.minecraft.network.chat.Component.literal(TextUtil.translate(GuiFluidSupplierMk2Pipe.PREFIX + logic.getMinMode().name())));
			MainProxy.sendPacketToServer(PacketHandler.getPacket(FluidSupplierMinMode.class).putInt(logic.getMinMode().ordinal()).setPosX(logic.getX()).setPosY(logic.getY()).setPosZ(logic.getZ()));
		});
		addRenderableWidget(minModeBtn);
		int[] amounts = {1, 10, 100, 1000};
		int[] ys = {topPos + 37, topPos + 48};
		int[] xs = {leftPos + 40, leftPos + 51, leftPos + 72, leftPos + 103};
		int[] widths = {10, 20, 30, 40};
		for (int col = 0; col < 4; col++) {
			for (int row = 0; row < 2; row++) {
				final int change = (row == 0 ? 1 : -1) * amounts[col];
				SmallGuiButton amtBtn = new SmallGuiButton((col + 1) * 10 + row, xs[col], ys[row], widths[col], 10, Integer.toString(amounts[col]));
				amtBtn.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(FluidSupplierAmount.class).putInt(change).setPosX(logic.getX()).setPosY(logic.getY()).setPosZ(logic.getZ())));
				addRenderableWidget(amtBtn);
			}
		}
	}

	@Override
	public void onClose() {
		super.onClose();
	}
}
