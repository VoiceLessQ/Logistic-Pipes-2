package logisticspipes.gui.hud;
import net.minecraft.client.renderer.RenderPipelines;
import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;


import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;



import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.string.StringUtils;

public class HUDPowerLevel extends BasicHUDGui implements IHeadUpDisplayRenderer {

	private final IPowerLevelDisplay junction;
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/power_junction.png");

	public HUDPowerLevel(IPowerLevelDisplay junction) {
		this.junction = junction;
	}

	@Override
	public void renderHeadUpDisplay(double distance, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
		LPGuiGraphics.setHudColor(day, 32);
		LPGuiGraphics.drawGuiBackGround(minecraft, -60, -40, 60, 40, 0, false);
		LPGuiGraphics.setHudColor(day, 127);
		super.renderHeadUpDisplay(distance, day, shifted, minecraft, config);
		GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		if (gg == null) return;
		// Frame (uv 9,10 size 7x61 on 256x256 texture)
		gg.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, -50, -30, 9.0f, 10.0f, 7, 61, 256, 256, SimpleGraphics.color);
		int level = 100 - junction.getChargeState();
		int filled = 59 - (level * 59 / 100);
		if (filled > 0) {
			// Fill bar (uv 176, level*59/100 size 5 x filled)
			gg.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, -49, -29 + (level * 59 / 100), 176.0f, (float)(level * 59 / 100), 5, filled, 256, 256, SimpleGraphics.color);
		}
		SimpleGraphics.drawString(gg, minecraft.font, "Stored Energy:", -30, -15, 0xff404040, false);
		SimpleGraphics.drawString(gg, minecraft.font, StringUtils.getStringWithSpacesFromInteger(junction.getDisplayPowerLevel()) + " " + junction.getBrand(), -30, -5, 0xff404040, false);
		SimpleGraphics.drawString(gg, minecraft.font, "/ " + StringUtils.getStringWithSpacesFromInteger(junction.getMaxStorage()) + " " + junction.getBrand(), -30, 5, 0xff404040, false);
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
