package logisticspipes.gui.popup;

import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.nbt.CompoundTag;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.SaveSecurityPlayerPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiSecurityStationPopup extends SubGuiScreen {

	private static final String PREFIX = "gui.securitystation.popup.player.";

	private final LogisticsSecurityTileEntity _tile;
	private final SecuritySettings activeSetting;
	private GuiCheckBox cb0, cb1, cb2, cb3, cb4, cb5;

	public GuiSecurityStationPopup(SecuritySettings setting, LogisticsSecurityTileEntity tile) {
		super(160, 135, 0, 0);
		activeSetting = setting;
		_tile = tile;
	}

	@Override
	public void init() {
		super.init();
		cb0 = new GuiCheckBox(0, guiLeft + 138, guiTop + 26, 16, 16, false);
		cb1 = new GuiCheckBox(1, guiLeft + 138, guiTop + 41, 16, 16, false);
		cb2 = new GuiCheckBox(2, guiLeft + 138, guiTop + 56, 16, 16, false);
		cb3 = new GuiCheckBox(3, guiLeft + 138, guiTop + 71, 16, 16, false);
		cb4 = new GuiCheckBox(4, guiLeft + 138, guiTop + 86, 16, 16, false);
		cb5 = new GuiCheckBox(5, guiLeft + 138, guiTop + 101, 16, 16, false);
		cb0.setPressListener(b -> { activeSetting.openGui = !activeSetting.openGui; refreshCheckBoxes(); sendSettings(); });
		cb1.setPressListener(b -> { activeSetting.openRequest = !activeSetting.openRequest; refreshCheckBoxes(); sendSettings(); });
		cb2.setPressListener(b -> { activeSetting.openUpgrades = !activeSetting.openUpgrades; refreshCheckBoxes(); sendSettings(); });
		cb3.setPressListener(b -> { activeSetting.openNetworkMonitor = !activeSetting.openNetworkMonitor; refreshCheckBoxes(); sendSettings(); });
		cb4.setPressListener(b -> { activeSetting.removePipes = !activeSetting.removePipes; refreshCheckBoxes(); sendSettings(); });
		cb5.setPressListener(b -> { activeSetting.accessRoutingChannels = !activeSetting.accessRoutingChannels; refreshCheckBoxes(); sendSettings(); });
		addRenderableWidget(cb0);
		addRenderableWidget(cb1);
		addRenderableWidget(cb2);
		addRenderableWidget(cb3);
		addRenderableWidget(cb4);
		addRenderableWidget(cb5);
		SmallGuiButton closeBtn = new SmallGuiButton(6, guiLeft + 123, guiTop + 118, 30, 10, TextUtil.translate(GuiSecurityStationPopup.PREFIX + "Close"));
		closeBtn.setPressListener(b -> exitGui());
		addRenderableWidget(closeBtn);
		refreshCheckBoxes();
	}

	private void sendSettings() {
		CompoundTag nbt = new CompoundTag();
		activeSetting.writeToNBT(nbt);
		MainProxy.sendPacketToServer(PacketHandler.getPacket(SaveSecurityPlayerPacket.class).put(nbt).setBlockPos(_tile.getBlockPos()));
	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(minecraft, guiLeft, guiTop, right, bottom, 0.0f, true);
		SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, TextUtil.translate(GuiSecurityStationPopup.PREFIX + "Player") + ": " + activeSetting.name, guiLeft + 10, guiTop + 10, 0x404040, false);
		SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, TextUtil.translate(GuiSecurityStationPopup.PREFIX + "ConfigureSettings") + ": ", guiLeft + 10, guiTop + 30, 0x404040, false);
		SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, TextUtil.translate(GuiSecurityStationPopup.PREFIX + "ActiveRequesting") + ": ", guiLeft + 10, guiTop + 45, 0x404040, false);
		SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, TextUtil.translate(GuiSecurityStationPopup.PREFIX + "UpgradePipes") + ": ", guiLeft + 10, guiTop + 60, 0x404040, false);
		SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, TextUtil.translate(GuiSecurityStationPopup.PREFIX + "CheckNetwork") + ": ", guiLeft + 10, guiTop + 75, 0x404040, false);
		SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, TextUtil.translate(GuiSecurityStationPopup.PREFIX + "RemovePipes") + ": ", guiLeft + 10, guiTop + 90, 0x404040, false);
		SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, TextUtil.translate(GuiSecurityStationPopup.PREFIX + "AccessRoutingChannels") + ": ", guiLeft + 10, guiTop + 105, 0x404040, false);
	}

	public void refreshCheckBoxes() {
		if (cb0 == null) return;
		cb0.setState(activeSetting.openGui);
		cb1.setState(activeSetting.openRequest);
		cb2.setState(activeSetting.openUpgrades);
		cb3.setState(activeSetting.openNetworkMonitor);
		cb4.setState(activeSetting.removePipes);
		cb5.setState(activeSetting.accessRoutingChannels);
	}
}
