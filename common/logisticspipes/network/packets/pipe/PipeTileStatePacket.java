package logisticspipes.network.packets.pipe;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.interfaces.IClientState;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataIOWrapper;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PipeTileStatePacket extends CoordinatesPacket {

	@Setter
	private IClientState renderState;

	@Setter
	private IClientState coreState;

	@Setter
	private IClientState pipe;

	@Getter
	private byte[] bytesRenderState;

	@Getter
	private byte[] bytesCoreState;

	@Getter
	private byte[] bytesPipe;

	@Getter
	@Setter
	private int statePacketId;

	public PipeTileStatePacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.level(), LTGPCompletionCheck.NONE);
		if (pipe == null) {
			return;
		}
		if (pipe.statePacketId <= statePacketId) {
			LPDataIOWrapper.provideData(bytesRenderState, pipe.renderState::readData);
			LPDataIOWrapper.provideData(bytesCoreState, pipe.coreState::readData);
			pipe.afterStateUpdated();
			if (pipe.pipe != null && bytesPipe.length != 0) {
				LPDataIOWrapper.provideData(bytesPipe, pipe.pipe::readData);
			}
			pipe.statePacketId = statePacketId;
		}
	}

	@Override
	public ModernPacket template() {
		return new PipeTileStatePacket(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);

		IClientState[] clientStates = new IClientState[] { renderState, coreState, pipe };
		byte[][] clientStateBuffers = new byte[][] { bytesRenderState, bytesCoreState, bytesPipe };
		for (int i = 0; i < clientStates.length; i++) {
			// pipe may be null for a BE whose pipe item failed to load (readData tolerates empty)
			IClientState state = clientStates[i];
			clientStateBuffers[i] = state == null ? new byte[0] : LPDataIOWrapper.collectData(state::writeData);
			output.writeByteArray(clientStateBuffers[i]);
		}

		output.writeInt(statePacketId);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);

		bytesRenderState = input.readByteArray();
		bytesCoreState = input.readByteArray();
		bytesPipe = input.readByteArray();

		statePacketId = input.readInt();
	}
}
