package logisticspipes.utils.transactor;

import net.minecraft.core.Direction;

import logisticspipes.utils.StorageItemHandler;

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
	public static Iterable<IInvSlot> getIterable(StorageItemHandler inv, Direction side) {

		return new InventoryIteratorSimple(inv);
	}

}
