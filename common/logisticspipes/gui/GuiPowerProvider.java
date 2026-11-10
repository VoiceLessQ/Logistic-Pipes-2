
package logisticspipes.gui;

import net.minecraft.client.renderer.RenderPipelines;
import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;



import logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.string.StringUtils;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

public class GuiPowerProvider extends LogisticsBaseGuiScreen {

	private static final String PREFIX = "gui.powerprovider.";

	private final LogisticsPowerProviderTileEntity junction;

	public GuiPowerProvider(Player player, LogisticsPowerProviderTileEntity junction) {
		super(buildDummy(player, junction), 176, 166, 0, 0);
		this.junction = junction;
	}
	private static DummyContainer buildDummy(Player player, LogisticsPowerProviderTileEntity junction) {
		DummyContainer dummy = new DummyContainer(player, null, junction);
		dummy.addNormalSlotsForPlayerInventory(8, 80);
		return dummy;
	}


	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/power_junction.png");

	@Override
	protected void renderBg(@Nonnull GuiGraphicsExtractor guiGraphics, float var1, int var2, int var3) {
		SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiPowerProvider.TEXTURE, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256, SimpleGraphics.color);
		int level = 100 - junction.getChargeState();
		int levelPixels = level * 59 / 100;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiPowerProvider.TEXTURE, j + 10, k + 11 + levelPixels, 176.0f, levelPixels, 5, 59 - levelPixels, 256, 256, SimpleGraphics.color);
	}

	@Override
	protected void renderLabels(@Nonnull GuiGraphicsExtractor guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, TextUtil.translate(GuiPowerProvider.PREFIX + "Logistics" + junction.getBrand() + "PowerProvider"), 25, 8, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, TextUtil.translate(GuiPowerProvider.PREFIX + "StoredEnergy") + ":", 40, 25, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, StringUtils.getStringWithSpacesFromInteger(junction.getDisplayPowerLevel()) + " " + junction.getBrand(), 40, 35, 0x404040, false);
		SimpleGraphics.drawString(guiGraphics, minecraft.font, "/ " + StringUtils.getStringWithSpacesFromInteger(junction.getMaxStorage()) + " " + junction.getBrand(), 40, 45, 0x404040, false);
	}
}
