package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;





import logisticspipes.interfaces.IGUIChannelInformationReceiver;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.InvSysConContentRequest;
import logisticspipes.network.packets.pipe.InvSysConOpenSelectChannelPopupPacket;
import logisticspipes.network.packets.pipe.InvSysConResistance;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

public class GuiInvSysConnector extends LogisticsBaseGuiScreen implements IGUIChannelInformationReceiver {

	private static final String PREFIX = "gui.invsyscon.";

	private int page = 0;
	private final List<ItemIdentifierStack> _allItems = new ArrayList<>();
	private final PipeItemsInvSysConnector pipe;
	private InputBar resistanceCountBar;

	private ChannelInformation connectedChannel = null;

	public GuiInvSysConnector(Player player, PipeItemsInvSysConnector pipe) {
		super(buildDummy(player, pipe), 180, 220, 0, 0);
		this.pipe = pipe;

	}
	private static DummyContainer buildDummy(Player player, PipeItemsInvSysConnector pipe) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), null);

		dummy.addNormalSlotsForPlayerInventory(10, 135);
		return dummy;
	}


	@Override
	public void init() {
		

		super.init();
		SmallGuiButton b0 = new SmallGuiButton(0, leftPos + 120, topPos + 67, 10, 10, "<");
		b0.setPressListener(b -> pageDown());
		addRenderableWidget(b0);
		SmallGuiButton b1 = new SmallGuiButton(1, leftPos + 160, topPos + 67, 10, 10, ">");
		b1.setPressListener(b -> pageUp());
		addRenderableWidget(b1);
		SmallGuiButton b2 = new SmallGuiButton(2, leftPos + 68, topPos + 67, 46, 10, TextUtil.translate(GuiInvSysConnector.PREFIX + "Refresh"));
		b2.setPressListener(b -> refreshPacket());
		addRenderableWidget(b2);
		SmallGuiButton b3 = new SmallGuiButton(3, leftPos + 80, topPos + 55, 10, 10, "<");
		b3.setPressListener(b -> resistanceCountBar.putInt(resistanceCountBar.getInt() - (Screen.hasControlDown() ? 10 : 1)));
		addRenderableWidget(b3);
		SmallGuiButton b4 = new SmallGuiButton(4, leftPos + 120, topPos + 55, 10, 10, ">");
		b4.setPressListener(b -> resistanceCountBar.putInt(resistanceCountBar.getInt() + 1));
		addRenderableWidget(b4);
		SmallGuiButton b5 = new SmallGuiButton(5, leftPos + 140, topPos + 55, 30, 10, TextUtil.translate(GuiInvSysConnector.PREFIX + "Save"));
		b5.setPressListener(b -> {
			pipe.resistance = resistanceCountBar.getInt();
			MainProxy.sendPacketToServer(PacketHandler.getPacket(InvSysConResistance.class).putInt(pipe.resistance).setPosX(pipe.getX()).setPosY(pipe.getY()).setPosZ(pipe.getZ()));
		});
		addRenderableWidget(b5);
		SmallGuiButton b6 = new SmallGuiButton(6, leftPos + 130, topPos + 20, 40, 10, TextUtil.translate(GuiInvSysConnector.PREFIX + "Change"));
		b6.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(InvSysConOpenSelectChannelPopupPacket.class).setTilePos(pipe.container)));
		addRenderableWidget(b6);

		if (this.resistanceCountBar == null) {
			this.resistanceCountBar = new InputBar(this.font, this, leftPos + 90, topPos + 55, 30, 12, false, true, InputBar.Align.CENTER);
			this.resistanceCountBar.minNumber = 0;
			this.resistanceCountBar.putInt(pipe.resistance);
		}
		this.resistanceCountBar.reposition(leftPos + 90, topPos + 55, 30, 12);

		refreshPacket();
	}

	@Override
	public void closeGui() throws IOException {
		super.closeGui();
		
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float var1, int var2, int var3) {
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 10, topPos + 135);
		guiGraphics.fill(leftPos + 9, topPos + 78, leftPos + 170, topPos + 132, Color.getValue(Color.GREY));
		resistanceCountBar.drawTextBox();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiInvSysConnector.PREFIX + "InventorySystemConnector"), 5, 6, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiInvSysConnector.PREFIX + "ConnectionInformation") + ":", 10, 21, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.getTrimmedString(TextUtil.translate(GuiInvSysConnector.PREFIX + "Channel") + ": " + (connectedChannel != null ? connectedChannel.getName() : "UNDEFINED"), 150, this.font, "..."), 15, 38, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiInvSysConnector.PREFIX + "Resistance") + ":", 10, 55, 0x404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiInvSysConnector.PREFIX + "Waitingfor") + ":", 10, 68, 0x404040, false);
		guiGraphics.drawString(minecraft.font, (page + 1) + "/" + maxPage(), 136, 69, 0x404040, false);
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(_allItems, null, page, 9, 79, 9, 27, 18, 18, 100.0F, DisplayAmount.ALWAYS);

		int ppi = 0;
		int column = 0;
		int row = 0;
		for (ItemIdentifierStack itemStack : _allItems) {
			ppi++;
			if (ppi <= 27 * page) continue;
			if (ppi > 27 * (page + 1)) continue;
			ItemStack st = itemStack.unsafeMakeNormalStack();
			int x = 9 + 18 * column + leftPos;
			int y = 79 + 18 * row + topPos;
			if (x < par1 && par1 < x + 18 && y < par2 && par2 < y + 18) {
				guiGraphics.renderTooltip(minecraft.font, st, par1, par2);
			}
			column++;
			if (column >= 9) {
				row++;
				column = 0;
			}
		}
	}

	private void refreshPacket() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(InvSysConContentRequest.class).setPosX(pipe.getX()).setPosY(pipe.getY()).setPosZ(pipe.getZ()));
	}

	private void pageDown() {
		if (page <= 0) {
			page = maxPage() - 1;
		} else {
			page--;
		}
	}

	private void pageUp() {
		if (page >= maxPage() - 1) {
			page = 0;
		} else {
			page++;
		}
	}

	private int maxPage() {
		int i = (int) (Math.floor(((float) _allItems.size()) / 27) + (((float) _allItems.size()) % 27 == 0 ? 0 : 1));
		if (i <= 0) {
			i = 1;
		}
		return i;
	}

	@Override
	public boolean mouseClicked(double x, double y, int k) {
		if (!resistanceCountBar.handleClick(x, y, k)) {
			return super.mouseClicked(x, y, k);
		}
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (resistanceCountBar.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (!resistanceCountBar.handleKey(c, i)) {
			return super.charTyped(c, i);
		}
		return true;
	}

	public void handleContentAnswer(Collection<ItemIdentifierStack> allItems) {
		_allItems.clear();
		_allItems.addAll(allItems);
	}

	public void handleResistanceAnswer(int resistance) {
		resistanceCountBar.putInt(resistance);
	}

	@Override
	public void handleChannelInformation(ChannelInformation channel, boolean flag) {
		if (this.getSubGui() instanceof IGUIChannelInformationReceiver) {
			((IGUIChannelInformationReceiver) this.getSubGui()).handleChannelInformation(channel, flag);
		}
		if (flag) {
			this.connectedChannel = channel;
		} else if (this.connectedChannel != null && this.connectedChannel.getChannelIdentifier().equals(channel.getChannelIdentifier())) {
			this.connectedChannel = channel;
		}
	}
}
