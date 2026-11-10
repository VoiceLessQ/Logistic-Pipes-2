package logisticspipes.network.packets.block;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

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

	@Environment(EnvType.CLIENT)
	private void handleClientSide(Player player) {
		if (Minecraft.getInstance().gui.screen() instanceof GuiSecurityStation) {
			SecuritySettings setting = new SecuritySettings(null);
			setting.readFromNBT(getTag());
			((GuiSecurityStation) Minecraft.getInstance().gui.screen()).handlePlayerSecurityOpen(setting);
		}
	}
}
