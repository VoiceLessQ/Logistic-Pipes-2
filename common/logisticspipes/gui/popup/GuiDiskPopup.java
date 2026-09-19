package logisticspipes.gui.popup;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;



import logisticspipes.interfaces.IDiskProvider;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.DiscContent;
import logisticspipes.network.packets.orderer.DiskMacroRequestPacket;
import logisticspipes.network.packets.orderer.DiskSetNamePacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.gui.TextListDisplay;

public class GuiDiskPopup extends SubGuiScreen {

	private boolean editName = false;
	private boolean displayCursor = false;
	private long oldSystemTime = 0;
	private String name1;
	private String name2;
	private final IDiskProvider diskProvider;
	private final TextListDisplay textList;

	private static final int SEARCH_WIDTH = 120;

	public GuiDiskPopup(IDiskProvider diskProvider) {
		super(150, 200, 0, 0);
		this.diskProvider = diskProvider;
		name2 = "";
		if (logisticspipes.utils.item.StackTag.hasTag(diskProvider.getDisk())) {
			name1 = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk()).getString("name");
		} else {
			name1 = "Disk";
		}
		textList = new TextListDisplay(this, 6, 46, 6, 30, 12, new TextListDisplay.List() {

			@Override
			public int getSize() {
				CompoundTag nbt = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk());
				if (nbt == null) {
					logisticspipes.utils.item.StackTag.setTag(diskProvider.getDisk(), new CompoundTag());
					nbt = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk());
				}

				if (!nbt.contains("macroList")) {
					ListTag list = new ListTag();
					nbt.put("macroList", list);
				}
				ListTag list = nbt.getList("macroList", 10);
				return list.size();
			}

