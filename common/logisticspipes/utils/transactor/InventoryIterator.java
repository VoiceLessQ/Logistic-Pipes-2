package logisticspipes.utils.transactor;

import net.minecraft.core.Direction;

import net.neoforged.neoforge.items.IItemHandler;

public final class InventoryIterator {

	/**
	 * Deactivate constructor
	 */
	private InventoryIterator() {}

	/**
	 * Returns an Iterable object for the specified side of the inventory.
	 *
	 * @param inv
	 * @param side
	 * @return Iterable
	 */
	public static Iterable<IInvSlot> getIterable(IItemHandler inv, Direction side) {

		return new InventoryIteratorSimple(inv);
	}

}
