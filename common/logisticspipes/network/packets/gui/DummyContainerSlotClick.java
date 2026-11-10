package logisticspipes.network.packets.gui;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.ColorSlot;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.DummySlot;
import logisticspipes.utils.gui.FluidSlot;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class DummyContainerSlotClick extends ModernPacket {

	@Getter
	@Setter
	int slotId;

	@Getter
	@Setter
	@Nonnull
	ItemStack stack;

	@Getter
	@Setter
	int button;

	public DummyContainerSlotClick(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		slotId = input.readInt();
		stack = input.readItemIdentifierStack().makeNormalStack();
		button = input.readInt();
	}

	@Override
	public void processPacket(Player player) {
		if (player instanceof ServerPlayer && ((ServerPlayer) player).containerMenu instanceof DummyContainer) {
			DummyContainer container = (DummyContainer) ((ServerPlayer) player).containerMenu;
			Slot slot = container.slots.get(slotId);
			if (slot instanceof DummySlot || slot instanceof ColorSlot || slot instanceof FluidSlot) {
				container.handleDummyClick(slot, slotId, stack, button, ContainerInput.PICKUP, player);
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeInt(slotId);
		output.writeItemIdentifierStack(ItemIdentifierStack.getFromStack(stack));
		output.writeInt(button);
	}

	@Override
	public ModernPacket template() {
		return new DummyContainerSlotClick(getId());
	}
}
