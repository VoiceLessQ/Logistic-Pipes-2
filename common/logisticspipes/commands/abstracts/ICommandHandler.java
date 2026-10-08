package logisticspipes.commands.abstracts;
import net.minecraft.commands.CommandSourceStack;


public interface ICommandHandler {

	String[] getNames();

	boolean isCommandUsableBy(CommandSourceStack sender);

	String[] getDescription();

	void executeCommand(CommandSourceStack sender, String[] args);
}
