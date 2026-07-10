package logisticspipes.commands.chathelper;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import net.minecraft.server.level.ServerPlayer;
// Player removed — use net.minecraft.commands.CommandSourceStack

import net.minecraft.network.chat.Component;

import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import net.neoforged.bus.api.SubscribeEvent;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.OpenChatGui;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.string.ChatColor;
import logisticspipes.utils.string.StringUtils;

public class LPChatListener {

	private static final Map<String, Callable<Boolean>> tasks = new HashMap<>();
	private static final Map<String, MorePageDisplay> morePageDisplays = new HashMap<>();

	private List<String> sendChatMessages = null;

	@SubscribeEvent
	public void serverChat(ServerChatEvent event) {
		ServerPlayer player = event.getPlayer();
		String playerName = player.getName().getString();
		String chatMessage = event.getMessage().getString();
		if (LPChatListener.tasks.containsKey(playerName)) {
			if (chatMessage.startsWith("/")) {
				player.sendSystemMessage(Component.literal(ChatColor.RED + "You need to answer the question, before you can use any other command"));
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), player);
			} else {
				if (!chatMessage.equalsIgnoreCase("true") && !chatMessage.equalsIgnoreCase("false") && !chatMessage.equalsIgnoreCase("on") && !chatMessage.equalsIgnoreCase("off") && !chatMessage.equalsIgnoreCase("0") && !chatMessage.equalsIgnoreCase("1") && !chatMessage
						.equalsIgnoreCase("no")
						&& !chatMessage.equalsIgnoreCase("yes")) {
					player.sendSystemMessage(Component.literal(ChatColor.RED + "Not a valid answer."));
					player.sendSystemMessage(Component.literal(ChatColor.AQUA + "Please enter " + ChatColor.RESET + "<" + ChatColor.GREEN + "yes" + ChatColor.RESET + "/" + ChatColor.RED + "no " + ChatColor.RESET + "| " + ChatColor.GREEN + "true" + ChatColor.RESET + "/" + ChatColor.RED + "flase "
							+ ChatColor.RESET + "| " + ChatColor.GREEN + "on" + ChatColor.RESET + "/" + ChatColor.RED + "off " + ChatColor.RESET + "| " + ChatColor.GREEN + "1" + ChatColor.RESET + "/" + ChatColor.RED + "0" + ChatColor.RESET + ">"));
					MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), player);
				} else {
					boolean flag = chatMessage.equalsIgnoreCase("true") || chatMessage.equalsIgnoreCase("on") || chatMessage.equalsIgnoreCase("1") || chatMessage.equalsIgnoreCase("yes");
					if (!handleAnswer(flag, player)) {
						player.sendSystemMessage(Component.literal(ChatColor.RED + "Error: Could not handle answer."));
					}
				}
			}
			event.setCanceled(true);
		} else if (LPChatListener.morePageDisplays.containsKey(playerName)) {
			if (!LPChatListener.morePageDisplays.get(playerName).isTerminated()) {
				if (chatMessage.startsWith("/")) {
					player.sendSystemMessage(Component.literal(ChatColor.RED + "Exit " + ChatColor.AQUA + "PageView" + ChatColor.RED + " first!"));
					MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), player);
					event.setCanceled(true);
				} else {
					if (LPChatListener.morePageDisplays.get(playerName).handleChat(chatMessage, player)) {
						event.setCanceled(true);
					}
				}
			}
		}
	}

	@SubscribeEvent
	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public void clientChat(ClientChatReceivedEvent event) {
		Component message = event.getMessage();
		if (message != null) {
			String realMessage = null;
			try {
				realMessage = message.getString();
			} catch (ClassCastException e) {
				//Ignore that
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (realMessage != null) {
				if (realMessage.equals("%LPCLEARCHAT%")) {
					clearChat();
					event.setCanceled(true);
				}
				if (realMessage.equals("%LPSTORESENDMESSAGE%")) {
					storeSendMessages();
					event.setCanceled(true);
				}
				if (realMessage.equals("%LPRESTORESENDMESSAGE%")) {
					restoreSendMessages();
					event.setCanceled(true);
				}
				if (realMessage.startsWith("%LPADDTOSENDMESSAGE%")) {
					addSendMessages(realMessage.substring(20));
					event.setCanceled(true);
				}
				if (realMessage.contains("LPDISPLAYMISSING") && LogisticsPipes.isDEBUG()) {
					System.out.println("LIST:");
					StringUtils.UNTRANSLATED_STRINGS.forEach(System.out::println);
				}
			}
		}
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	private void clearChat() {
		net.minecraft.client.Minecraft.getInstance().gui.getChat().clearMessages(true);
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	private void storeSendMessages() {
		sendChatMessages = new ArrayList<>(net.minecraft.client.Minecraft.getInstance().gui.getChat().getRecentChat());
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	private void restoreSendMessages() {
		if (sendChatMessages != null) {
			net.minecraft.client.gui.components.ChatComponent chat = net.minecraft.client.Minecraft.getInstance().gui.getChat();
			for (String msg : sendChatMessages) {
				chat.addRecentChat(msg);
			}
		}
		sendChatMessages = null;
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	private void addSendMessages(String substring) {
		net.minecraft.client.Minecraft.getInstance().gui.getChat().addRecentChat(substring);
	}

	public static void register(MorePageDisplay displayInput, String name) {
		if (LPChatListener.morePageDisplays.containsKey(name) && !LPChatListener.morePageDisplays.get(name).isTerminated()) {
			return;
		}
		LPChatListener.morePageDisplays.put(name, displayInput);
	}

	public static void remove(String name) {
		LPChatListener.morePageDisplays.remove(name);
	}

	public boolean handleAnswer(boolean flag, Player sender) {
		String senderName = sender.getName().getString();
		if (!LPChatListener.tasks.containsKey(senderName)) {
			return false;
		}
		if (flag) {
			try {
				Boolean result;
				if ((result = LPChatListener.tasks.get(senderName).call()) != null) {
					if (result != null && !result) {
						return false;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		} else {
			sender.sendSystemMessage(Component.literal(ChatColor.GREEN + "Answer handled."));
		}
		LPChatListener.tasks.remove(senderName);
		return true;
	}

	public static boolean existTaskFor(String name) {
		return LPChatListener.tasks.containsKey(name);
	}

	public static void removeTask(String name) {
		LPChatListener.tasks.remove(name);
	}

	public static boolean addTask(Callable<Boolean> input, Player sender) {
		String senderName = sender.getName().getString();
		if (LPChatListener.tasks.containsKey(senderName)) {
			return false;
		} else {
			LPChatListener.tasks.put(senderName, input);
			return true;
		}
	}
}
