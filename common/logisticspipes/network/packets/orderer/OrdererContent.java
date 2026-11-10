package logisticspipes.network.packets.orderer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;



import logisticspipes.gui.orderer.GuiOrderer;
import logisticspipes.gui.orderer.GuiRequestTable;
import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class OrdererContent extends InventoryModuleCoordinatesPacket {

	public OrdererContent(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new OrdererContent(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (Minecraft.getInstance().gui.screen() instanceof GuiOrderer) {
			((GuiOrderer) Minecraft.getInstance().gui.screen()).handlePacket(getIdentList());
		} else if (Minecraft.getInstance().gui.screen() instanceof GuiRequestTable) {
			((GuiRequestTable) Minecraft.getInstance().gui.screen()).handlePacket(getIdentList());
		}
	}
}
