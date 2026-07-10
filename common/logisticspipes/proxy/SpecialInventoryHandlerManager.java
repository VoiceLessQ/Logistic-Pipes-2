package logisticspipes.proxy;

import net.neoforged.fml.ModList;

import static logisticspipes.LPConstants.appliedenergisticsModID;

import logisticspipes.proxy.specialinventoryhandler.AEInterfaceInventoryHandler;
import network.rs485.logisticspipes.proxy.StorageDrawersProxy;

public class SpecialInventoryHandlerManager {

	public static void load() {

		if (ModList.get().isLoaded(appliedenergisticsModID)) {
			SimpleServiceLocator.inventoryUtilFactory.registerHandler(new AEInterfaceInventoryHandler());
		}

		// BuildCraft inventory handler removed — no 1.20.1 port exists.

		StorageDrawersProxy.INSTANCE.registerInventoryHandler();

		// Charset has no 1.20.1 port — removed
	}

}
