package logisticspipes.proxy;


import net.fabricmc.loader.api.FabricLoader;

import logisticspipes.LPConstants;
import logisticspipes.proxy.cc.CCProxy;
import logisticspipes.proxy.ccl.CCLProxy;

public class ProxyManager {

	public static void load() {
		// CCL is LP's own built-in impl; no ASM wrapper, so callers get the real render state/model classes.
		SimpleServiceLocator.setCCLProxy(new CCLProxy());

		SimpleServiceLocator.setConfigToolHandler(new ConfigToolHandler());
		SimpleServiceLocator.configToolHandler.registerWrapper();

		SimpleServiceLocator.setPowerProxy(new PowerProxy());

		if (Boolean.getBoolean("lp.devtest")) {
			DevTestCommands.register();
			if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
				logisticspipes.proxy.side.DevTestClient.register();
			}
		}

		if (FabricLoader.getInstance().isModLoaded(LPConstants.computerCraftModID)) {
			SimpleServiceLocator.setCCProxy(new CCProxy());
		}
		// EnderStorage has no Fabric 26.x build; its proxy stays parked and SimpleServiceLocator keeps the dummy.
	}
}