			@Override
			public String getTextAt(int index) {
				CompoundTag nbt = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk());
				if (nbt == null) {
					logisticspipes.utils.item.StackTag.setTag(diskProvider.getDisk(), new CompoundTag());
					nbt = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk());
				}

				if (!nbt.contains("macroList")) {
					ListTag list = new ListTag();
					nbt.put("macroList", list);
				}
				ListTag list = nbt.getList("macroList", 10);
				return list.getCompound(index).getString("name");
			}

			@Override
			public int getTextColor(int index) {
				return 0xFFFFFF;
			}
		});
	}

	@Override
	public boolean mouseClicked(double i, double j, int k) {
		int x = (int) i - guiLeft;
		int y = (int) j - guiTop;
		textList.mouseClicked(i, j, k);
		if (k == 0) {
			if (10 < x && x < 138 && 29 < y && y < 44) {
				editName = true;
			} else if (editName) {
				writeDiskName();
			} else {
				return super.mouseClicked(i, j, k);
			}
		} else {
			return super.mouseClicked(i, j, k);
		}
		return true;
	}

	private void writeDiskName() {
		editName = false;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(DiskSetNamePacket.class).setString(name1 + name2).setPosX(diskProvider.getX()).setPosY(diskProvider.getY()).setPosZ(diskProvider.getZ()));
		CompoundTag nbt = new CompoundTag();
		if (logisticspipes.utils.item.StackTag.hasTag(diskProvider.getDisk())) {
			nbt = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk());
		}
		nbt.putString("name", name1 + name2);
		logisticspipes.utils.item.StackTag.setTag(diskProvider.getDisk(), nbt);
		MainProxy.sendPacketToServer(PacketHandler.getPacket(DiscContent.class).setStack(diskProvider.getDisk()).setPosX(diskProvider.getX()).setPosY(diskProvider.getY()).setPosZ(diskProvider.getZ()));
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton req = new SmallGuiButton(0, xCenter + 16, bottom - 27, 50, 10, "Request");
		req.setPressListener(b -> handleRequest());
		addRenderableWidget(req);
		SmallGuiButton exit = new SmallGuiButton(1, xCenter + 16, bottom - 15, 50, 10, "Exit");
		exit.setPressListener(b -> exitGui());
		addRenderableWidget(exit);
		SmallGuiButton addEdit = new SmallGuiButton(2, xCenter - 66, bottom - 27, 50, 10, "Add/Edit");
		addEdit.setPressListener(b -> handleAddEdit());
		addRenderableWidget(addEdit);
		SmallGuiButton del = new SmallGuiButton(3, xCenter - 66, bottom - 15, 50, 10, "Delete");
		del.setPressListener(b -> handleDelete());
		addRenderableWidget(del);
		SmallGuiButton up = new SmallGuiButton(4, xCenter - 12, bottom - 27, 25, 10, "/\\");
		up.setPressListener(b -> textList.scrollDown());
		addRenderableWidget(up);
		SmallGuiButton dn = new SmallGuiButton(5, xCenter - 12, bottom - 15, 25, 10, "\\/");
		dn.setPressListener(b -> textList.scrollUp());
		addRenderableWidget(dn);
	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(minecraft, guiLeft, guiTop, right, bottom, 0.0f, true);
		getGuiGraphics().drawString(minecraft.font, "Disk", xCenter - (minecraft.font.width("Disk") / 2), guiTop + 10, 0xFFFFFF, true);

		//NameInput
		if (editName) {
			getGuiGraphics().fill(guiLeft + 10, guiTop + 28, right - 10, guiTop + 45, Color.getValue(Color.BLACK));
			getGuiGraphics().fill(guiLeft + 11, guiTop + 29, right - 11, guiTop + 44, Color.getValue(Color.WHITE));
		} else {
			getGuiGraphics().fill(guiLeft + 11, guiTop + 29, right - 11, guiTop + 44, Color.getValue(Color.BLACK));
		}
		getGuiGraphics().fill(guiLeft + 12, guiTop + 30, right - 12, guiTop + 43, Color.getValue(Color.DARKER_GREY));

		getGuiGraphics().drawString(minecraft.font, name1 + name2, guiLeft + 15, guiTop + 33, 0xFFFFFF, false);

		//getGuiGraphics().fill(guiLeft + 6, guiTop + 46, right - 6, bottom - 30, Color.getValue(Color.GREY));

		textList.renderGuiBackground(mouseX, mouseY);

		if (editName) {
			int lineX = guiLeft + 15 + minecraft.font.width(name1);
			if (System.currentTimeMillis() - oldSystemTime > 500) {
				displayCursor = !displayCursor;
				oldSystemTime = System.currentTimeMillis();
			}
			if (displayCursor) {
				getGuiGraphics().fill(lineX, guiTop + 31, lineX + 1, guiTop + 42, Color.getValue(Color.WHITE));
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY < 0) {
			textList.scrollUp();
		} else if (scrollY > 0) {
			textList.scrollDown();
		} else {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		return true;
	}

	private void handleRequest() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(DiskMacroRequestPacket.class).putInt(textList.getSelected()).setPosX(diskProvider.getX()).setPosY(diskProvider.getY()).setPosZ(diskProvider.getZ()));
	}

	private void handleDelete() {
		CompoundTag nbt = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk());
		if (nbt == null) {
			logisticspipes.utils.item.StackTag.setTag(diskProvider.getDisk(), new CompoundTag());
			nbt = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk());
		}

		if (!nbt.contains("macroList")) {
			ListTag list = new ListTag();
			nbt.put("macroList", list);
		}

		ListTag list = nbt.getList("macroList", 10);
		ListTag newList = new ListTag();

		for (int i = 0; i < list.size(); i++) {
			if (i != textList.getSelected()) {
				newList.add(list.getCompound(i));
			}
		}
		textList.setSelected(-1);
		nbt.put("macroList", newList);
		MainProxy.sendPacketToServer(PacketHandler.getPacket(DiscContent.class).setStack(diskProvider.getDisk()).setPosX(diskProvider.getX()).setPosY(diskProvider.getY()).setPosZ(diskProvider.getZ()));
	}

	private void handleAddEdit() {
		String macroName = "";
		CompoundTag nbt = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk());
		if (nbt != null) {
			if (nbt.contains("macroList")) {
				ListTag list = nbt.getList("macroList", 10);
				if (textList.getSelected() != -1 && textList.getSelected() < list.size()) {
					CompoundTag entry = list.getCompound(textList.getSelected());
					macroName = entry.getString("name");
				}
			}
		}
		setSubGui(new GuiAddMacro(diskProvider, macroName));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!editName) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		if (Screen.isPaste(keyCode)) {
			name1 = name1 + net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
			if (name1.length() > 0) {
				name1 = name1.substring(0, name1.length() - 1);
			}
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
			if (name1.length() > 0) {
				name2 = name1.substring(name1.length() - 1) + name2;
				name1 = name1.substring(0, name1.length() - 1);
			}
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
			if (name2.length() > 0) {
				name1 += name2.substring(0, 1);
				name2 = name2.substring(1);
			}
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
			writeDiskName();
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_HOME) {
			name2 = name1 + name2;
			name1 = "";
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_END) {
			name1 = name1 + name2;
			name2 = "";
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
			if (name2.length() > 0) {
				name2 = name2.substring(1);
			}
		}
		return true;
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (editName) {
			if (Character.isLetterOrDigit(c) || c == ' ') {
				if (minecraft.font.width(name1 + c + name2) <= SEARCH_WIDTH) {
					name1 += c;
				}
				return true;
			}
		} else {
			return super.charTyped(c, i);
		}
		return false;
	}
}
