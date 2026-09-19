package logisticspipes.gui.popup;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;


import net.minecraft.core.BlockPos;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.RequestSatellitePipeListPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.TextListDisplay;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiSelectSatellitePopup extends SubGuiScreen {

	String GUI_LANG_KEY = "gui.popup.selectsatellite.";

	private final Consumer<UUID> handleResult;
	private List<Pair<String, UUID>> pipeList = Collections.EMPTY_LIST;
	private final TextListDisplay textList;

	public GuiSelectSatellitePopup(BlockPos pos, boolean fluidSatellites, Consumer<UUID> handleResult) {
		super(150, 170, 0, 0);
		this.handleResult = handleResult;
		this.textList = new TextListDisplay(this, 6, 16, 6, 30, 12, new TextListDisplay.List() {

			@Override
			public int getSize() {
				return pipeList.size();
			}

			@Override
			public String getTextAt(int index) {
				return pipeList.get(index).getValue1();
			}

			@Override
			public int getTextColor(int index) {
				return 0xFFFFFF;
			}
		});
		MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestSatellitePipeListPacket.class).setFlag(fluidSatellites).setBlockPos(pos));
	}

	protected void drawTitle(GuiGraphics guiGraphics) {
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "title"), xCenter - (minecraft.font.width(TextUtil.translate(GUI_LANG_KEY + "title")) / 2f), guiTop + 6, 0xFFFFFF, true);
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton sel = new SmallGuiButton(0, xCenter + 16, bottom - 27, 50, 10, TextUtil.translate(GUI_LANG_KEY + "select"));
		sel.setPressListener(b -> {
			int selected = textList.getSelected();
			if (selected >= 0) {
				handleResult.accept(pipeList.get(selected).getValue2());
				exitGui();
			}
		});
		addRenderableWidget(sel);
		SmallGuiButton ex = new SmallGuiButton(1, xCenter + 16, bottom - 15, 50, 10, TextUtil.translate(GUI_LANG_KEY + "exit"));
		ex.setPressListener(b -> exitGui());
		addRenderableWidget(ex);
		SmallGuiButton unset = new SmallGuiButton(2, xCenter - 66, bottom - 27, 50, 10, TextUtil.translate(GUI_LANG_KEY + "unset"));
		unset.setPressListener(b -> { handleResult.accept(null); exitGui(); });
		addRenderableWidget(unset);
		SmallGuiButton up = new SmallGuiButton(4, xCenter - 12, bottom - 27, 25, 10, "/\\");
		up.setPressListener(b -> textList.scrollDown());
		addRenderableWidget(up);
		SmallGuiButton dn = new SmallGuiButton(5, xCenter - 12, bottom - 15, 25, 10, "\\/");
		dn.setPressListener(b -> textList.scrollUp());
		addRenderableWidget(dn);
	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(minecraft, guiLeft, guiTop, right, bottom, 0.0f, true);
		drawTitle(getGuiGraphics());

		textList.renderGuiBackground(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double i, double j, int k) {
		textList.mouseClicked(i, j, k);
		return super.mouseClicked(i, j, k);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY < 0) {
			textList.scrollUp();
		} else if (scrollY > 0) {
			textList.scrollDown();
		} else {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		return true;
	}

	public void handleSatelliteList(List<Pair<String, UUID>> list) {
		pipeList = list;
	}
}
