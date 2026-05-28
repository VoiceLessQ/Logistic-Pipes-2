
package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;



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


	private static final ResourceLocation TEXTURE = new ResourceLocation("logisticspipes", "textures/gui/power_junction.png");

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float var1, int var2, int var3) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		// texture: GuiPowerProvider.TEXTURE
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(GuiPowerProvider.TEXTURE, j, k, 0, 0, imageWidth, imageHeight);
		int level = 100 - junction.getChargeState();
		guiGraphics.blit(GuiPowerProvider.TEXTURE, j + 10, k + 11 + (level * 59 / 100), 176, level * 59 / 100, 5, 59 - (level * 59 / 100));
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiPowerProvider.PREFIX + "Logistics" + junction.getBrand() + "PowerProvider"), leftPos + 25, topPos + 8, 0x404040);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiPowerProvider.PREFIX + "StoredEnergy") + ":", leftPos + 40, topPos + 25, 0x404040);
		guiGraphics.drawString(minecraft.font, StringUtils.getStringWithSpacesFromInteger(junction.getDisplayPowerLevel()) + " " + junction.getBrand(), leftPos + 40, topPos + 35, 0x404040);
		guiGraphics.drawString(minecraft.font, "/ " + StringUtils.getStringWithSpacesFromInteger(junction.getMaxStorage()) + " " + junction.getBrand(), leftPos + 40, topPos + 45, 0x404040);
	}
}
