package logisticspipes.transport;

import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.Objects;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import logisticspipes.LogisticsPipes;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.utils.OrientationsUtil;
import logisticspipes.utils.item.ItemIdentifierStack;

public class TransportInvConnection extends PipeTransportLogistics {

	public TransportInvConnection() {
		super(true);
	}

	@Override
	protected boolean isItemUnwanted(ItemIdentifierStack stack) {
		return false;
	}

	@Override
	protected void inventorySystemConnectorHook(ItemRoutingInformation info, BlockEntity tile) {
		if (tile == null) {
			return;
		}

		final Direction orientationOfTilewithTile = OrientationsUtil.getOrientationOfTilewithTile(getPipe().container, tile);
		Objects.requireNonNull(orientationOfTilewithTile, "Could not get direction from pipe and tile entity");

		if (tile.getLevel() != null && tile.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, tile.getBlockPos(), orientationOfTilewithTile.getOpposite()) != null) {
			((PipeItemsInvSysConnector) container.pipe).handleItemEnterInv(info, tile);
		}
	}
}
