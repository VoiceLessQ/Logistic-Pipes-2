package logisticspipes.network.packets.block;

import net.minecraft.world.entity.player.Player;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class PowerJunctionCheatPacket extends CoordinatesPacket {

	public PowerJunctionCheatPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new PowerJunctionCheatPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (!LogisticsPipes.isDEBUG()) {
			return;
		}
		final LogisticsPowerJunctionTileEntity tile = this.getTileAs(player.level(), LogisticsPowerJunctionTileEntity.class);
		if (tile != null) {
			tile.addEnergy(100000);
		}
	}
}
