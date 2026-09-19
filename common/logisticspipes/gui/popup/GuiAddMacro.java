package logisticspipes.gui.popup;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;



import logisticspipes.interfaces.IDiskProvider;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.DiscContent;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.IItemSearch;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class GuiAddMacro extends SubGuiScreen implements IItemSearch {

	private final IDiskProvider diskProvider;
	private int mousePosX = 0;
	private int mousePosY = 0;
	private int mouseButton = 0;
	private int pageAll = 0;
	private int maxPageAll = 0;
	private int pageMacro = 0;
	private int maxPageMacro = 0;
	private int wheelUp = 0;
	private int wheelDown = 0;
	private boolean editSearch = false;
	private boolean editName = false;
	private LinkedList<ItemIdentifierStack> macroItems = new LinkedList<>();
	private String name1;
	private String name2 = "";
	private String Search1 = "";
	private String Search2 = "";
	private boolean displayCursor = false;
	private long oldSystemTime = 0;

	private Object[] tooltip;

	private static final int NAME_WIDTH = 122;
	private static final int SEARCH_WIDTH = 138;

	public GuiAddMacro(IDiskProvider diskProvider, String macroName) {
		super(200, 200, 0, 0);
		this.diskProvider = diskProvider;
		name1 = macroName;
		loadMacroItems();
	}

	private void loadMacroItems() {
		if ((name1 + name2).equals("")) {
			return;
		}
		ListTag inventar = null;

		ListTag list = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk()).getList("macroList", 10);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompound(i);
			String name = tag.getString("name");
			if (name.equals(name1 + name2)) {
				inventar = tag.getList("inventar", 10);
				break;
			}
		}
		if (inventar == null) {
			return;
		}
		for (int i = 0; i < inventar.size(); i++) {
			CompoundTag itemNBT = inventar.getCompound(i);
			int itemID = itemNBT.getInt("id");
			int itemData = itemNBT.getInt("data");
			CompoundTag tag = null;
			if (itemNBT.contains("nbt")) {
				tag = itemNBT.getCompound("nbt");
			}
			ItemIdentifier item = ItemIdentifier.get(BuiltInRegistries.ITEM.byId(itemID), itemData, tag);
			int amount = itemNBT.getInt("amount");
			ItemIdentifierStack stack = new ItemIdentifierStack(item, amount);
			macroItems.add(stack);
		}
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton nAll = new SmallGuiButton(0, right - 15, guiTop +5, 10, 10, ">");
		nAll.setPressListener(b -> nextPageAll());
		addRenderableWidget(nAll);
		SmallGuiButton pAll = new SmallGuiButton(1, right - 90, guiTop +5, 10, 10, "<");
		pAll.setPressListener(b -> prevPageAll());
		addRenderableWidget(pAll);
		SmallGuiButton nMac = new SmallGuiButton(2, right - 15, guiTop +135, 10, 10, ">");
		nMac.setPressListener(b -> nextPageMacro());
		addRenderableWidget(nMac);
		SmallGuiButton pMac = new SmallGuiButton(3, right - 90, guiTop +135, 10, 10, "<");
		pMac.setPressListener(b -> prevPageMacro());
		addRenderableWidget(pMac);
		SmallGuiButton saveBtn = new SmallGuiButton(4, right - 39, bottom - 27, 35, 20, "Save");
		saveBtn.setPressListener(b -> handleSave());
		addRenderableWidget(saveBtn);
	}

	private void handleSave() {
		if (!(name1 + name2).equals("") && macroItems.size() != 0) {
			ListTag inventar = new ListTag();
			for (ItemIdentifierStack stack : macroItems) {
				CompoundTag itemNBT = new CompoundTag();
				itemNBT.putInt("id", BuiltInRegistries.ITEM.getId(stack.getItem().item));
				itemNBT.putInt("data", stack.getItem().itemDamage);
				if (stack.getItem().tag != null) {
					itemNBT.put("nbt", stack.getItem().tag);
				}
				itemNBT.putInt("amount", stack.getStackSize());
				inventar.add(itemNBT);
			}

			boolean flag = false;
			CompoundTag diskTag = logisticspipes.utils.item.StackTag.getTag(diskProvider.getDisk());
			if (diskTag == null) diskTag = new CompoundTag();
			ListTag list = diskTag.getList("macroList", 10);

			for (int i = 0; i < list.size(); i++) {
				CompoundTag tag = list.getCompound(i);
				String name = tag.getString("name");
				if (name.equals(name1 + name2)) {
					flag = true;
					tag.put("inventar", inventar);
					break;
				}
			}
			if (!flag) {
				CompoundTag nbt = new CompoundTag();
				nbt.putString("name", name1 + name2);
				nbt.put("inventar", inventar);
				list.add(nbt);
			}
			diskTag.put("macroList", list);
			logisticspipes.utils.item.StackTag.setTag(diskProvider.getDisk(), diskTag);
			MainProxy.sendPacketToServer(PacketHandler.getPacket(DiscContent.class).setStack(diskProvider.getDisk()).setPosX(diskProvider.getX()).setPosY(diskProvider.getY()).setPosZ(diskProvider.getZ()));
			exitGui();
		} else if (macroItems.size() != 0) {
			setSubGui(new GuiMessagePopup("Please enter a name"));
		} else {
			setSubGui(new GuiMessagePopup("Select some items"));
		}
	}

	@Override
	public boolean mouseClicked(double i, double j, int k) {
		mousePosX = (int) i;
		mousePosY = (int) j;
		mouseButton = k;
		int x = (int) i - guiLeft;
		int y = (int) j - guiTop;
		if (50 < x && x < 188 && 118 < y && y < 133) {
			editSearch = true;
			editName = false;
		} else if (37 < x && x < 159 && 176 < y && y < 190) {
			editSearch = false;
			editName = true;
		} else {
			editSearch = false;
			editName = false;
		}
		return super.mouseClicked(i, j, k);
	}

	// Deferred: scroll wheel handling not wired

	@Override
	protected void renderToolTips(int mouseX, int mouseY, float par3) {
		if (tooltip != null && tooltip.length >= 3) {
			getGuiGraphics().renderTooltip(minecraft.font, (net.minecraft.world.item.ItemStack) tooltip[2], (int) tooltip[0], (int) tooltip[1]);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		int panelXSize = 20;
		int panelYSize = 20;

		int ppi = 0;
		int column = 0;
		int row = 0;

		if (wheelUp != 0 || wheelDown != 0) {
			mouseButton = wheelUp != 0 ? 0 : 1;
			mousePosX = mouseX;
			mousePosY = mouseY;
		}

		tooltip = null;

		for (ItemIdentifierStack itemIdStack : diskProvider.getItemDisplay()._allItems) {
			ItemIdentifier item = itemIdStack.getItem();
			if (!itemSearched(item)) {
				continue;
			}
			ppi++;

			if (ppi <= 45 * pageAll) {
				continue;
			}
			if (ppi > 45 * (pageAll + 1)) {
				continue;
			}
			ItemStack st = itemIdStack.unsafeMakeNormalStack();
			int x = guiLeft +10 + panelXSize * column;
			int y = guiTop +18 + panelYSize * row;

			if (!super.hasSubGui()) {
				if (mouseX >= x && mouseX < x + panelXSize && mouseY >= y && mouseY < y + panelYSize) {
					getGuiGraphics().fill(x - 3, y - 1, x + panelXSize - 3, y + panelYSize - 3, Color.getValue(Color.BLACK));
					getGuiGraphics().fill(x - 2, y - 0, x + panelXSize - 4, y + panelYSize - 4, Color.getValue(Color.DARKER_GREY));
					tooltip = new Object[] { mouseX, mouseY, st };
				}

				if (mousePosX != 0 && mousePosY != 0) {
					if ((mousePosX >= x && mousePosX < x + panelXSize && mousePosY >= y && mousePosY < y + panelYSize) || (mouseX >= x && mouseX < x + panelXSize && mouseY >= y && mouseY < y + panelYSize && (wheelDown != 0 || wheelUp != 0))) {
						boolean handled = false;
						for (ItemIdentifierStack stack : macroItems) {
							if (stack.getItem().equals(item)) {
								if (mouseButton == 0 || wheelUp != 0) {
									stack.setStackSize(stack.getStackSize() + (1 + (wheelUp != 0 ? wheelUp - 1 : 0)));
								} else if (mouseButton == 1 || wheelDown != 0) {
									stack.setStackSize(stack.getStackSize() - (1 + (wheelDown != 0 ? wheelDown - 1 : 0)));
									if (stack.getStackSize() <= 0) {
										macroItems.remove(stack);
									}
								}
								handled = true;
								break;
							}
						}
						if (!handled) {
							int i = 0;
							for (ItemIdentifierStack stack : macroItems) {
								if (item.equals(stack.getItem()) && item.itemDamage < stack.getItem().itemDamage) {
									if (mouseButton == 0 || wheelUp != 0) {
										macroItems.add(i, item.makeStack(1 + (wheelUp != 0 ? wheelUp - 1 : 0)));
									} else if (mouseButton == 2) {
										macroItems.add(i, item.makeStack(64));
									}
									handled = true;
									break;
								}
								if (BuiltInRegistries.ITEM.getId(item.item) < BuiltInRegistries.ITEM.getId(stack.getItem().item)) {
									if (mouseButton == 0 || wheelUp != 0) {
										macroItems.add(i, item.makeStack(1 + (wheelUp != 0 ? wheelUp - 1 : 0)));
									} else if (mouseButton == 2) {
										macroItems.add(i, item.makeStack(64));
									}
									handled = true;
									break;
								}
								i++;
							}
							if (!handled) {
								if (mouseButton == 0 || wheelUp != 0) {
									macroItems.addLast(item.makeStack(1 + (wheelUp != 0 ? wheelUp - 1 : 0)));
								} else if (mouseButton == 2) {
									macroItems.addLast(item.makeStack(64));
								}
							}
						}
						mousePosX = 0;
						mousePosY = 0;
						wheelUp = 0;
						wheelDown = 0;
					}
				}
			}
			column++;
			if (column == 9) {
				row++;
				column = 0;
			}
		}

		ItemStackRenderer.renderItemIdentifierStackListIntoGui(diskProvider.getItemDisplay()._allItems, this, pageAll, guiLeft +9, guiTop +17, 9, 45, panelXSize, panelYSize, 100.0F, DisplayAmount.NEVER);

		ppi = 0;
		column = 0;
		row = 0;

		for (ItemIdentifierStack itemStack : macroItems) {
			ItemIdentifier item = itemStack.getItem();
			if (!itemSearched(item)) {
				continue;
			}
			ppi++;

			if (ppi <= 9 * pageMacro) {
				continue;
			}
			if (ppi > 9 * (pageMacro + 1)) {
				continue;
			}
			ItemStack st = itemStack.unsafeMakeNormalStack();
			int x = guiLeft +10 + panelXSize * column;
			int y = guiTop +150 + panelYSize * row;

			if (!super.hasSubGui()) {
				if (mouseX >= x && mouseX < x + panelXSize && mouseY >= y && mouseY < y + panelYSize) {
					tooltip = new Object[] { mouseX, mouseY, st };
				}
			}
			column++;
			if (column == 9) {
				row++;
				column = 0;
			}
		}
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(macroItems, this, pageMacro, guiLeft +10, guiTop +150, 9, 9, panelXSize, panelYSize, 100.0F, DisplayAmount.ALWAYS);
	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(minecraft, guiLeft, guiTop, right, bottom, 0.0f, false);
		getGuiGraphics().drawString(minecraft.font, "Add Macro", guiLeft +minecraft.font.width("Add Macro") / 2, guiTop +6, 0x404040, false);

		maxPageAll = (int) Math.floor((getSearchedItemNumber(diskProvider.getItemDisplay()._allItems) - 1) / 45F);
		if (maxPageAll == -1) {
			maxPageAll = 0;
		}
		if (pageAll > maxPageAll) {
			pageAll = maxPageAll;
		}

		String pageString1 = "Page " + (pageAll + 1) + " / " + (maxPageAll + 1);
		getGuiGraphics().drawString(minecraft.font, pageString1, right - 47 - minecraft.font.width(pageString1) / 2, guiTop +6, 0x404040, false);

		getGuiGraphics().drawString(minecraft.font, "Macro Items", guiLeft +minecraft.font.width("Add Macro") / 2, guiTop +136, 0x404040, false);

		maxPageMacro = (int) Math.floor((getSearchedItemNumber(macroItems) - 1) / 9F);
		if (maxPageMacro == -1) {
			maxPageMacro = 0;
		}
		if (pageMacro > maxPageMacro) {
			pageMacro = maxPageMacro;
		}

		String pageString2 = "Page " + (pageMacro + 1) + " / " + (maxPageMacro + 1);
		getGuiGraphics().drawString(minecraft.font, pageString2, right - 47 - minecraft.font.width(pageString2) / 2, guiTop +136, 0x404040, false);

		getGuiGraphics().drawString(minecraft.font, "Search:", guiLeft +8, guiTop +122, 0x404040, false);

		if (editSearch) {
			getGuiGraphics().fill(guiLeft +50, bottom - 66, right - 10, bottom - 83, Color.getValue(Color.BLACK));
			getGuiGraphics().fill(guiLeft +51, bottom - 67, right - 11, bottom - 82, Color.getValue(Color.WHITE));
		} else {
			getGuiGraphics().fill(guiLeft +51, bottom - 67, right - 11, bottom - 82, Color.getValue(Color.BLACK));
		}
		getGuiGraphics().fill(guiLeft +52, bottom - 68, right - 12, bottom - 81, Color.getValue(Color.DARKER_GREY));

		getGuiGraphics().drawString(minecraft.font, Search1 + Search2, guiLeft +55, guiTop +122, 0xFFFFFF, false);

		if (editSearch) {
			int lineX = guiLeft +55 + minecraft.font.width(Search1);
			if (System.currentTimeMillis() - oldSystemTime > 500) {
				displayCursor = !displayCursor;
				oldSystemTime = System.currentTimeMillis();
			}
			if (displayCursor) {
				getGuiGraphics().fill(lineX, guiTop +120, lineX + 1, guiTop +131, Color.getValue(Color.WHITE));
			}
		}

		getGuiGraphics().drawString(minecraft.font, "Name:", guiLeft +8, bottom - 20, 0x404040, false);

		if (editName) {
			getGuiGraphics().fill(guiLeft +36, bottom - 8, right - 40, bottom - 25, Color.getValue(Color.BLACK));
			getGuiGraphics().fill(guiLeft +37, bottom - 9, right - 41, bottom - 24, Color.getValue(Color.WHITE));
		} else {
			getGuiGraphics().fill(guiLeft +37, bottom - 9, right - 41, bottom - 24, Color.getValue(Color.BLACK));
		}
		getGuiGraphics().fill(guiLeft +38, bottom - 10, right - 42, bottom - 23, Color.getValue(Color.DARKER_GREY));

		getGuiGraphics().drawString(minecraft.font, name1 + name2, guiLeft +41, bottom - 20, 0xFFFFFF, false);

		if (editName) {
			int lineX = guiLeft +41 + minecraft.font.width(name1);
			if (System.currentTimeMillis() - oldSystemTime > 500) {
				displayCursor = !displayCursor;
				oldSystemTime = System.currentTimeMillis();
			}
			if (displayCursor) {
				getGuiGraphics().fill(lineX, bottom - 11, lineX + 1, bottom - 22, Color.getValue(Color.WHITE));
			}
		}

		getGuiGraphics().fill(guiLeft +6, guiTop +16, right - 12, bottom - 84, Color.getValue(Color.GREY));
		getGuiGraphics().fill(guiLeft +6, bottom - 52, right - 12, bottom - 32, Color.getValue(Color.DARKER_GREY));
	}

	private int getSearchedItemNumber(List<ItemIdentifierStack> list) {
		int count = 0;
		for (ItemIdentifierStack item : list) {
			if (itemSearched(item.getItem())) {
				count++;
			}
		}
		return count;
	}

	@Override
	public boolean itemSearched(ItemIdentifier item) {
		if (Search1.isEmpty() && Search2.isEmpty()) {
			return true;
		}
		if (isSearched(item.getFriendlyName().toLowerCase(Locale.US), (Search1 + Search2).toLowerCase(Locale.US))) {
			return true;
		}
		return isSearched(String.valueOf(BuiltInRegistries.ITEM.getId(item.item)), (Search1 + Search2));
	}

	private boolean isSearched(String value, String search) {
		boolean flag = true;
		for (String s : search.split(" ")) {
			if (!value.contains(s)) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	private void nextPageAll() {
		if (pageAll < maxPageAll) {
			pageAll++;
		} else {
			pageAll = 0;
		}
	}

	private void prevPageAll() {
		if (pageAll > 0) {
			pageAll--;
		} else {
			pageAll = maxPageAll;
		}
	}

	private void nextPageMacro() {
		if (pageMacro < maxPageMacro) {
			pageMacro++;
		} else {
			pageMacro = 0;
		}
	}

	private void prevPageMacro() {
		if (pageMacro > 0) {
			pageMacro--;
		} else {
			pageMacro = maxPageMacro;
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (editName) {
			if (Screen.isPaste(keyCode)) {
				name1 = name1 + Minecraft.getInstance().keyboardHandler.getClipboard();
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
				editName = false;
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
		} else if (editSearch) {
			if (Screen.isPaste(keyCode)) {
				Search1 = Search1 + Minecraft.getInstance().keyboardHandler.getClipboard();
			} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
				if (Search1.length() > 0) {
					Search1 = Search1.substring(0, Search1.length() - 1);
				}
			} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
				if (Search1.length() > 0) {
					Search2 = Search1.substring(Search1.length() - 1) + Search2;
					Search1 = Search1.substring(0, Search1.length() - 1);
				}
			} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
				if (Search2.length() > 0) {
					Search1 += Search2.substring(0, 1);
					Search2 = Search2.substring(1);
				}
			} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
				editSearch = false;
			} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_HOME) {
				Search2 = Search1 + Search2;
				Search1 = "";
			} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_END) {
				Search1 = Search1 + Search2;
				Search2 = "";
			} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
				if (Search2.length() > 0) {
					Search2 = Search2.substring(1);
				}
			}
		} else {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		return true;
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (editName) {
			if (Character.isLetterOrDigit(c) || c == ' ') {
				if (minecraft.font.width(name1 + c + name2) <= NAME_WIDTH) {
					name1 += c;
				}
				return true;
			}
		} else if (editSearch) {
			if (Character.isLetterOrDigit(c) || c == ' ') {
				if (minecraft.font.width(Search1 + c + Search2) <= SEARCH_WIDTH) {
					Search1 += c;
				}
				return true;
			}
		} else {
			return super.charTyped(c, i);
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double lpScrollX, double delta) {
		if (delta > 0) {
			wheelUp = (int) Math.max(1, delta);
			wheelDown = 0;
		} else if (delta < 0) {
			wheelDown = (int) Math.max(1, -delta);
			wheelUp = 0;
		}
		return true;
	}
}
