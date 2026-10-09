package logisticspipes.gui.orderer;

import java.io.IOException;


import net.minecraft.world.entity.player.Player;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.OrdererRefreshRequestPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;

public class NormalGuiOrderer extends GuiOrderer {

	private enum DisplayOptions {
		Both,
		SupplyOnly,
		CraftOnly,
	}

	protected DisplayOptions displayOptions = DisplayOptions.Both;

	public NormalGuiOrderer(int x, int y, int z, net.minecraft.resources.ResourceLocation dim, Player entityPlayer) {
		super(x, y, z, dim, entityPlayer);
		refreshItems();
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton refreshBtn = new SmallGuiButton(3, leftPos + 10, bottom - 15, 46, 10, "Refresh");
		refreshBtn.setPressListener(b -> refreshItems());
		addRenderableWidget(refreshBtn);
		addRenderableWidget(wire(new SmallGuiButton(13, leftPos + 10, bottom - 28, 46, 10, "Content"), 13));
		SmallGuiButton modeBtn = new SmallGuiButton(9, leftPos + 10, bottom - 41, 46, 10, "Both");
		modeBtn.setPressListener(b -> {
			String displayString = "";
			switch (displayOptions) {
				case Both:
					displayOptions = DisplayOptions.CraftOnly;
					displayString = "Craft";
					break;
				case CraftOnly:
					displayOptions = DisplayOptions.SupplyOnly;
					displayString = "Supply";
					break;
				case SupplyOnly:
					displayOptions = DisplayOptions.Both;
					displayString = "Both";
					break;
			}
			b.setMessage(net.minecraft.network.chat.Component.literal(displayString));
			refreshItems();
		});
		addRenderableWidget(modeBtn);
	}

	@Override
	public void refreshItems() {
		int integer;
		switch (displayOptions) {
			case Both:
				integer = 0;
				break;
			case SupplyOnly:
				integer = 1;
				break;
			case CraftOnly:
				integer = 2;
				break;
			default:
				integer = 3;
		}
		MainProxy.sendPacketToServer(PacketHandler.getPacket(OrdererRefreshRequestPacket.class).putInt(integer).setPosX(xCoord).setPosY(yCoord).setPosZ(zCoord).setDimension(dimension));
	}

	@Override
	public void specialItemRendering(ItemIdentifier item, int x, int y) {}
}
