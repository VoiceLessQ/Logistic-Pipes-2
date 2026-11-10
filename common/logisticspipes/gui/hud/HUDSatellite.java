package logisticspipes.gui.hud;

import logisticspipes.utils.gui.SimpleGraphics;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;



import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.hud.BasicHUDButton;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import network.rs485.logisticspipes.SatellitePipe;

public class HUDSatellite extends BasicHUDGui {

	@Nonnull
	private final SatellitePipe pipe;
	private int page;

	public HUDSatellite(@Nonnull SatellitePipe pipe) {
		this.pipe = pipe;
		addRenderableWidget(new BasicHUDButton("<", -2, -40, 8, 8) {

			@Override
			public void clicked() {
				if (page > 0) {
					page--;
				}
			}

			@Override
			public boolean shouldRenderButton() {
				return HUDSatellite.this.pipe.getItemList().size() > 0;
			}

			@Override
			public boolean buttonEnabled() {
				return page > 0;
			}
		});
		addRenderableWidget(new BasicHUDButton(">", 37, -40, 8, 8) {

			@Override
			public void clicked() {
				if (page + 1 < getMaxPage()) {
					page++;
				}
			}

			@Override
			public boolean shouldRenderButton() {
				return HUDSatellite.this.pipe.getItemList().size() > 0;
			}

			@Override
			public boolean buttonEnabled() {
				return page + 1 < getMaxPage();
			}
		});
	}

	@Override
	public void renderHeadUpDisplay(double distance, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
		net.minecraft.client.gui.GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		int textColor = 0xff000000;
		if (pipe.getItemList().size() > 0) {
			LPGuiGraphics.setHudColor(day, 64);
			LPGuiGraphics.drawGuiBackGround(minecraft, -50, -50, 50, 50, 0, false);
			LPGuiGraphics.setHudColor(day, 127);
			super.renderHeadUpDisplay(distance, day, shifted, minecraft, config);

			String message = pipe.getSatellitePipeName();
			if (gg != null) {
				if (minecraft.font.width(message) > 40) {
					gg.pose().pushMatrix();
					gg.pose().scale(0.45F, 0.45F);
					SimpleGraphics.drawString(gg, minecraft.font, message, -100, -85, textColor, false);
					gg.pose().popMatrix();
				} else {
					SimpleGraphics.drawString(gg, minecraft.font, message, -42, -40, textColor, false);
				}
			}
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(pipe.getItemList(), null, page, -35, -20, 4, 12, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
			if (gg != null) {
				SimpleGraphics.drawString(gg, minecraft.font, String.format("(%d/%d)", page + 1, getMaxPage()), 9, -41, textColor, false);
			}
		} else {
			LPGuiGraphics.setHudColor(day, 64);
			LPGuiGraphics.drawGuiBackGround(minecraft, -50, -15, 50, 20, 0, false);
			LPGuiGraphics.setHudColor(day, 127);
			super.renderHeadUpDisplay(distance, day, shifted, minecraft, config);
			String message = pipe.getSatellitePipeName();
			if (gg != null) {
				SimpleGraphics.drawString(gg, minecraft.font, message, -(minecraft.font.width(message) / 2), -2, textColor, false);
			}
		}
	}

	public int getMaxPage() {
		int ret = pipe.getItemList().size() / 12;
		if (pipe.getItemList().size() % 12 != 0 || ret == 0) {
			ret++;
		}
		return ret;
	}

	@Override
	public boolean display(IHUDConfig config) {
		return config.isHUDSatellite();
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		if (pipe.getItemList().size() > 0) {
			return -50 < x && x < 50 && -50 < y && y < 50;
		} else {
			return -50 < x && x < 50 && -15 < y && y < 20;
		}
	}
}
