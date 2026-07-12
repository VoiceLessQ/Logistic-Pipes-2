package logisticspipes.network.packets.pipe;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;


import lombok.Getter;
import lombok.Setter;

import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class SlotFinderNumberPacket extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private int pipePosX;
	@Getter
	@Setter
	private int pipePosY;
	@Getter
	@Setter
	private int pipePosZ;
	@Setter
	private int inventorySlot;
	@Getter
	@Setter
	private int slot;

	public SlotFinderNumberPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SlotFinderNumberPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		BlockEntity inv = this.getTileAs(player.level(), BlockEntity.class);
		IInventoryUtil util = SimpleServiceLocator.inventoryUtilFactory.getInventoryUtil(inv, null);
		if (util == null) return;
		Slot result = null;
		if (player.containerMenu.slots.get(inventorySlot).index == inventorySlot) {
			result = player.containerMenu.slots.get(inventorySlot);
		}
		if (result == null) {
			for (Slot slotObject : player.containerMenu.slots) {
				if (slotObject.index == inventorySlot) {
					result = slotObject;
					break;
				}
			}
		}
		if (result == null) {
			player.sendSystemMessage(Component.translatable("lp.chat.slotnotfound"));
			return;
		}
		int resultIndex = -1;
		ItemStack content = result.getItem();
		if (!content.isEmpty()) {
			for (int i = 0; i < util.getContainerSize(); i++) {
				if (content == util.getItem(i)) {
					resultIndex = i;
					break;
				}
			}
		} else {
			ItemStack dummyStack = new ItemStack(Blocks.DIRT, 1);
			CompoundTag nbt = new CompoundTag();
			nbt.putBoolean("LPStackFinderBoolean", true); //Make it unique
			logisticspipes.utils.item.StackTag.setTag(dummyStack, nbt); // dummyStack: yay, I am unique
			result.set(dummyStack);
			for (int i = 0; i < util.getContainerSize(); i++) {
				if (dummyStack == util.getItem(i)) {
					resultIndex = i;
					break;
				}
			}
			if (resultIndex == -1) {
				for (int i = 0; i < util.getContainerSize(); i++) {
					ItemStack stack = util.getItem(i);
					if (stack.isEmpty()) {
						continue;
					}
					if (ItemIdentifier.get(stack).equals(ItemIdentifier.get(dummyStack)) && stack.getCount() == dummyStack.getCount()) {
						resultIndex = i;
						break;
					}
				}
			}
			result.set(ItemStack.EMPTY);
		}

		if (resultIndex == -1) {
			player.sendSystemMessage(Component.translatable("lp.chat.slotnotfound"));
		} else {
			//Copy pipe to coordinates to use the getPipe method
			setPosX(getPipePosX());
			setPosY(getPipePosY());
			setPosZ(getPipePosZ());
			ModuleActiveSupplier module = this.getLogisticsModule(player, ModuleActiveSupplier.class);
			if (module != null) {
				module.slotAssignmentPattern.set(slot, resultIndex);
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(inventorySlot);
		output.writeInt(slot);
		output.writeInt(pipePosX);
		output.writeInt(pipePosY);
		output.writeInt(pipePosZ);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		inventorySlot = input.readInt();
		slot = input.readInt();
		pipePosX = input.readInt();
		pipePosY = input.readInt();
		pipePosZ = input.readInt();
	}
}
