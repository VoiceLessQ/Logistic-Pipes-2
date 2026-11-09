package logisticspipes.network.packets.module;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.modules.ModuleItemSink;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.gui.module.ItemSinkGui;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ItemSinkImportPacket extends ModuleCoordinatesPacket {

	@Nullable
	@Setter
	@Getter
	public List<ItemIdentifier> importedItems = null;

	public ItemSinkImportPacket(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCollection(importedItems, LPDataOutput::writeItemIdentifier);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		importedItems = input.readArrayList(LPDataInput::readItemIdentifier);
	}

	@Override
	public ModernPacket template() {
		return new ItemSinkImportPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (MainProxy.isServer(player.level())) {
			ModuleItemSink module = this.getLogisticsModule(player, ModuleItemSink.class);
			if (module == null) {
				return;
			}
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ItemSinkImportPacket.class)
				.setImportedItems(module.getAdjacentInventoriesItems()
					.limit(module.filterInventory.getContainerSize())
					.collect(Collectors.toList()))
				.setPacketPos(this), player);
		} else if (MainProxy.isClient(player.level())) {
			if (importedItems == null) return;
			if (Minecraft.getInstance().gui.screen() instanceof ItemSinkGui) {
				((ItemSinkGui) Minecraft.getInstance().gui.screen()).importFromInventory(importedItems);
			}
		}
	}

}
