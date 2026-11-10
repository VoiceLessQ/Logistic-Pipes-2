package logisticspipes.ticks;

import java.util.ArrayList;
import java.util.List;

// Fabric has no built-in update check (Forge read updateJSONURL from mods.toml)
public final class VersionChecker {

	public String getVersionCheckerStatus() {
		return "Version checking is not available for this Logistics Pipes build.";
	}

	public boolean isNewVersionAvailable() {
		return false;
	}

	public String getNewestVersion() {
		return "";
	}

	public List<String> getChangelog() {
		return new ArrayList<>();
	}
}
