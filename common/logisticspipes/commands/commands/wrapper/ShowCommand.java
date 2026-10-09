package logisticspipes.commands.commands.wrapper;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.network.chat.Component;

import logisticspipes.asm.wrapper.AbstractWrapper;
import logisticspipes.asm.wrapper.LogisticsWrapperHandler;
import logisticspipes.asm.wrapper.WrapperState;
import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;

public class ShowCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "show" };
	}

	@Override
	public boolean isCommandUsableBy(CommandSourceStack sender) {
		return LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Re-enables the given proxy", "if it was disabled by an exception." };
	}

	@Override
	public void executeCommand(CommandSourceStack sender, String[] args) {
		if (args.length != 1) {
			sender.sendSystemMessage(Component.literal("Wrong amount of arguments"));
			return;
		}
		String name = args[0];
		List<AbstractWrapper> list = new ArrayList<>(LogisticsWrapperHandler.wrapperController);
		Iterator<AbstractWrapper> iter = list.iterator();
		while (iter.hasNext()) {
			AbstractWrapper item = iter.next();
			if (item.getState() != WrapperState.Exception) {
				iter.remove();
			}
		}
		iter = list.iterator();
		while (iter.hasNext()) {
			AbstractWrapper item = iter.next();
			if (!(item.getName() + item.getTypeName()).startsWith(name)) {
				iter.remove();
			}
		}
		if (list.size() > 1) {
			sender.sendSystemMessage(Component.literal("Possible: "));
			for (AbstractWrapper can : list) {
				sender.sendSystemMessage(Component.literal(can.getName() + can.getTypeName()));
			}
		} else if (list.isEmpty()) {
			sender.sendSystemMessage(Component.literal("No match found"));
		} else {
			AbstractWrapper wrapper = list.get(0);
			Throwable t = wrapper.getReason();
			if (t == null) {
				sender.sendSystemMessage(Component.literal("null"));
				return;
			} else {
				sender.sendSystemMessage(Component.literal("-----------------------------------------------------"));
				sender.sendSystemMessage(Component.literal(t.toString()));
				for (StackTraceElement s : t.getStackTrace()) {
					sender.sendSystemMessage(Component.literal(" " + s.toString()));
				}
				t.printStackTrace();
			}
		}
	}
}
