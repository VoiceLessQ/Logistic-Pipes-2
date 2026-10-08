package logisticspipes.commands.commands.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import logisticspipes.commands.abstracts.ICommandHandler;

public class HandCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "hand" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return sender.getPlayer() != null;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Start debugging the selected ItemStack" };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		Player player = sender.getPlayer();
		ItemStack item = player.getInventory().items.get(player.getInventory().selected);
		if (!item.isEmpty()) {
			DebugGuiController.instance().startWatchingOf(item, player);
			sender.sendSystemMessage(Component.literal("Starting HandDebuging"));
		}
	}
}
