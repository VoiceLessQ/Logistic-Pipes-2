package logisticspipes.network.packets.gui;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.IntegerPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

/** Client asks for the open LP menu's slot contents once its screen exists; the initial sync can arrive before the client menu does. */
@StaticResolve
public class GuiMenuResyncPacket extends IntegerPacket {

	public GuiMenuResyncPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		if (player.containerMenu.containerId == getInteger()) {
			player.containerMenu.sendAllDataToRemote();
		}
	}

	@Override
	public ModernPacket template() {
		return new GuiMenuResyncPacket(getId());
	}
}
