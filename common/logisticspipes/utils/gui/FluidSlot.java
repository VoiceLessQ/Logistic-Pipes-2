package logisticspipes.utils.gui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class FluidSlot extends Slot {

	public FluidSlot(Container par1iInventory, int par2, int par3, int par4) {
		super(par1iInventory, par2, par3, par4);
	}

	public FluidSlot(Slot slot) {
		super(slot.container, slot.getContainerSlot(), slot.x, slot.y);
	}

	@Override
	public boolean mayPickup(Player par1Player) {
		return false;
	}
}
