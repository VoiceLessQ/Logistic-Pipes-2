package logisticspipes.gui;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;



import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.gui.popup.GuiEditCCAccessTable;
import logisticspipes.gui.popup.GuiSecurityStationPopup;
import logisticspipes.interfaces.PlayerListReciver;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.PlayerListRequest;
import logisticspipes.network.packets.block.SecurityAuthorizationPacket;
import logisticspipes.network.packets.block.SecurityCardPacket;
import logisticspipes.network.packets.block.SecurityRequestCCIdsPacket;
import logisticspipes.network.packets.block.SecurityStationAutoDestroy;
import logisticspipes.network.packets.block.SecurityStationCC;
import logisticspipes.network.packets.block.SecurityStationOpenPlayerRequest;
import logisticspipes.network.packets.gui.OpenSecurityChannelManagerPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

public class GuiSecurityStation extends LogisticsBaseGuiScreen implements PlayerListReciver {

	private static final String PREFIX = "gui.securitystation.";

	private final LogisticsSecurityTileEntity _tile;
	private final List<String> players = new LinkedList<>();

	//Player name:
	protected static final int searchWidth = 250;
	protected int lastClickedX = 0;
	protected int lastClickedY = 0;
	protected int lastClickedK = 0;
	private int addition;
	private boolean authorized;
	private InputBar searchBar;
	private SmallGuiButton btnMinusMinus;
	private SmallGuiButton btnMinus;
	private SmallGuiButton btnPlus;
	private SmallGuiButton btnPlusPlus;
	private SmallGuiButton btnOpen;
	private GuiCheckBox checkAllowCC;
	private SmallGuiButton btnEditTable;
	private SmallGuiButton btnAuthorize;
	private SmallGuiButton btnDeauthorize;
	private GuiCheckBox checkAutoDestroy;
	private SmallGuiButton btnChannelManager;

	protected final String _title = "Request items";
	protected boolean clickWasButton = false;

