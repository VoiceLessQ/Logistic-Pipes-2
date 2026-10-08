package logisticspipes.commands.commands.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.debug.PipeDebugLogAskForTarget;
import logisticspipes.network.packets.pipe.PipeDebugAskForTarget;
import logisticspipes.proxy.MainProxy;

public class PipeCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "pipe" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return sender.getPlayer() != null;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Set the pipe into debug mode" };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		if (args.length != 1) {
			sender.sendSystemMessage(Component.literal("Wrong amount of arguments"));
			return;
		}
		if (args[0].equalsIgnoreCase("help")) {
			sender.sendSystemMessage(Component.literal("client, server, both or console"));
		} else if (args[0].equalsIgnoreCase("both")) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(true), sender.getPlayer());
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(false), sender.getPlayer());
			sender.sendSystemMessage(Component.literal("Asking for Target."));
		} else if (args[0].equalsIgnoreCase("console") || args[0].equalsIgnoreCase("c")) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugLogAskForTarget.class), sender.getPlayer());
			sender.sendSystemMessage(Component.literal("Asking for Target."));
		} else {
			boolean isClient = args[0].equalsIgnoreCase("client");
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(!isClient), sender.getPlayer());
			sender.sendSystemMessage(Component.literal("Asking for Target."));
		}
	}
}
