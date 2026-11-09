package logisticspipes.network.packets.pipe;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import logisticspipes.gui.orderer.GuiOrderer;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class RequestPipeDimension extends ModernPacket {

	public RequestPipeDimension(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new RequestPipeDimension(getId());
	}

	@Override
	public void processPacket(Player player) {
		if (Minecraft.getInstance().gui.screen() instanceof GuiOrderer) {
			((GuiOrderer) Minecraft.getInstance().gui.screen()).dimension = getDimension();
			((GuiOrderer) Minecraft.getInstance().gui.screen()).refreshItems();
		} else {
			GuiOrderer.dimensioncache = getDimension();
			GuiOrderer.cachetime = System.currentTimeMillis();
		}
	}
}
