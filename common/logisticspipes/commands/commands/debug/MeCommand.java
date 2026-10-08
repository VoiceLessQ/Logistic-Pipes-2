package logisticspipes.commands.commands.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import logisticspipes.commands.abstracts.ICommandHandler;

public class MeCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "me", "self" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return sender.getPlayer() != null;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Start debugging the CommandSender" };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		DebugGuiController.instance().startWatchingOf(sender, sender.getPlayer());
		sender.sendSystemMessage(Component.literal("Starting SelfDebugging"));
	}
}
