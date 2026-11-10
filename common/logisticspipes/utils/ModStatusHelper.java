package logisticspipes.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;

public class ModStatusHelper {

	public static boolean isModLoaded(String modId) {
		if (modId.contains("@")) {
			String version = modId.substring(modId.indexOf('@') + 1);
			modId = modId.substring(0, modId.indexOf('@'));
			if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId)) {
				final String modIdFinal = modId;
				return FabricLoader.getInstance().getModContainer(modIdFinal)
						.map(mod -> mod.getMetadata().getVersion().getFriendlyString().startsWith(version))
						.orElse(false);
			}
			return false;
		} else {
			return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
		}
	}

	public static boolean areModsLoaded(String modIds) {
		if (modIds.contains("+")) {
			for (String modId : modIds.split("\\+")) {
				if (!isModLoaded(modId)) {
					return false;
				}
			}
			return true;
		} else {
			return isModLoaded(modIds);
		}
	}

	public static boolean isModVersionEqualsOrHigher(String modId, String version) {
		// Numeric-aware version compare (lexicographic broke on e.g. 1.10 vs 1.9)
		return FabricLoader.getInstance().getModContainer(modId)
				.map(mod -> {
					try {
						return mod.getMetadata().getVersion().compareTo(SemanticVersion.parse(version)) >= 0;
					} catch (VersionParsingException e) {
						return false;
					}
				})
				.orElse(false);
	}
}
