package logisticspipes.commands.commands;
import net.minecraft.commands.CommandSourceStack;

import net.minecraft.network.chat.Component;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.ticks.RoutingTableUpdateThread;

public class RoutingThreadCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "routingthread", "rt" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Display Routing thread status information" };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		sender.sendSystemMessage(Component.literal("RoutingTableUpdateThread: Queued: " + RoutingTableUpdateThread.size()));
		sender.sendSystemMessage(Component.literal("RoutingTableUpdateThread: Average: " + RoutingTableUpdateThread.getAverage() + "ns"));
	}
}
