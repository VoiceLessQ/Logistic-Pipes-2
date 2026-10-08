package logisticspipes.commands.commands;
import net.minecraft.commands.CommandSourceStack;

import net.minecraft.network.chat.Component;

import logisticspipes.LogisticsPipes;
import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.ticks.VersionChecker;

public class VersionCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "version", "v" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Display the used LP version", "and shows, if an update is available" };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		sender.sendSystemMessage(Component.literal(LogisticsPipes.getVersionString()));

		VersionChecker versionChecker = LogisticsPipes.versionChecker;
		sender.sendSystemMessage(Component.literal(versionChecker.getVersionCheckerStatus()));

		if (versionChecker.isVersionCheckDone() && versionChecker.getVersionInfo().isNewVersionAvailable()) {
			sender.sendSystemMessage(Component.literal("Use \"/logisticspipes changelog\" to see a changelog."));
		}
	}
}
