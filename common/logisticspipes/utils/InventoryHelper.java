package logisticspipes.utils;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import net.neoforged.neoforge.capabilities.Capabilities;

import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.transactor.ITransactor;
import logisticspipes.utils.transactor.TransactorSimple;
import network.rs485.logisticspipes.inventory.ProviderMode;

public class InventoryHelper {

	//BC getTransactorFor using our getInventory
	public static ITransactor getTransactorFor(Object object, @Nullable Direction dir) {
		if (object instanceof BlockEntity tile) {
			ITransactor t = SimpleServiceLocator.inventoryUtilFactory.getSpecialHandlerFor(tile, dir, ProviderMode.DEFAULT);
			if (t != null) {
				return t;
			}
			if (tile.getLevel() != null) {
				var handler = tile.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, tile.getBlockPos(), dir);
				if (handler != null) {
					return new TransactorSimple(handler);
				}
			}
		}
		return null;
	}
}
