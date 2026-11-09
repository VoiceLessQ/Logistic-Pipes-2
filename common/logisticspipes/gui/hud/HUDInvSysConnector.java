package logisticspipes.gui.hud;

import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.client.Minecraft;



import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class HUDInvSysConnector extends BasicHUDGui {

	private PipeItemsInvSysConnector pipe;
	private long display = System.currentTimeMillis();

	public HUDInvSysConnector(PipeItemsInvSysConnector pipe) {
		this.pipe = pipe;
	}

	@Override
	public void renderHeadUpDisplay(double distance, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
		LPGuiGraphics.setHudColor(day, 64);
		LPGuiGraphics.drawGuiBackGround(minecraft, -50, -50, 50, 50, 0, false);
		LPGuiGraphics.setHudColor(day, 127);
		super.renderHeadUpDisplay(distance, day, shifted, minecraft, config);
		net.minecraft.client.gui.GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		if (gg != null) {
			int textColor = 0xff000000;
			SimpleGraphics.drawString(gg, minecraft.font, "Expected:", -28, -25, textColor, false);
		}
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(pipe.displayList, null, 0, -37, -18, 3, 9, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
	}

	@Override
	public boolean display(IHUDConfig config) {
		if (!config.isHUDInvSysCon()) {
			return false;
		}
		if (display > System.currentTimeMillis()) {
			return true;
		}
		if (pipe.displayList.size() > 0) {
			display = System.currentTimeMillis() + (2 * 1000);
		}
		return pipe.displayList.size() > 0;
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		return (-50 < x && x < 50 && -50 < y && y < 50);
	}
}
