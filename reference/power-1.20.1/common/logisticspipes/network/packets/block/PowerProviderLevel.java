package logisticspipes.network.packets.block;

import net.minecraft.world.entity.player.Player;

import logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PowerProviderLevel extends CoordinatesPacket {

	private Double aDouble;

	public PowerProviderLevel(int id) {
		super(id);
	}

	public double getDouble() {
		return aDouble;
	}

	public PowerProviderLevel setDouble(double d) {
		aDouble = d;
		return this;
	}

	public PowerProviderLevel putDouble(double d) {
		return setDouble(d);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		setDouble(input.readDouble());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeDouble(getDouble());
	}

	@Override
	public ModernPacket template() {
		return new PowerProviderLevel(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsPowerProviderTileEntity tile = this.getTileAs(player.level(), LogisticsPowerProviderTileEntity.class);
		if (tile != null) {
			tile.handlePowerPacket(getDouble());
		}
	}
}
