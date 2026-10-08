
package logisticspipes.gui.orderer;

import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nonnull;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;



import logisticspipes.gui.popup.GuiDiskPopup;
import logisticspipes.interfaces.IDiskProvider;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.DiskDropPacket;
import logisticspipes.network.packets.orderer.DiskRequestConectPacket;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;

public class NormalMk2GuiOrderer extends NormalGuiOrderer implements IDiskProvider {

	public PipeItemsRequestLogisticsMk2 pipe;
	private SmallGuiButton macroButton;

	public NormalMk2GuiOrderer(PipeItemsRequestLogisticsMk2 RequestPipeMK2, Player entityPlayer) {
		super(RequestPipeMK2.getX(), RequestPipeMK2.getY(), RequestPipeMK2.getZ(), RequestPipeMK2.getWorld().dimension().location(), entityPlayer);
		pipe = RequestPipeMK2;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(DiskRequestConectPacket.class).setPosX(pipe.getX()).setPosY(pipe.getY()).setPosZ(pipe.getZ()));
	}

	@Override
	public void init() {
		super.init();
		macroButton = new SmallGuiButton(12, right - 55, bottom - 60, 50, 10, "Disk");
		macroButton.setPressListener(b -> {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(DiskRequestConectPacket.class).setPosX(pipe.getX()).setPosY(pipe.getY()).setPosZ(pipe.getZ()));
			setSubGui(new GuiDiskPopup(this));
		});
		addRenderableWidget(macroButton);
		macroButton.active = false;
	}

	@Override
	public void renderBg(@Nonnull GuiGraphics guiGraphics, float f, int i, int j) {
		super.renderBg(guiGraphics, f, i, j);

		guiGraphics.fill(right - 39, bottom - 47, right - 19, bottom - 27, Color.getValue(Color.BLACK));
		guiGraphics.fill(right - 37, bottom - 45, right - 21, bottom - 29, Color.getValue(Color.DARKER_GREY));

		if (pipe.getDisk() != null) {
			guiGraphics.renderItem(pipe.getDisk(), right - 36, bottom - 44);
			macroButton.active = true;
		} else {
			macroButton.active = false;
		}
	}

	@Override
	public boolean mouseClicked(double x, double y, int k) {
		if (x >= right - 39 && x < right - 19 && y >= bottom - 47 && y < bottom - 27) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(DiskDropPacket.class).setPosX(pipe.getX()).setPosY(pipe.getY()).setPosZ(pipe.getZ()));
			return true;
		} else {
			return super.mouseClicked(x, y, k);
		}
	}

	@Override
	@Nonnull
	public ItemStack getDisk() {
		return pipe.getDisk();
	}

	@Override
	public void specialItemRendering(ItemIdentifier item, int x, int y) {
		// ThaumCraft integration not ported to 1.20.1
	}

	@Override
	public int getX() {
		return pipe.getX();
	}

	@Override
	public int getY() {
		return pipe.getY();
	}

	@Override
	public int getZ() {
		return pipe.getZ();
	}

	@Override
	public ItemDisplay getItemDisplay() {
		return itemDisplay;
	}
}
