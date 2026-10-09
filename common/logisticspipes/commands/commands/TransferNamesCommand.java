package logisticspipes.commands.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.RequestUpdateNamesPacket;
import logisticspipes.proxy.MainProxy;

public class TransferNamesCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "transfernames", "tn" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return sender.getPlayer() != null && LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Sends all item names form the client", "to the server to update the Language Database" };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		sender.sendSystemMessage(Component.literal("Requesting Transfer"));
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RequestUpdateNamesPacket.class), sender.getPlayer());
		MainProxy.proxy.sendNameUpdateRequest(sender.getPlayer());
	}
}
