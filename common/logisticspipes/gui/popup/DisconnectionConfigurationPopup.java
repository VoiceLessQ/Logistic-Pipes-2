package logisticspipes.gui.popup;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Rectangle;

import net.minecraft.client.Minecraft;


import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.upgrade.ToogleDisconnectionUpgradeSidePacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.UpgradeSlot;
import logisticspipes.utils.gui.sideconfig.SideConfigDisplay;
import network.rs485.logisticspipes.util.TextUtil;

public class DisconnectionConfigurationPopup extends SubGuiScreen {

	private static final String PREFIX = "gui.pipecontroller.popup.";

	private SideConfigDisplay configDisplay;
	private CoreRoutedPipe pipe;
	private Rectangle bounds;
	private UpgradeSlot pos;

	public DisconnectionConfigurationPopup(CoreRoutedPipe pipe, UpgradeSlot pos) {
		super(250, 250, 0, 0);
		this.pipe = pipe;
		this.pos = pos;
	}

	@Override
	public void init() {
		super.init();
		configDisplay = new SideConfigDisplay(pipe) {

			@Override
			public void handleSelection(SelectedFace selection) {
				DisconnectionConfigurationPopup.this.handleSelection(selection);
			}
		};
		configDisplay.init();
		configDisplay.renderNeighbours = true;

		logisticspipes.utils.gui.SmallGuiButton okBtn = new logisticspipes.utils.gui.SmallGuiButton(0, right - 106, bottom - 26, 100, 20, "OK");
		okBtn.setPressListener(b -> exitGui());
		addRenderableWidget(okBtn);

		bounds = new Rectangle(guiLeft + 5, guiTop + 20, this.xSize - 10, this.ySize - 50);
	}

	public void handleSelection(SideConfigDisplay.SelectedFace selection) {
		//ItemStack stack = pipe.getOriginalUpgradeManager().getInv().getItem(pos);
		MainProxy.sendPacketToServer(PacketHandler.getPacket(ToogleDisconnectionUpgradeSidePacket.class).setSide(selection.face).setSlot(pos));
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0xff000000);

		Minecraft mc = Minecraft.getInstance();
		int vpx = bounds.x * (int) Minecraft.getInstance().getWindow().getGuiScale();
		int vpy = (bounds.y + 10) * (int) Minecraft.getInstance().getWindow().getGuiScale();
		int w = bounds.width * (int) Minecraft.getInstance().getWindow().getGuiScale();
		int h = (bounds.height - 1) * (int) Minecraft.getInstance().getWindow().getGuiScale();

		guiGraphics.drawString(minecraft.font, TextUtil.translate(PREFIX + "disconnectTitle"), guiLeft + 8, guiTop + 8, logisticspipes.utils.Color.getValue(logisticspipes.utils.Color.DARKER_GREY), false);

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