	public GuiSecurityStation(LogisticsSecurityTileEntity tile, Player player) {
		super(buildDummy(tile, player), 280, 260, 0, 0);
		_tile = tile;
		authorized = SimpleServiceLocator.securityStationManager.isAuthorized(tile.getSecId());
	}
	private static DummyContainer buildDummy(LogisticsSecurityTileEntity tile, Player player) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), tile.inv);
		dummy.addRestrictedSlot(0, tile.inv, 82, 141, (Item) null);
		dummy.addNormalSlotsForPlayerInventory(10, 175);
		return dummy;
	}


	@Override
	public void init() {
		

		super.init();
		btnMinusMinus = new SmallGuiButton(0, leftPos + 10, topPos + 179, 30, 20, "--");
		btnMinusMinus.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityCardPacket.class).setInteger(0).setBlockPos(_tile.getBlockPos())));
		addRenderableWidget(btnMinusMinus);
		btnMinusMinus.visible = false;
		btnMinus = new SmallGuiButton(1, leftPos + 10, topPos + 139, 30, 20, "-");
		btnMinus.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityCardPacket.class).setInteger(1).setBlockPos(_tile.getBlockPos())));
		addRenderableWidget(btnMinus);
		btnPlus = new SmallGuiButton(2, leftPos + 45, topPos + 139, 30, 20, "+");
		btnPlus.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityCardPacket.class).setInteger(2).setBlockPos(_tile.getBlockPos())));
		addRenderableWidget(btnPlus);
		btnPlusPlus = new SmallGuiButton(3, leftPos + 140, topPos + 179, 30, 20, "++");
		btnPlusPlus.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityCardPacket.class).setInteger(3).setBlockPos(_tile.getBlockPos())));
		addRenderableWidget(btnPlusPlus);
		btnPlusPlus.visible = false;
		btnOpen = new SmallGuiButton(4, leftPos + 241, topPos + 217, 30, 10, TextUtil.translate(GuiSecurityStation.PREFIX + "Open"));
		btnOpen.setPressListener(b -> {
			if (!searchBar.getText().isEmpty()) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityStationOpenPlayerRequest.class).setString(searchBar.getText()).setBlockPos(_tile.getBlockPos()));
			}
		});
		addRenderableWidget(btnOpen);
		checkAllowCC = new GuiCheckBox(5, leftPos + 160, topPos + 42, 16, 16, _tile.allowCC);
		checkAllowCC.setPressListener(b -> {
			_tile.allowCC = !_tile.allowCC;
			refreshCheckBoxes();
			MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityStationCC.class).setInteger(_tile.allowCC ? 1 : 0).setBlockPos(_tile.getBlockPos()));
		});
		addRenderableWidget(checkAllowCC);
		btnEditTable = new SmallGuiButton(6, leftPos + 162, topPos + 60, 60, 10, TextUtil.translate(GuiSecurityStation.PREFIX + "EditTable"));
		btnEditTable.setPressListener(b -> {
			setSubGui(new GuiEditCCAccessTable(_tile));
			MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityRequestCCIdsPacket.class).setBlockPos(_tile.getBlockPos()));
		});
		addRenderableWidget(btnEditTable);
		// ComputerCraft not available on 1.20.1 (former dummy isCC() was always false) — CC widgets only in DEBUG.
		if (!LogisticsPipes.isDEBUG()) {
			checkAllowCC.visible = false;
			btnEditTable.visible = false;
		}
		btnAuthorize = new SmallGuiButton(7, leftPos + 55, topPos + 95, 70, 20, TextUtil.translate(GuiSecurityStation.PREFIX + "Authorize"));
		btnAuthorize.setPressListener(b -> {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityAuthorizationPacket.class).setInteger(1).setBlockPos(_tile.getBlockPos()));
			authorized = true;
		});
		addRenderableWidget(btnAuthorize);
		btnDeauthorize = new SmallGuiButton(8, leftPos + 175, topPos + 95, 70, 20, TextUtil.translate(GuiSecurityStation.PREFIX + "Deauthorize"));
		btnDeauthorize.setPressListener(b -> {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityAuthorizationPacket.class).setInteger(0).setBlockPos(_tile.getBlockPos()));
			authorized = false;
		});
		addRenderableWidget(btnDeauthorize);
		checkAutoDestroy = new GuiCheckBox(9, leftPos + 160, topPos + 74, 16, 16, _tile.allowAutoDestroy);
		checkAutoDestroy.setPressListener(b -> {
			_tile.allowAutoDestroy = !_tile.allowAutoDestroy;
			refreshCheckBoxes();
			MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityStationAutoDestroy.class).setInteger(_tile.allowAutoDestroy ? 1 : 0).setBlockPos(_tile.getBlockPos()));
		});
		addRenderableWidget(checkAutoDestroy);
		btnChannelManager = new SmallGuiButton(10, leftPos + 177, topPos + 230, 95, 20, TextUtil.translate(GuiSecurityStation.PREFIX + "ChannelManager"));
		btnChannelManager.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(OpenSecurityChannelManagerPacket.class).setBlockPos(_tile.getBlockPos())));
		addRenderableWidget(btnChannelManager);
		if (searchBar == null) {
			searchBar = new InputBar(this.font, this, leftPos + 180, bottom - 120, right - 8 + addition - leftPos - 180, 17);
			lastClickedX = -10000000;
			lastClickedY = -10000000;
		}
		searchBar.reposition(leftPos + 180, bottom - 120, right - 8 + addition - leftPos - 180, 17);
		MainProxy.sendPacketToServer(PacketHandler.getPacket(PlayerListRequest.class));
	}

	@Override
	public void closeGui() throws IOException {
		super.closeGui();
		
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float var1, int var2, int var3) {
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 10, topPos + 175);
		LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 81, topPos + 140);

		addition = (minecraft.font.width(searchBar.getText()) - 82);
		if (addition < 0) addition = 0;

		searchBar.drawTextBox();

		// Click detection for player list (drawing happens in renderLabels)
		int pos = bottom - 95;
		for (String player : players) {
			if (player.contains(searchBar.getText())) {
				pos += 11;
			}
			if (leftPos + 180 < lastClickedX && lastClickedX < leftPos + 280 && pos - 11 < lastClickedY && lastClickedY < pos) {
				lastClickedX = -10000000;
				lastClickedY = -10000000;
				searchBar.setText(player);
			}
			if (pos > bottom - 12) break;
		}

		if (authorized) {
			guiGraphics.fill(leftPos + 127, topPos + 101, leftPos + 147, topPos + 108, Color.getValue(Color.GREEN));
		} else {
			guiGraphics.fill(leftPos + 153, topPos + 101, leftPos + 173, topPos + 108, Color.getValue(Color.RED));
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		guiGraphics.drawString(font, TextUtil.translate(GuiSecurityStation.PREFIX + "SecurityStation"), 105, 10, 0x404040, false);
		guiGraphics.drawString(font, _tile.getSecId() == null ? "null" : _tile.getSecId().toString(), 32, 25, 0x404040, false);
		if (LogisticsPipes.isDEBUG()) {
			guiGraphics.drawString(font, TextUtil.translate(GuiSecurityStation.PREFIX + "allowCCAccess") + ":", 10, 46, 0x404040, false);
			guiGraphics.drawString(font, TextUtil.translate(GuiSecurityStation.PREFIX + "excludeIDs") + ":", 10, 61, 0x404040, false);
		}
		guiGraphics.drawString(font, TextUtil.translate(GuiSecurityStation.PREFIX + "pipeRemove") + ":", 10, 78, 0x404040, false);
		guiGraphics.drawString(font, TextUtil.translate(GuiSecurityStation.PREFIX + "Player") + ":", 180, 127, 0x404040, false);
		guiGraphics.drawString(font, TextUtil.translate(GuiSecurityStation.PREFIX + "SecurityCards") + ":", 10, 127, 0x404040, false);
		guiGraphics.drawString(font, TextUtil.translate(GuiSecurityStation.PREFIX + "Inventory") + ":", 10, 163, 0x404040, false);

		int pos = bottom - topPos - 95;
		for (String player : players) {
			if (player.contains(searchBar.getText())) {
				guiGraphics.drawString(font, player, 180, pos, 0x404040, false);
				pos += 11;
			}
			if (pos > bottom - topPos - 12) {
				guiGraphics.drawString(font, "...", 180, pos - 5, 0x404040, false);
				break;
			}
		}
	}

	@Override
	public boolean mouseClicked(double i, double j, int k) {
		if ((i >= leftPos + 5 && i < right - 5 + addition && j >= topPos + 5 && j < bottom - 5) && !searchBar.isFocused()) {
			lastClickedX = (int)i;
			lastClickedY = (int)j;
			lastClickedK = k;
		}
		if (searchBar.handleClick(i, j, k)) {
			return true;
		}
		return super.mouseClicked(i, j, k);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (searchBar != null && searchBar.isFocused()) {
			if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
				searchBar.setFocused(false);
				return true;
			}
			if (searchBar.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (searchBar.isFocused()) {
			if (searchBar.handleKey(c, i)) {
				return true;
			}
		}
		return super.charTyped(c, i);
	}

	@Override
	public void receivePlayerList(List<String> list) {
		players.clear();
		players.addAll(list);
	}

	public void handlePlayerSecurityOpen(SecuritySettings setting) {
		searchBar.setText("");
		setSubGui(new GuiSecurityStationPopup(setting, _tile));
	}

	public void refreshCheckBoxes() {
		checkAllowCC.setState(_tile.allowCC);
		checkAutoDestroy.setState(_tile.allowAutoDestroy);
	}
}
