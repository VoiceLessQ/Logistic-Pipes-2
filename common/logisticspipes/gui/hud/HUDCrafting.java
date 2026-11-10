package logisticspipes.gui.hud;

import logisticspipes.utils.gui.SimpleGraphics;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;



import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class HUDCrafting extends BasicHUDGui {

	private final PipeItemsCraftingLogistics pipe;

	public HUDCrafting(PipeItemsCraftingLogistics pipe) {
		this.pipe = pipe;
	}

	@Override
	public void renderHeadUpDisplay(double d, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
		LPGuiGraphics.setHudColor(day, 64);
		if (pipe.displayList.size() > 0) {
			LPGuiGraphics.drawGuiBackGround(minecraft, -50, -28, 50, 30, 0, false);
		} else {
			LPGuiGraphics.drawGuiBackGround(minecraft, -30, -22, 30, 25, 0, false);
		}
		LPGuiGraphics.setHudColor(day, 127);
		super.renderHeadUpDisplay(d, day, shifted, minecraft, config);
		net.minecraft.client.gui.GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		int textColor = 0xff000000;
		if (gg != null) {
			if (pipe.displayList.size() > 0) {
				SimpleGraphics.drawString(gg, minecraft.font, "Result:", -20, -25, textColor, false);
				SimpleGraphics.drawString(gg, minecraft.font, "Todo:", -20, 0, textColor, false);
			} else {
				SimpleGraphics.drawString(gg, minecraft.font, "Result:", -25, -18, textColor, false);
			}
		}
		List<ItemIdentifierStack> list = new ArrayList<>();
		List<ItemIdentifierStack> craftables = pipe.getCraftedItems();
		if (craftables != null && craftables.size() > 0) {
			//TODO: handle multiple craftables.
			list.add(craftables.get(0));
		}
		if (pipe.displayList.size() > 0) {
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(list, null, 0, 11, -18, 1, 1, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(pipe.displayList, null, 0, 13, 3, 1, 1, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
		} else {
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(list, null, 0, -9, -1, 1, 1, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
		}
	}

	@Override
	public boolean display(IHUDConfig config) {
		return config.isHUDCrafting() && ((!pipe.hasCraftingSign() && pipe.getCraftedItems() != null) || pipe.displayList.size() > 0);
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		if (pipe.displayList.size() > 0) {
			return -50 < x && x < 50 && -28 < y && y < 30;
		} else {
			return -30 < x && x < 30 && -22 < y && y < 25;
		}
	}
}
