package logisticspipes.gui.popup;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;


import net.minecraft.core.BlockPos;

import logisticspipes.interfaces.IGUIChannelInformationReceiver;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.DeleteChannelPacket;
import logisticspipes.network.packets.gui.OpenAddChannelGUIPacket;
import logisticspipes.network.packets.gui.OpenEditChannelGUIPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.TextListDisplay;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiManageChannelPopup extends SubGuiScreen implements IGUIChannelInformationReceiver {

	private static String GUI_LANG_KEY = "gui.popup.managechannel.";

	protected final List<ChannelInformation> channelList;
	protected final TextListDisplay textList;
	private BlockPos position;

	public GuiManageChannelPopup(List<ChannelInformation> channelList, BlockPos pos) {
		super(150, 170, 0, 0);
		this.channelList = channelList;
		this.position = pos;
		this.textList = new TextListDisplay(this, 6, 16, 6, 30, 12, new TextListDisplay.List() {

			@Override
			public int getSize() {
				return channelList.size();
			}

			@Override
			public String getTextAt(int index) {
				return channelList.get(index).getName();
			}

			@Override
			public int getTextColor(int index) {
				return 0xFFFFFF;
			}
		});
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton delBtn = new SmallGuiButton(10, xCenter + 16, bottom - 27, 50, 10, "Delete");
		delBtn.setPressListener(b -> {
			int selected = textList.getSelected();
			if (selected >= 0) {
				this.setSubGui(new ActionChoicePopup(TextUtil.translate(GUI_LANG_KEY + "deletedialog.title"), TextUtil.translate(GUI_LANG_KEY + "deletedialog.yes"), () ->
						MainProxy.sendPacketToServer(PacketHandler.getPacket(DeleteChannelPacket.class).setChannelIdentifier(channelList.get(selected).getChannelIdentifier())),
						TextUtil.translate(GUI_LANG_KEY + "deletedialog.no"), () -> {}));
			}
		});
		addRenderableWidget(delBtn);
		SmallGuiButton exitBtn = new SmallGuiButton(1, xCenter + 16, bottom - 15, 50, 10, "Exit");
		exitBtn.setPressListener(b -> exitGui());
		addRenderableWidget(exitBtn);
		SmallGuiButton addBtn = new SmallGuiButton(2, xCenter - 66, bottom - 27, 50, 10, "Add");
		addBtn.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(OpenAddChannelGUIPacket.class).setBlockPos(position)));
		addRenderableWidget(addBtn);
		SmallGuiButton editBtn = new SmallGuiButton(3, xCenter - 66, bottom - 15, 50, 10, "Edit");
		editBtn.setPressListener(b -> {
			int selected = textList.getSelected();
			if (selected >= 0) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(OpenEditChannelGUIPacket.class).setIdentifier(channelList.get(selected).getChannelIdentifier().toString()).setBlockPos(position));
			}
		});
		addRenderableWidget(editBtn);
		SmallGuiButton upBtn = new SmallGuiButton(4, xCenter - 12, bottom - 27, 25, 10, "/\\");
		upBtn.setPressListener(b -> textList.scrollDown());
		addRenderableWidget(upBtn);
		SmallGuiButton dnBtn = new SmallGuiButton(5, xCenter - 12, bottom - 15, 25, 10, "\\/");
		dnBtn.setPressListener(b -> textList.scrollUp());
		addRenderableWidget(dnBtn);
	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(minecraft, guiLeft, guiTop, right, bottom, 0.0f, true);
		drawTitle(getGuiGraphics());

		textList.renderGuiBackground(mouseX, mouseY);
	}

	protected void drawTitle(GuiGraphics guiGraphics) {
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GUI_LANG_KEY + "title"), (int) (xCenter - (minecraft.font.width(TextUtil.translate(GUI_LANG_KEY + "title")) / 2f)), guiTop + 6, 0xFFFFFF, true);
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

	@Override
	public void handleChannelInformation(ChannelInformation channel, boolean flag) {
		if (!flag) {
			if (channel.getName() == null) {
				channelList.removeIf(chan -> chan.getChannelIdentifier().equals(channel.getChannelIdentifier()));
			} else {
				if (channelList.stream().anyMatch(chan -> chan.getChannelIdentifier().equals(channel.getChannelIdentifier()))) {
					channelList.stream().filter(chan -> chan.getChannelIdentifier().equals(channel.getChannelIdentifier())).forEach(chan -> {
						chan.setName(channel.getName());
						chan.setRights(channel.getRights());
						chan.setResponsibleSecurityID(channel.getResponsibleSecurityID());
					});
				} else {
					channelList.add(channel);
				}
			}
		}
	}
}
