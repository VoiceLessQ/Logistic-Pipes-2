package logisticspipes.network.packets.orderer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

import logisticspipes.LPItems;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class DiskRequestConectPacket extends CoordinatesPacket {

	public DiskRequestConectPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new DiskRequestConectPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof PipeItemsRequestLogisticsMk2) {
			if (((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk() != null) {
				if (((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().getItem().equals(LPItems.disk.get())) {
					if (!logisticspipes.utils.item.StackTag.hasTag(((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk())) {
						logisticspipes.utils.item.StackTag.setTag(((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk(), new CompoundTag());
					}
				}
			}
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(DiscContent.class).setStack(((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk()).setBlockPos(pipe.getBlockPos()), player);
		}
		if (pipe.pipe instanceof PipeBlockRequestTable) {
			if (((PipeBlockRequestTable) pipe.pipe).diskInv.getItem(0) != null) {
				if (((PipeBlockRequestTable) pipe.pipe).diskInv.getItem(0).getItem().equals(LPItems.disk.get())) {
					if (!logisticspipes.utils.item.StackTag.hasTag(((PipeBlockRequestTable) pipe.pipe).diskInv.getItem(0))) {
						logisticspipes.utils.item.StackTag.setTag(((PipeBlockRequestTable) pipe.pipe).diskInv.getItem(0), new CompoundTag());
					}
				}
			}
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(DiscContent.class).setStack(((PipeBlockRequestTable) pipe.pipe).diskInv.getItem(0)).setBlockPos(pipe.getBlockPos()), player);
		}
	}
}
