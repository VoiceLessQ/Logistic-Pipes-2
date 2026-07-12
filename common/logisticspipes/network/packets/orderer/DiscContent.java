package logisticspipes.network.packets.orderer;

import net.minecraft.world.entity.player.Player;

import logisticspipes.LPItems;
import logisticspipes.network.abstractpackets.ItemPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class DiscContent extends ItemPacket {

	public DiscContent(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new DiscContent(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe tile = this.getPipe(player.level());
		if (tile == null) {
			return;
		}
		if (tile.pipe instanceof PipeItemsRequestLogisticsMk2) {
			if (MainProxy.isServer(tile.getLevel())) {
				if (!((PipeItemsRequestLogisticsMk2) tile.pipe).getDisk().isEmpty() && ((PipeItemsRequestLogisticsMk2) tile.pipe).getDisk().getItem().equals(LPItems.disk.get())) {
					if (!getStack().isEmpty() && getStack().getItem().equals(LPItems.disk.get())) {
						logisticspipes.utils.item.StackTag.setTag(((PipeItemsRequestLogisticsMk2) tile.pipe).getDisk(), logisticspipes.utils.item.StackTag.getTag(getStack()));
					}
				}
			} else {
				((PipeItemsRequestLogisticsMk2) tile.pipe).setDisk(getStack());
			}
		}
		if (tile.pipe instanceof PipeBlockRequestTable) {
			if (MainProxy.isServer(tile.getLevel())) {
				if (!((PipeBlockRequestTable) tile.pipe).diskInv.getItem(0).isEmpty() && ((PipeBlockRequestTable) tile.pipe).diskInv.getItem(0).getItem().equals(LPItems.disk.get())) {
					if (!getStack().isEmpty() && getStack().getItem().equals(LPItems.disk.get())) {
						logisticspipes.utils.item.StackTag.setTag(((PipeBlockRequestTable) tile.pipe).diskInv.getItem(0), logisticspipes.utils.item.StackTag.getTag(getStack()));
					}
				}
			} else {
				((PipeBlockRequestTable) tile.pipe).diskInv.setItem(0, getStack());
			}
		}
	}
}
