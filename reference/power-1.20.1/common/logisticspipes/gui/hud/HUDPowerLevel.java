package logisticspipes.gui.hud;
import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;



import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.string.StringUtils;

public class HUDPowerLevel extends BasicHUDGui implements IHeadUpDisplayRenderer {

	private final IPowerLevelDisplay junction;
	private static final ResourceLocation TEXTURE = new ResourceLocation("logisticspipes", "textures/gui/power_junction.png");

	public HUDPowerLevel(IPowerLevelDisplay junction) {
		this.junction = junction;
	}

	@Override
	public void renderHeadUpDisplay(double distance, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
		LPGuiGraphics.drawGuiBackGround(minecraft, -60, -40, 60, 40, 0, false);
		super.renderHeadUpDisplay(distance, day, shifted, minecraft, config);
		GuiGraphics gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		if (gg == null) return;
		// Frame (uv 9,10 size 7x61 on 256x256 texture)
		gg.blit(TEXTURE, -50, -30, 9, 10, 7, 61);
		int level = 100 - junction.getChargeState();
		int filled = 59 - (level * 59 / 100);
		if (filled > 0) {
			// Fill bar (uv 176, level*59/100 size 5 x filled)
			gg.blit(TEXTURE, -49, -29 + (level * 59 / 100), 176, level * 59 / 100, 5, filled);
		}
	}

	@Override
	public boolean display(IHUDConfig config) {
		return !junction.isHUDInvalid() && config.isHUDPowerLevel();
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		return -60 < x && x < 60 && -40 < y && y < 40;
	}

}
