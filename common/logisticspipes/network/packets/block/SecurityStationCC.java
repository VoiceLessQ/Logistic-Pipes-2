package logisticspipes.network.packets.block;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.gui.GuiSecurityStation;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SecurityStationCC extends IntegerCoordinatesPacket {

	public SecurityStationCC(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SecurityStationCC(getId());
	}

	@Override
	public void processPacket(Player player) {
		LogisticsSecurityTileEntity tile = this.getTileAs(player.level(), LogisticsSecurityTileEntity.class);
		if (tile instanceof LogisticsSecurityTileEntity) {
			if (MainProxy.isClient(player.level())) {
				tile.setClientCC(getInteger() == 1);
				handleClientSide(player);
			} else {
				tile.changeCC();
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void handleClientSide(Player player) {
		if (Minecraft.getInstance().screen instanceof GuiSecurityStation) {
			((GuiSecurityStation) Minecraft.getInstance().screen).refreshCheckBoxes();
		}
	}
}
