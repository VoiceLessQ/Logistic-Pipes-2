package logisticspipes.network.packets.upgrade;

import java.util.Objects;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.SlotPacket;
import logisticspipes.pipes.upgrades.ConnectionUpgradeConfig;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.UpgradeSlot;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ToogleDisconnectionUpgradeSidePacket extends SlotPacket {

	@Getter
	@Setter
	private Direction side;

	public ToogleDisconnectionUpgradeSidePacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		UpgradeSlot slot = getSlot(player, UpgradeSlot.class);
		ItemStack stack = slot.getItem();
		if (stack.isEmpty()) return;

		if (!logisticspipes.utils.item.StackTag.hasTag(stack)) {
			logisticspipes.utils.item.StackTag.setTag(stack, new CompoundTag());
		}

		CompoundTag tag = Objects.requireNonNull(logisticspipes.utils.item.StackTag.getTag(stack));
		String sideName = ConnectionUpgradeConfig.Sides.getNameForDirection(side);
		tag.putBoolean(sideName, !tag.getBoolean(sideName));

		logisticspipes.utils.item.StackTag.setTag(stack, tag);

		slot.set(stack);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeFacing(side);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		side = input.readFacing();
	}

	@Override
	public ModernPacket template() {
		return new ToogleDisconnectionUpgradeSidePacket(getId());
	}
}
