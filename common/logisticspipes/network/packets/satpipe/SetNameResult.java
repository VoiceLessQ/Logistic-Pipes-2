package logisticspipes.network.packets.satpipe;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;



import lombok.Getter;
import lombok.Setter;

import logisticspipes.gui.GuiSatellitePipe;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.SatelliteNamingResult;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class SetNameResult extends ModernPacket {

	@Setter
	@Getter
	private SatelliteNamingResult result;
	@Setter
	@Getter
	private String newName;

	public SetNameResult(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		result = input.readEnum(SatelliteNamingResult.class);
		newName = input.readUTF();
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeEnum(result);
		output.writeUTF(newName);
	}

	@Override
	public void processPacket(Player player) {
		if (Minecraft.getInstance().gui.screen() instanceof GuiSatellitePipe) {
			((GuiSatellitePipe) Minecraft.getInstance().gui.screen()).handleResponse(result, newName);
		}
	}

	@Override
	public ModernPacket template() {
		return new SetNameResult(getId());
	}
}
