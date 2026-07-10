package logisticspipes.network.packets.block;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.gui.GuiSecurityStation;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.NBTCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SecurityStationOpenPlayer extends NBTCoordinatesPacket {

	public SecurityStationOpenPlayer(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SecurityStationOpenPlayer(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (MainProxy.isClient(player.level())) {
			handleClientSide(player);
		} else {

		}
	}

	@OnlyIn(Dist.CLIENT)
	private void handleClientSide(Player player) {
		if (Minecraft.getInstance().screen instanceof GuiSecurityStation) {
			SecuritySettings setting = new SecuritySettings(null);
			setting.readFromNBT(getTag());
			((GuiSecurityStation) Minecraft.getInstance().screen).handlePlayerSecurityOpen(setting);
		}
	}
}
