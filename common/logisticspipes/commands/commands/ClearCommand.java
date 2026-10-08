package logisticspipes.commands.commands;
import net.minecraft.commands.CommandSourceStack;

import net.minecraft.network.chat.Component;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.utils.string.ChatColor;

public class ClearCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "clear" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Clears the chat window from every content", ChatColor.GRAY + "add '" + ChatColor.YELLOW + "all" + ChatColor.GRAY + "' to also clear the send messages" };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		if (args.length <= 0 || !args[0].equalsIgnoreCase("all")) {
			sender.sendSystemMessage(Component.literal("%LPSTORESENDMESSAGE%"));
			sender.sendSystemMessage(Component.literal("%LPCLEARCHAT%"));
			sender.sendSystemMessage(Component.literal("%LPRESTORESENDMESSAGE%"));
		} else {
			sender.sendSystemMessage(Component.literal("%LPCLEARCHAT%"));
		}
	}
}
