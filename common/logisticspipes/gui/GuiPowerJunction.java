
package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;


import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;



import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.PowerJunctionCheatPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

public class GuiPowerJunction extends LogisticsBaseGuiScreen {

	private static final String PREFIX = "gui.powerjunction.";

	private final LogisticsPowerJunctionTileEntity junction;

	public GuiPowerJunction(Player player, LogisticsPowerJunctionTileEntity junction) {
		super(buildDummy(player, junction), 176, 166, 0, 0);
		this.junction = junction;
	}
	private static DummyContainer buildDummy(Player player, LogisticsPowerJunctionTileEntity junction) {
		DummyContainer dummy = new DummyContainer(player, null, junction);
		dummy.addNormalSlotsForPlayerInventory(8, 80);
		return dummy;
	}


	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiPowerJunction.PREFIX + "LogisticsPowerJunction"), 30, 8, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiPowerJunction.PREFIX + "StoredEnergy") + ":", 40, 23, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.formatNumberWithCommas(junction.getPowerLevel()) + " LP", 40, 33, 0x404040, false);
		guiGraphics.drawString(minecraft.font, "/ " + TextUtil.formatNumberWithCommas(LogisticsPowerJunctionTileEntity.MAX_STORAGE) + " LP", 40, 43, 0x404040, false);
		guiGraphics.drawString(minecraft.font, "1 MJ = 5 LP", 30, 58, 0x404040, false);
		guiGraphics.drawString(minecraft.font, "1 EU = 2 LP", 100, 58, 0x404040, false);
		guiGraphics.drawString(minecraft.font, "10 RF = 5 LP", 24, 68, 0x404040, false);
	}

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/gui/power_junction.png");

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float var1, int var2, int var3) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(GuiPowerJunction.TEXTURE, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256);
		int level = 100 - junction.getChargeState();
		int levelPixels = level * 59 / 100;
		guiGraphics.blit(GuiPowerJunction.TEXTURE, j + 10, k + 11 + levelPixels, 176.0f, levelPixels, 5, 59 - levelPixels, 256, 256);
	}

	@Override
	public void init() {
		super.init();
		if (LogisticsPipes.isDEBUG()) {
			logisticspipes.utils.gui.SmallGuiButton cheat = new logisticspipes.utils.gui.SmallGuiButton(0, leftPos + 140, topPos + 20, 20, 20, "+");
			cheat.setPressListener(b -> MainProxy.sendPacketToServer(
					PacketHandler.getPacket(PowerJunctionCheatPacket.class).setTilePos(junction)));
			addRenderableWidget(cheat);
		}
	}
}
