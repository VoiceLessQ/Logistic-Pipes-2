package logisticspipes.commands;

import java.util.Arrays;
import java.util.Locale;

import net.minecraft.world.entity.player.Player;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import logisticspipes.LogisticsPipes;
import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.commands.exception.CommandNotFoundException;
import logisticspipes.commands.exception.LPCommandException;
import logisticspipes.commands.exception.PermissionDeniedException;
import logisticspipes.proxy.MainProxy;

// Registered via RegisterCommandsEvent in LogisticsPipes.registerCommands().
// Subcommands are parsed from a greedy-string argument and dispatched through the
// legacy ICommandHandler chain — a future cleanup could promote each subcommand to
// its own Brigadier node for richer tab completion, but behaviour is feature-complete.
public class LogisticsPipesCommand {

	private final ICommandHandler mainCommand;

	public LogisticsPipesCommand() {
		mainCommand = new MainCommandHandler();
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("logisticspipes")
				.requires(src -> src.hasPermission(0))
				.then(Commands.literal("help")
					.executes(ctx -> {
						executeForSource(ctx.getSource(), new String[]{"help"});
						return 1;
					}))
				.then(Commands.argument("args", StringArgumentType.greedyString())
					.executes(ctx -> {
						String raw = StringArgumentType.getString(ctx, "args");
						executeForSource(ctx.getSource(), raw.isEmpty() ? new String[]{} : raw.split("\\s+"));
						return 1;
					}))
				.executes(ctx -> {
					executeForSource(ctx.getSource(), new String[]{});
					return 1;
				})
		);
		// Short alias: /lp ... → /logisticspipes ...
		dispatcher.register(
			Commands.literal("lp")
				.requires(src -> src.hasPermission(0))
				.redirect(dispatcher.getRoot().getChild("logisticspipes"))
		);
	}

	private void executeForSource(CommandSourceStack source, String[] arguments) {
		Player sender = source.getPlayer();
		if (sender == null) return;
		if (arguments.length <= 0) {
			sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("Type '/logisticspipes help' for help."));
			return;
		}
		try {
			mainCommand.executeCommand(sender, arguments);
		} catch (LPCommandException e) {
			if (e instanceof PermissionDeniedException) {
				sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("You are not allowed to execute that command now."));
			} else if (e instanceof CommandNotFoundException) {
				sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("The command was not found"));
			} else {
				sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("Usage: /logisticspipes help"));
			}
		}
	}

	public static boolean isOP(Player sender) {
		return Arrays.asList(ServerLifecycleHooks.getCurrentServer().getPlayerList().getOps().getUserList())
				.contains(sender.getName().getString().toLowerCase(Locale.US)) || (MainProxy.proxy.checkSinglePlayerOwner(sender.getName().getString()));
	}
}
