/*
 * Copyright (c) Krapht, 2012
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import java.io.IOException;
import java.util.Locale;


import net.minecraft.world.Container;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;



import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.modules.SneakyModuleDirectionUpdate;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import network.rs485.logisticspipes.module.SneakyDirection;
import javax.annotation.Nonnull;

public class GuiSneakyConfigurator extends ModuleBaseGui {

	private final SneakyDirection directionReceiver;
	private final logisticspipes.utils.gui.SmallGuiButton[] dirButtons = new logisticspipes.utils.gui.SmallGuiButton[7];

	public GuiSneakyConfigurator(Container playerInventory, LogisticsModule module) {
		super(new DummyContainer(playerInventory, null), module);
		if (!(module instanceof SneakyDirection)) throw new IllegalArgumentException("Module is not sneaky");
		directionReceiver = (SneakyDirection) module;
		imageWidth = 160;
		imageHeight = 200;
	}

	@Override
	public void init() {
		super.init();

		int left = width / 2 - imageWidth / 2;
		int top = height / 2 - imageHeight / 2;

		dirButtons[0] = wire(new logisticspipes.utils.gui.SmallGuiButton(0, left + 110, top + 103, 40, 20, "")); //DOWN
		dirButtons[1] = wire(new logisticspipes.utils.gui.SmallGuiButton(1, left + 110, top + 43, 40, 20, "")); //UP
		dirButtons[2] = wire(new logisticspipes.utils.gui.SmallGuiButton(2, left + 50, top + 53, 50, 20, "")); //NORTH
		dirButtons[3] = wire(new logisticspipes.utils.gui.SmallGuiButton(3, left + 50, top + 93, 50, 20, "")); //SOUTH
		dirButtons[4] = wire(new logisticspipes.utils.gui.SmallGuiButton(4, left + 10, top + 73, 40, 20, "")); //WEST
		dirButtons[5] = wire(new logisticspipes.utils.gui.SmallGuiButton(5, left + 100, top + 73, 40, 20, "")); //EAST
		dirButtons[6] = wire(new logisticspipes.utils.gui.SmallGuiButton(6, left + 10, top + 23, 60, 20, "")); //DEFAULT
		for (logisticspipes.utils.gui.SmallGuiButton b : dirButtons) addRenderableWidget(b);

		refreshButtons();
	}

	private logisticspipes.utils.gui.SmallGuiButton wire(logisticspipes.utils.gui.SmallGuiButton btn) {
		btn.setPressListener(b -> {
			directionReceiver.setSneakyDirection(b.id == 6 ? null : Direction.from3DDataValue(b.id));
			MainProxy.sendPacketToServer(PacketHandler.getPacket(SneakyModuleDirectionUpdate.class).setDirection(directionReceiver.getSneakyDirection()).setModulePos(module));
			refreshButtons();
		});
		return btn;
	}

	private void refreshButtons() {
		for (logisticspipes.utils.gui.SmallGuiButton button : dirButtons) {
			if (button == null) continue;
			button.setMessage(net.minecraft.network.chat.Component.literal(getButtonOrientationString(button.id == 6 ? null : Direction.from3DDataValue(button.id))));
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {

		refreshButtons();

		super.renderLabels(guiGraphics, par1, par2);

		guiGraphics.drawString(minecraft.font, "Sneaky orientation", imageWidth / 2 - minecraft.font.width("Sneaky orientation") / 2, 10, 0x404040, false);
	}

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/gui/sneaky.png");

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float f, int x, int y) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		// texture: GuiSneakyConfigurator.TEXTURE
		int j = leftPos;
		int k = topPos;
		//guiGraphics.fill(width/2 - imageWidth / 2, height / 2 - imageHeight /2, width/2 + imageWidth / 2, height / 2 + imageHeight /2, 0xFF404040);
		guiGraphics.blit(GuiSneakyConfigurator.TEXTURE, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256);
	}

	private String getButtonOrientationString(Direction orientation) {
		String s = (orientation == null ? "DEFAULT" : orientation.name());
		if (orientation == directionReceiver.getSneakyDirection()) {
			return "\u00a7a>" + s + "<";
		}
		return s.toLowerCase(Locale.US);
	}

}
