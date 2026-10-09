package logisticspipes.commands.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.commands.exception.MissingArgumentException;
import logisticspipes.utils.item.ItemIdentifier;

public class NameLookupCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "name" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Displays the serverside stored name for", "the <item id> and <meta data>" };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		if (args.length < 2) {
			throw new MissingArgumentException();
		}
		String idString = args[0];
		String metaString = args[1];
		int id = Integer.valueOf(idString);
		int meta = Integer.valueOf(metaString);
		ItemIdentifier item = ItemIdentifier.get(BuiltInRegistries.ITEM.byId(id), meta, null);
		sender.sendSystemMessage(Component.literal("Name: " + item.getFriendlyNameCC()));
	}
}
