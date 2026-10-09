package logisticspipes.commands.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.ActivateNBTDebug;
import logisticspipes.proxy.MainProxy;

public class NBTDebugCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "nbtdebug" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return sender.getPlayer() != null && LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Enables the Hotkey to show an debug gui", "for the howered item. (Only if NEI is installed)" };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		sender.sendSystemMessage(Component.literal("Trying to Enable NBTDebug"));
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ActivateNBTDebug.class), sender.getPlayer());
	}
}
