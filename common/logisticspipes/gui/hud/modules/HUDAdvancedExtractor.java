package logisticspipes.gui.hud.modules;
import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;





import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.hud.BasicHUDButton;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;

public class HUDAdvancedExtractor implements IHUDModuleRenderer {

	private final List<IHUDButton> buttons = new ArrayList<>();
	private final AsyncAdvancedExtractor module;
	private int selected = 0;

	public HUDAdvancedExtractor(AsyncAdvancedExtractor moduleAdvancedExtractor) {
		module = moduleAdvancedExtractor;
		buttons.add(new TabButton("Side", 0, -30, -50, 25, 10));
		buttons.add(new TabButton("Inv", 1, -5, -50, 25, 10));
	}

	@Override
	public void renderContent(boolean shifted) {
		Minecraft mc = Minecraft.getInstance();
		GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		if (selected == 0) {
			Direction d = module.getSneakyDirection();
			if (gg != null) {
				SimpleGraphics.drawString(gg, mc.font, "Extract", -22, -22, 0xff000000, false);
				SimpleGraphics.drawString(gg, mc.font, "from:", -22, -9, 0xff000000, false);
				SimpleGraphics.drawString(gg, mc.font, d == null ? "DEFAULT" : d.name(), -22, 18, 0xff000000, false);
			}
		} else {
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(
					ItemIdentifierStack.getListFromInventory(module.getFilterInventory()), null, 0, -25, -32, 3, 9, 18,
					18, 100.0F, DisplayAmount.NEVER, false, shifted);
			if (gg != null) {
				SimpleGraphics.drawString(gg, mc.font, module.getItemsIncluded().getValue() ? "Included" : "Excluded", -22, 25, 0xff000000, false);
			}
		}
	}

	@Override
	public List<IHUDButton> getButtons() {
		return buttons;
	}

	private class TabButton extends BasicHUDButton {

		private final int mode;

		public TabButton(String name, int mode, int x, int y, int width, int height) {
			super(name, x, y, width, height);
			this.mode = mode;
		}

		@Override
		public void clicked() {
			selected = mode;
		}

		@Override
		public void renderButton(boolean hover, boolean clicked, boolean shifted) {
			Minecraft mc = Minecraft.getInstance();
			LPGuiGraphics.setHudColor(false, hover ? 127 : 64);
			LPGuiGraphics.drawGuiBackGround(mc,
					posX * 2,
					posY * 2,
					(posX + sizeX) * 2,
					(posY + sizeY) * 2 + 15,
					0,
					false,
					true,
					true,
					false,
					true);
			LPGuiGraphics.setHudColor(false, 127);

			int color;
			if (hover && !clicked) {
				color = Color.getValue(Color.LIGHT_YELLOW);
			} else if (!clicked) {
				color = Color.getValue(Color.BLACK);
			} else {
				color = Color.getValue(Color.DARK_GREY);
			}
			GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
			if (gg != null) {
				int tx = -(mc.font.width(label) / 2) + posX + sizeX / 2;
				int ty = posY + (sizeY - 8) / 2 + 2;
				SimpleGraphics.drawString(gg, mc.font, label, tx, ty, color, false);
			}
		}

		@Override
		public boolean shouldRenderButton() {
			return true;
		}

		@Override
		public boolean buttonEnabled() {
			return mode != selected;
		}
	}
}
