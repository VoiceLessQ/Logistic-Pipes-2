package logisticspipes.commands.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;

public class BypassCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "bypass", "bp" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return sender.getPlayer() != null && LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Allows to enable/disable the", "security station bypass token" };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		if (!LogisticsSecurityTileEntity.byPassed.contains(sender.getPlayer())) {
			LogisticsSecurityTileEntity.byPassed.add(sender.getPlayer());
			sender.sendSystemMessage(Component.literal("Enabled"));
		} else {
			LogisticsSecurityTileEntity.byPassed.remove(sender.getPlayer());
			sender.sendSystemMessage(Component.literal("Disabled"));
		}
	}
}
