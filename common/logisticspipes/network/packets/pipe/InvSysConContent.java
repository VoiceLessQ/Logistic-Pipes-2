package logisticspipes.network.packets.pipe;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;



import logisticspipes.gui.GuiInvSysConnector;
import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class InvSysConContent extends InventoryModuleCoordinatesPacket {

	public InvSysConContent(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new InvSysConContent(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (Minecraft.getInstance().gui.screen() instanceof GuiInvSysConnector) {
			((GuiInvSysConnector) Minecraft.getInstance().gui.screen()).handleContentAnswer(getIdentList());
		}
	}
}
