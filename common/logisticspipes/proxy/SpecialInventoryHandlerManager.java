package logisticspipes.proxy;


import network.rs485.logisticspipes.proxy.StorageDrawersProxy;

public class SpecialInventoryHandlerManager {

	public static void load() {

		// AE2 interface handler is parked until AE2 is wired for Fabric.

		// BuildCraft inventory handler removed — no 1.20.1 port exists.

		StorageDrawersProxy.INSTANCE.registerInventoryHandler();

		// Charset has no 1.20.1 port — removed
	}

}
