package logisticspipes.gui.popup;

import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.UUID;



import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.EditChannelPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.SmallGuiButton;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiEditChannelPopup extends GuiAddChannelPopup {

	private static String GUI_LANG_KEY = "gui.popup.editchannel.";
	private final UUID channelIdentifier;
	private ChannelInformation toInit;

	public GuiEditChannelPopup(UUID correspondingSecurityStationID, ChannelInformation toEdit) {
		super(correspondingSecurityStationID, 160);
		this.channelIdentifier = toEdit.getChannelIdentifier();
		toInit = toEdit;
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton editSave = new SmallGuiButton(5, guiLeft + 58, guiTop + 140, 50, 10, TextUtil.translate(GUI_LANG_KEY + "save"));
		editSave.setPressListener(b -> {
			ChannelInformation.AccessRights rights = null;
			UUID security = null;
			if (checkPublic.getState()) {
				rights = ChannelInformation.AccessRights.PUBLIC;
			} else if (checkSecurity.getState()) {
				rights = ChannelInformation.AccessRights.SECURED;
				security = responsibleSecurityID;
			} else if (checkPrivate.getState()) {
				rights = ChannelInformation.AccessRights.PRIVATE;
			}
			MainProxy.sendPacketToServer(
					PacketHandler.getPacket(EditChannelPacket.class).setChannelIdentifier(channelIdentifier).setName(this.textInput.getText()).setRights(rights).setSecurityStationID(security));
			exitGui();
		});
		addRenderableWidget(editSave);
		if (toInit != null) {
			this.textInput.setText(toInit.getName());
			checkPublic.setState(toInit.getRights() == ChannelInformation.AccessRights.PUBLIC);
			checkSecurity.setState(toInit.getRights() == ChannelInformation.AccessRights.SECURED);
			checkPrivate.setState(toInit.getRights() == ChannelInformation.AccessRights.PRIVATE);
		}
	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		super.renderGuiBackground(mouseX, mouseY);
		SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, TextUtil.translate(GUI_LANG_KEY + "owner") + ": ", guiLeft + 10, guiTop + 115, 0x404040, false);
		SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, toInit.getOwner().getUsername(), guiLeft + 10, guiTop + 127, 0x404040, false);
	}

	@Override
	protected void drawTitle(GuiGraphicsExtractor guiGraphics) {
		SimpleGraphics.drawString(guiGraphics, minecraft.font, TextUtil.translate(GUI_LANG_KEY + "title"), xCenter - (minecraft.font.width(TextUtil.translate(GUI_LANG_KEY + "title")) / 2f), guiTop + 6, 0xFFFFFF, true);
	}

}
