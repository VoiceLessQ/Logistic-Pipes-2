package logisticspipes.network.packets.orderer;

import net.minecraft.world.entity.player.Player;

import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.RequestPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SubmitFluidRequestPacket extends RequestPacket {

	public SubmitFluidRequestPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SubmitFluidRequestPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = getPipeOrNull(player.level());
		if (pipe == null || !(pipe.pipe instanceof CoreRoutedPipe) || !(pipe.pipe instanceof IRequestFluid)) {
			return;
		}
		RequestHandler.requestFluid(player, getStack(), (CoreRoutedPipe) pipe.pipe, (IRequestFluid) pipe.pipe);
	}
}
