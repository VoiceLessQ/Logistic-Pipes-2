package logisticspipes.network.abstractpackets;

import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nonnull;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import lombok.Setter;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class ItemPacket extends CoordinatesPacket {

	@Getter
	@Setter
	@Nonnull
	private ItemStack stack = ItemStack.EMPTY;

	public ItemPacket(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		if (getStack().isEmpty()) {
			output.writeInt(0);
		} else {
			output.writeInt(BuiltInRegistries.ITEM.getId(getStack().getItem()));
			output.writeInt(getStack().getCount());
			output.writeInt(getStack().getDamageValue());
			output.writeCompoundTag(logisticspipes.utils.item.StackTag.getTag(getStack()));
		}
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);

		final int itemID = input.readInt();
		if (itemID == 0) {
			setStack(ItemStack.EMPTY);
		} else {
			int stackSize = input.readInt();
			int damage = input.readInt();
			ItemStack newStack = new ItemStack(BuiltInRegistries.ITEM.byId(itemID), stackSize);
			newStack.setDamageValue(damage);
			setStack(newStack);
			logisticspipes.utils.item.StackTag.setTag(getStack(), input.readCompoundTag());
		}
	}
}
