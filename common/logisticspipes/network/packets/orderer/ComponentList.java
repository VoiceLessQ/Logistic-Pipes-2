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
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ComponentList extends ModernPacket {

	@Getter
	@Setter
	private Collection<IResource> used = new ArrayList<>();

	@Getter
	@Setter
	private Collection<IResource> missing = new ArrayList<>();

	public ComponentList(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ComponentList(getId());
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
					.handleSimulateAnswer(used, missing, (GuiOrderer) Minecraft.getInstance().screen, player);
		} else if (Configs.DISPLAY_POPUP && Minecraft.getInstance().screen instanceof GuiRequestTable) {
			((GuiRequestTable) Minecraft.getInstance().screen)
					.handleSimulateAnswer(used, missing, (GuiRequestTable) Minecraft.getInstance().screen, player);
		} else {
			for (IResource item : used) {
				player.sendSystemMessage(Component.literal("Component: " + item.getDisplayText(ColorCode.SUCCESS)));
			}
			for (IResource item : missing) {
				player.sendSystemMessage(Component.literal("Missing: " + item.getDisplayText(ColorCode.MISSING)));
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeCollection(used);
		output.writeCollection(missing);
	}

	@Override
	public void readData(LPDataInput input) {
		used = input.readArrayList(ResourceNetwork::readResource);
		missing = input.readArrayList(ResourceNetwork::readResource);
	}
}
