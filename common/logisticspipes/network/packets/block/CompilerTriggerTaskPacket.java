package logisticspipes.network.packets.block;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class CompilerTriggerTaskPacket extends CoordinatesPacket {

	public CompilerTriggerTaskPacket(int id) {
		super(id);
	}

	@Getter
	@Setter
	private Identifier category;

	@Getter
	@Setter
	private String type;

	@Override
	public void processPacket(Player player) {
		LogisticsProgramCompilerTileEntity tile = this.getTileAs(player.level(), LogisticsProgramCompilerTileEntity.class);
		tile.triggerNewTask(getCategory(), getType());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeResourceLocation(category);
		output.writeUTF(type);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		category = input.readResourceLocation();
		type = input.readUTF();
	}

	@Override
	public ModernPacket template() {
		return new CompilerTriggerTaskPacket(getId());
	}
}
