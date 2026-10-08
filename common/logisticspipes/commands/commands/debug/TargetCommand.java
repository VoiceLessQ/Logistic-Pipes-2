package logisticspipes.commands.commands.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.debuggui.DebugAskForTarget;
import logisticspipes.proxy.MainProxy;

public class TargetCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "target", "look", "watch" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return sender.getPlayer() != null;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Starts debugging the BlockEntity", "or Entity you are currently looking at." };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(DebugAskForTarget.class), sender.getPlayer());
		sender.sendSystemMessage(Component.literal("Asking for Target."));
	}
}
