package logisticspipes.gui.popup;

import net.minecraft.client.gui.GuiGraphics;

import java.awt.Rectangle;
import java.util.List;

import net.minecraft.client.Minecraft;


import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.upgrade.SneakyUpgradeSidePacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.UpgradeSlot;
import logisticspipes.utils.gui.sideconfig.SideConfigDisplay;
import network.rs485.logisticspipes.util.TextUtil;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class SneakyConfigurationPopup extends SubGuiScreen {

	private static final String PREFIX = "gui.pipecontroller.popup.";

	private SideConfigDisplay configDisplay;
	private List<DoubleCoordinates> config;
	private Rectangle bounds;
	private UpgradeSlot pos;

	public SneakyConfigurationPopup(List<DoubleCoordinates> config, UpgradeSlot pos) {
		super(250, 250, 0, 0);
		this.config = config;
		this.pos = pos;
	}

	@Override
	public void init() {
		super.init();
		configDisplay = new SideConfigDisplay(config) {

			@Override
			public void handleSelection(SelectedFace selection) {
				SneakyConfigurationPopup.this.handleSelection(selection);
			}
		};
		configDisplay.init();
		configDisplay.renderNeighbours = true;

		logisticspipes.utils.gui.SmallGuiButton cancel = new logisticspipes.utils.gui.SmallGuiButton(0, right - 106, bottom - 26, 100, 20, "Cancel");
		cancel.setPressListener(b -> exitGui());
		addRenderableWidget(cancel);

		bounds = new Rectangle(guiLeft + 5, guiTop + 20, this.xSize - 10, this.ySize - 50);
	}

	public void handleSelection(SideConfigDisplay.SelectedFace selection) {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(SneakyUpgradeSidePacket.class).setSide(selection.face).setSlot(pos));
		this.exitGui();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0xff000000);

		Minecraft mc = Minecraft.getInstance();
		int vpx = bounds.x * (int) Minecraft.getInstance().getWindow().getGuiScale();
		int vpy = (bounds.y + 10) * (int) Minecraft.getInstance().getWindow().getGuiScale();
		int w = bounds.width * (int) Minecraft.getInstance().getWindow().getGuiScale();
		int h = (bounds.height - 1) * (int) Minecraft.getInstance().getWindow().getGuiScale();

		guiGraphics.drawString(font, TextUtil.translate(PREFIX + "sneakyTitle"), guiLeft + 8, guiTop + 8, Color.getValue(Color.DARKER_GREY), false);

		configDisplay.drawScreen(mouseX, mouseY, 0.0f, new Rectangle(vpx, vpy, w, h), bounds);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && bounds != null && bounds.contains((int) mouseX, (int) mouseY)) {
			int vpx = bounds.x * (int) Minecraft.getInstance().getWindow().getGuiScale();
			int vpy = (bounds.y + 10) * (int) Minecraft.getInstance().getWindow().getGuiScale();
			int w = bounds.width * (int) Minecraft.getInstance().getWindow().getGuiScale();
			int h = (bounds.height - 1) * (int) Minecraft.getInstance().getWindow().getGuiScale();
			configDisplay.onMouseClicked((int) mouseX, (int) mouseY, new java.awt.Rectangle(vpx, vpy, w, h));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (bounds != null && bounds.contains((int) mouseX, (int) mouseY)) {
			configDisplay.onMouseDragged(dx, dy, button);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double lpScrollX, double delta) {
		if (bounds != null && bounds.contains((int) mouseX, (int) mouseY)) {
			configDisplay.onMouseScrolled(delta);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, lpScrollX, delta);
	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(minecraft, guiLeft, guiTop, right, bottom, 0.0f, true);
	}

}
