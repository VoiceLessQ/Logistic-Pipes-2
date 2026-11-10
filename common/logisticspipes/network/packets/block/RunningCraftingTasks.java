package logisticspipes.network.packets.block;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.gui.GuiStatistics;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class RunningCraftingTasks extends ModernPacket {

	@Getter
	@Setter
	private List<ItemIdentifierStack> identList;

	public RunningCraftingTasks(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		if (Minecraft.getInstance().gui.screen() instanceof GuiStatistics) {
			((GuiStatistics) Minecraft.getInstance().gui.screen()).handlePacket2(getIdentList());
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeCollection(identList, LPDataOutput::writeItemIdentifierStack);
	}

	@Override
	public void readData(LPDataInput input) {
		identList = input.readArrayList(LPDataInput::readItemIdentifierStack);
	}

	@Override
	public ModernPacket template() {
		return new RunningCraftingTasks(getId());
	}
}
