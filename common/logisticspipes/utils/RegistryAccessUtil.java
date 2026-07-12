package logisticspipes.utils;

import net.minecraft.core.HolderLookup;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Side-safe access to the active registry lookup, needed by the 1.20.5+
 * ItemStack save/parse signatures. Server path first; the client fallback is
 * isolated in a client-only holder class (OpenChatGui pattern).
 */
public final class RegistryAccessUtil {

	private RegistryAccessUtil() {}

	public static HolderLookup.Provider registries() {
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			return server.registryAccess();
		}
		if (FMLEnvironment.dist == Dist.CLIENT) {
			HolderLookup.Provider client = Client.registries();
			if (client != null) {
				return client;
			}
		}
		throw new IllegalStateException("No registry access available (no server, no client level)");
	}

	@net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
	private static final class Client {

		static HolderLookup.Provider registries() {
			var level = net.minecraft.client.Minecraft.getInstance().level;
			return level == null ? null : level.registryAccess();
		}
	}
}
