package logisticspipes.network.packets.orderer;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RequestFluidOrdererRefreshPacket extends CoordinatesPacket {

	public RequestFluidOrdererRefreshPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new RequestFluidOrdererRefreshPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = getPipeOrNull(player.level());
		if (pipe == null || !(pipe.pipe instanceof CoreRoutedPipe)) {
			return;
		}
		RequestHandler.refreshFluid(player, (CoreRoutedPipe) pipe.pipe);
	}
}
