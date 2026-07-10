package logisticspipes.network.packets.orderer;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;



import lombok.Getter;
import lombok.Setter;

import logisticspipes.config.Configs;
import logisticspipes.gui.orderer.GuiOrderer;
import logisticspipes.gui.orderer.GuiRequestTable;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.IResource.ColorCode;
import logisticspipes.request.resources.ResourceNetwork;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.string.ChatColor;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class MissingItems extends ModernPacket {

	@Getter
	@Setter
	private Collection<IResource> items = new ArrayList<>();

	@Setter
	@Getter
	private boolean flag;

	public MissingItems(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new MissingItems(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
			handleClient(player);
		}
	}

	// See OpenChatGui: the client refs (Minecraft/LP GUI screens) live in this @OnlyIn helper so they
	// are stripped before verification on the dedicated server, letting the packet class link and be
	// sent server-side. processPacket stays free of client classes.
	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	private void handleClient(Player player) {
		if (Configs.DISPLAY_POPUP && Minecraft.getInstance().screen instanceof GuiOrderer) {
			((GuiOrderer) Minecraft.getInstance().screen)
					.handleRequestAnswer(getItems(), isFlag(), (GuiOrderer) Minecraft.getInstance().screen, player);
		} else if (Configs.DISPLAY_POPUP && Minecraft.getInstance().screen instanceof GuiRequestTable) {
			((GuiRequestTable) Minecraft.getInstance().screen)
					.handleRequestAnswer(getItems(), isFlag(), (GuiRequestTable) Minecraft.getInstance().screen, player);
		} else if (isFlag()) {
			for (IResource item : items) {
				player.sendSystemMessage(Component.literal(ChatColor.RED + "Missing: " + item.getDisplayText(ColorCode.MISSING)));
			}
		} else {
			for (IResource item : items) {
				player.sendSystemMessage(Component.literal(ChatColor.GREEN + "Requested: " + item.getDisplayText(ColorCode.SUCCESS)));
			}
			player.sendSystemMessage(Component.literal(ChatColor.GREEN + "Request successful!"));
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeCollection(items);
		output.writeBoolean(isFlag());
	}

	@Override
	public void readData(LPDataInput input) {
		items = input.readArrayList(ResourceNetwork::readResource);
		setFlag(input.readBoolean());
	}
}
