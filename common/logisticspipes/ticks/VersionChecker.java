package logisticspipes.ticks;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import lombok.Data;

import logisticspipes.LogisticsPipes;

public final class VersionChecker implements Callable<VersionChecker.VersionInfo> {

	private static Future<VersionInfo> versionCheckFuture;
	private String statusString;
	private VersionInfo versionInfo = null;

	private VersionChecker() {
	}

	public static VersionChecker runVersionCheck() {
		VersionChecker obj = new VersionChecker();
		versionCheckFuture = LogisticsPipes.singleThreadExecutor.submit(obj);
		return obj;
	}

	public String getVersionCheckerStatus() {
		if (versionCheckFuture != null) {
			if (versionCheckFuture.isDone()) {
				// has to be done, before getting final string and setting to null
				statusString = internalGetVersionCheckerStatus();
				versionCheckFuture = null;
			} else {
				statusString = internalGetVersionCheckerStatus();
			}
		}
		return statusString;
	}

	public boolean isVersionCheckDone() {
		return versionInfo != null;
	}

	public VersionInfo getVersionInfo() {
		return versionInfo;
	}

	private String internalGetVersionCheckerStatus() {
		if (versionCheckFuture.isDone()) {
			try {
				versionInfo = versionCheckFuture.get();
				if (versionInfo == null) {
					if (LogisticsPipes.isDevelopmentEnvironment()) {
						return "You are running Logistics Pipes from a development environment.";
					} else {
						return "Version checking is not available for this Logistics Pipes build.";
					}
				} else {
					if (versionInfo.isNewVersionAvailable()) {
						return "New Logistics Pipes build found: #" + versionInfo.getNewestBuild();
					} else {
						return "You have the newest Logistics Pipes build :)";
					}
				}
			} catch (InterruptedException e) {
				return "The version check task was interrupted and there is no version information available.";
			} catch (ExecutionException e) {
				LogisticsPipes.log.warn("The version check task had an exception while getting the newest version information", e);
				return "The version check task had an exception. See the log file for more information.";
			}
		} else {
			return "The version check is not yet ready, sorry.";
		}
	}

	@Override
	public VersionInfo call() {
		// LP1's update server (rs485.network) is offline; there is nothing to ask.
		return null;
	}

	@Data
	public static class VersionInfo {

		private boolean newVersionAvailable;
		private boolean imcMessageSent;
		private String newestBuild;
		private List<String> changelog;
	}
}
