package logisticspipes.gui.popup;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Collections;

import net.minecraft.client.gui.screens.Screen;



import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.SecurityAddCCIdPacket;
import logisticspipes.network.packets.block.SecurityRemoveCCIdPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiEditCCAccessTable extends SubGuiScreen {

	private static final String PREFIX = "gui.securitystation.popup.ccAccess.";

	private final LogisticsSecurityTileEntity _tile;

	private String searchInput1 = "0";
	private String searchInput2 = "";
	private boolean editSearch = false;
	private boolean editSearchB = false;
	private boolean displayCursor = true;
	private long oldSystemTime = 0;
	private static int searchWidth = 55;
	private int lastClickedX = 0;
	private int lastClickedY = 0;
	private int lastClickedK = 0;
	private boolean clickWasButton = false;
	private int page = 0;

	public GuiEditCCAccessTable(LogisticsSecurityTileEntity tile) {
		super(150, 150, 0, 0);
		_tile = tile;
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton minus = new SmallGuiButton(0, guiLeft + 10, guiTop + 119, 30, 20, "-");
		minus.setPressListener(b -> handleBtn(0));
		addRenderableWidget(minus);
		SmallGuiButton plus = new SmallGuiButton(1, guiLeft + 110, guiTop + 119, 30, 20, "+");
		plus.setPressListener(b -> handleBtn(1));
		addRenderableWidget(plus);
		SmallGuiButton rm = new SmallGuiButton(2, guiLeft + 30, guiTop + 107, 40, 10, TextUtil.translate(GuiEditCCAccessTable.PREFIX + "Remove"));
		rm.setPressListener(b -> handleBtn(2));
		addRenderableWidget(rm);
		SmallGuiButton add = new SmallGuiButton(3, guiLeft + 80, guiTop + 107, 40, 10, TextUtil.translate(GuiEditCCAccessTable.PREFIX + "Add"));
		add.setPressListener(b -> handleBtn(3));
		addRenderableWidget(add);
		SmallGuiButton prev = new SmallGuiButton(4, guiLeft + 87, guiTop + 4, 10, 10, "<");
		prev.setPressListener(b -> handleBtn(4));
		addRenderableWidget(prev);
		SmallGuiButton next = new SmallGuiButton(5, guiLeft + 130, guiTop + 4, 10, 10, ">");
		next.setPressListener(b -> handleBtn(5));
		addRenderableWidget(next);
	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(minecraft, guiLeft, guiTop, right, bottom, 0.0f, true);
		getGuiGraphics().drawString(minecraft.font, "(" + (page + 1) + "/" + ((int) ((_tile.excludedCC.size() / 9D) + 1 - (_tile.excludedCC.size() % 9 == 0 && _tile.excludedCC.size() != 0 ? 1 : 0))) + ")", guiLeft + 100, guiTop + 5, 0x4F4F4F, false);

		boolean dark = true;
		for (int i = 0; i < 9; i++) {
			getGuiGraphics().fill(guiLeft + 10, guiTop + 15 + (i * 10), right - 10, guiTop + 25 + (i * 10), dark ? Color.getValue(Color.DARKER_GREY) : Color.getValue(Color.LIGHTER_GREY));
			dark = !dark;
		}
		dark = true;
		for (int i = 0; i < 9 && i + (page * 9) < _tile.excludedCC.size(); i++) {
			Integer id = _tile.excludedCC.get(i + (page * 9));
			getGuiGraphics().drawString(minecraft.font, Integer.toString(id), guiLeft + 75 - (minecraft.font.width(Integer.toString(id)) / 2), guiTop + 16 + (i * 10), dark ? 0xFFFFFF : 0x000000, false);
			dark = !dark;
			if (lastClickedX >= guiLeft + 10 && lastClickedX < right - 10 && lastClickedY >= guiTop + 15 + (i * 10) && lastClickedY < guiTop + 25 + (i * 10)) {
				lastClickedX = -10000000;
				lastClickedY = -10000000;
				searchInput1 = Integer.toString(id);
				searchInput2 = "";
			}
		}

		//SearchInput
		if (editSearch) {
			getGuiGraphics().fill(guiLeft + 40, bottom - 30, right - 40, bottom - 13, Color.getValue(Color.BLACK));
			getGuiGraphics().fill(guiLeft + 41, bottom - 29, right - 41, bottom - 14, Color.getValue(Color.WHITE));
		} else {
			getGuiGraphics().fill(guiLeft + 41, bottom - 29, right - 41, bottom - 14, Color.getValue(Color.BLACK));
		}
		getGuiGraphics().fill(guiLeft + 42, bottom - 28, right - 42, bottom - 15, Color.getValue(Color.DARKER_GREY));

		getGuiGraphics().drawString(minecraft.font, searchInput1 + searchInput2, guiLeft + 75 - (minecraft.font.width(searchInput1 + searchInput2) / 2), bottom - 25, 0xFFFFFF, false);
		if (editSearch) {
			int lineX = guiLeft + 75 + minecraft.font.width(searchInput1) - (minecraft.font.width(searchInput1 + searchInput2) / 2);
			if (System.currentTimeMillis() - oldSystemTime > 500) {
				displayCursor = !displayCursor;
				oldSystemTime = System.currentTimeMillis();
			}
			if (displayCursor) {
				getGuiGraphics().fill(lineX, bottom - 27, lineX + 1, bottom - 16, Color.getValue(Color.WHITE));
			}
		}

		//Click into search
		if (lastClickedX != -10000000 && lastClickedY != -10000000) {
			if (lastClickedX >= guiLeft + 42 && lastClickedX < right - 42 && lastClickedY >= bottom - 30 && lastClickedY < bottom - 13) {
				editSearch = true;
				if (searchInput1.equals("0") && searchInput2.length() == 0) {
					searchInput1 = "";
				}
				lastClickedX = -10000000;
				lastClickedY = -10000000;
				if (lastClickedK == 1) {
					searchInput1 = "0";
					searchInput2 = "";
				}
			} else {
				editSearch = false;
				if (searchInput1.length() == 0 && searchInput2.length() == 0) {
					searchInput1 = "0";
				}
			}
		}
	}

	@Override
	public boolean mouseClicked(double i, double j, int k) {
		clickWasButton = false;
		editSearchB = true;
		boolean result = super.mouseClicked(i, j, k);
		if ((!clickWasButton && i >= guiLeft + 10 && i < right - 10 && j >= guiTop + 18 && j < bottom - 10) || editSearch) {
			if (!editSearchB) {
				editSearch = false;
			}
			lastClickedX = (int) i;
			lastClickedY = (int) j;
			lastClickedK = k;
		}
		return result;
	}

	private void handleBtn(int id) {
		if (editSearch) {
			editSearchB = false;
		}
		clickWasButton = true;
		switch (id) {
			case 0:
				if ((searchInput1 + searchInput2).equals("")) {
					searchInput1 = "0";
					break;
				}
				try {
					int number = Integer.valueOf(searchInput1 + searchInput2);
					number--;
					if (number < 0) {
						number = 0;
					}
					searchInput1 = Integer.toString(number);
					searchInput2 = "";
				} catch (Exception e) {
					e.printStackTrace();
					searchInput1 = "0";
					searchInput2 = "";
				}
				break;
			case 1:
				if ((searchInput1 + searchInput2).equals("")) {
					searchInput1 = "1";
					break;
				}
				try {
					int number = Integer.valueOf(searchInput1 + searchInput2);
					number++;
					if (minecraft.font.width(Integer.toString(number)) <= GuiEditCCAccessTable.searchWidth) {
						searchInput1 = Integer.toString(number);
						searchInput2 = "";
					}
				} catch (Exception e) {
					e.printStackTrace();
					searchInput1 = "0";
					searchInput2 = "";
				}
				break;
			case 2: {
				Integer id1 = Integer.valueOf(searchInput1 + searchInput2);
				_tile.excludedCC.remove(id1);
				MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityRemoveCCIdPacket.class).putInt(id1).setBlockPos(_tile.getBlockPos()));
			}
			break;
			case 3: {
				Integer id2 = Integer.valueOf(searchInput1 + searchInput2);
				if (!_tile.excludedCC.contains(id2)) {
					_tile.excludedCC.add(id2);
					Collections.sort(_tile.excludedCC);
				}
				MainProxy.sendPacketToServer(PacketHandler.getPacket(SecurityAddCCIdPacket.class).putInt(id2).setBlockPos(_tile.getBlockPos()));
			}
			break;
			case 4:
				page--;
				if (page < 0) {
					page = 0;
				}
				break;
			case 5:
				page++;
				if (page > (_tile.excludedCC.size() / 9) - (_tile.excludedCC.size() % 9 == 0 && _tile.excludedCC.size() != 0 ? 1 : 0)) {
					page = (_tile.excludedCC.size() / 9) - (_tile.excludedCC.size() % 9 == 0 && _tile.excludedCC.size() != 0 ? 1 : 0);
				}
				break;
			default:
				break;
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!editSearch) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		if (Screen.isPaste(keyCode)) {
			try {
				String clip = net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
				Integer.valueOf(clip);
				searchInput1 = searchInput1 + clip;
			} catch (Exception e) {
				setSubGui(new GuiMessagePopup("Clipboard doesn't", "contain a number."));
			}
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
			if (searchInput1.length() > 0) {
				searchInput1 = searchInput1.substring(0, searchInput1.length() - 1);
			}
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
			if (searchInput1.length() > 0) {
				searchInput2 = searchInput1.substring(searchInput1.length() - 1) + searchInput2;
				searchInput1 = searchInput1.substring(0, searchInput1.length() - 1);
			}
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
			if (searchInput2.length() > 0) {
				searchInput1 += searchInput2.substring(0, 1);
				searchInput2 = searchInput2.substring(1);
			}
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
			editSearch = false;
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_HOME) {
			searchInput2 = searchInput1 + searchInput2;
			searchInput1 = "";
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_END) {
			searchInput1 = searchInput1 + searchInput2;
			searchInput2 = "";
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
			if (searchInput2.length() > 0) {
				searchInput2 = searchInput2.substring(1);
			}
		}
		return true;
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (editSearch) {
			if (Character.isDigit(c)) {
				if (minecraft.font.width(searchInput1 + c + searchInput2) <= GuiEditCCAccessTable.searchWidth) {
					searchInput1 += c;
				}
				return true;
			}
		} else {
			return super.charTyped(c, i);
		}
		return false;
	}

	public void fillColor(int x1, int y1, int x2, int y2, Color color) {
		net.minecraft.client.gui.GuiGraphics gg = getGuiGraphics();
		if (gg != null) gg.fill(x1, y1, x2, y2, Color.getValue(color));
	}
}
