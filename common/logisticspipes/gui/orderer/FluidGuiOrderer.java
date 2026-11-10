package logisticspipes.gui.orderer;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.RequestFluidOrdererRefreshPacket;
import logisticspipes.network.packets.orderer.SubmitFluidRequestPacket;
import logisticspipes.pipes.PipeFluidRequestLogistics;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class FluidGuiOrderer extends GuiOrderer {

	public FluidGuiOrderer(PipeFluidRequestLogistics pipe, Player entityPlayer) {
		super(pipe.getX(), pipe.getY(), pipe.getZ(), pipe.getWorld().dimension().identifier(), entityPlayer);
		_title = "Request Fluid";
		refreshItems();
	}

	@Override
	public void init() {
		boolean setItemDisplay = itemDisplay == null;
		super.init();
		logisticspipes.utils.gui.SmallGuiButton refreshBtn = new logisticspipes.utils.gui.SmallGuiButton(3, leftPos + 10, bottom - 25, 46, 20, "Refresh");
		refreshBtn.setPressListener(b -> refreshItems());
		addRenderableWidget(refreshBtn);
		if (setItemDisplay) {
			itemDisplay = new ItemDisplay(this, font, this, this, leftPos + 10, topPos + 18, imageWidth - 20, imageHeight - 100, xCenter, bottom - 24, 49, new int[] { 1, 1000, 16000, 100 }, false);
		}
		itemDisplay.reposition(leftPos + 10, topPos + 18, imageWidth - 20, imageHeight - 100, xCenter, bottom - 24);
	}

	@Override
	protected void submitRequest(ItemIdentifierStack stack) {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(SubmitFluidRequestPacket.class).setStack(stack).setPosX(xCoord).setPosY(yCoord).setPosZ(zCoord).setDimension(dimension));
	}

	@Override
	protected int getStackAmount() {
		return 1000;
	}

	@Override
	public void refreshItems() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestFluidOrdererRefreshPacket.class).setPosX(xCoord).setPosY(yCoord).setPosZ(zCoord).setDimension(dimension));
	}

	@Override
	public void specialItemRendering(ItemIdentifier item, int x, int y) {}
}
