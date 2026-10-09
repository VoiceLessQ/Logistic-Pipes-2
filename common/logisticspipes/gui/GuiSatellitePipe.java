/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui;

import java.io.IOException;
import javax.annotation.Nonnull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.satpipe.SatelliteSetNamePacket;
import logisticspipes.pipes.SatelliteNamingResult;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import network.rs485.logisticspipes.SatellitePipe;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiSatellitePipe extends LogisticsBaseGuiScreen {

	@Nonnull
	private final SatellitePipe satellitePipe;

	@Nonnull
	private String response = "";

	private InputBar input;

	public GuiSatellitePipe(@Nonnull SatellitePipe satellitePipe) {
		super(new DummyContainer(null, null));
		imageWidth = 116;
		imageHeight = 77;
		this.satellitePipe = satellitePipe;
	}

	@Override
	public void init() {
		

		super.init();
		SmallGuiButton saveBtn = new SmallGuiButton(0, (width / 2) - (30 / 2) + 35, (height / 2) + 20, 30, 10, "Save");
		saveBtn.setPressListener(b -> MainProxy.sendPacketToServer(
				PacketHandler.getPacket(SatelliteSetNamePacket.class).setString(input.getText()).setTilePos(satellitePipe.getContainer())));
		addRenderableWidget(saveBtn);
		input = new InputBar(font, this, leftPos + 8, topPos + 40, 100, 16);
	}

	@Override
	public void closeGui() throws IOException {
		super.closeGui();
		
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		guiGraphics.drawCenteredString(font, TextUtil.translate("gui.satellite.SatelliteName"), 59, 7, 0x404040);
		String name = TextUtil.getTrimmedString(satellitePipe.getSatellitePipeName(), 100, minecraft.font, "...");
		int yOffset = 0;
		if (!response.isEmpty()) {
			guiGraphics.drawCenteredString(font, TextUtil.translate("gui.satellite.naming_result." + response), imageWidth / 2, 30, response.equals("success") ? 0x404040 : 0x5c1111);
			yOffset = 4;
		}
		guiGraphics.drawCenteredString(font, name, imageWidth / 2, 24 - yOffset, 0x404040);
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float f, int x, int y) {
		super.renderBg(guiGraphics, f, x, y);
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		input.drawTextBox();
	}

	@Override
	public boolean mouseClicked(double x, double y, int k) {
		if (!input.handleClick(x, y, k)) {
			return super.mouseClicked(x, y, k);
		}
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (input.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (!input.handleKey(c, i)) {
			return super.charTyped(c, i);
		}
		return true;
	}

	public void handleResponse(SatelliteNamingResult result, String newName) {
		response = result.toString();
		if (result == SatelliteNamingResult.SUCCESS) {
			satellitePipe.setSatellitePipeName(newName);
		}
	}
}
