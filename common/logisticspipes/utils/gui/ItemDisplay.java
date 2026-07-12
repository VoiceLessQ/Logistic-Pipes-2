package logisticspipes.utils.gui;
import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.screens.Screen;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;



import lombok.Getter;




import logisticspipes.config.Configs;
import logisticspipes.interfaces.ISpecialItemRenderer;
import logisticspipes.utils.Color;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import logisticspipes.utils.tuples.Pair;

public class ItemDisplay {

	public enum DisplayOption {
		ID,
		ID_DOWN,
		SIZE,
		SIZE_DOWN,
		NAME,
		NAME_DOWN,
	}

	private static final ResourceLocation TEXTURE = ResourceLocation.parse("textures/gui/icons.png");
	private static final int PANELSIZEX = 20;
	private static final int PANELSIZEY = 20;

	private ItemIdentifierStack selectedItem = null;
	public final LinkedList<ItemIdentifierStack> _allItems = new LinkedList<>();
	private final Map<Pair<Integer, Integer>, ItemIdentifierStack> map = new HashMap<>();

	@Getter
	private int page = 0;
	private int maxPage = 0;
	//private int requestCount = 1;
	private InputBar requestCountBar;
	private Object[] tooltip = null;
	private boolean listbyserver = false;

	private final IItemSearch search;
	private final Font font;
	private final LogisticsBaseGuiScreen screen;
	private final ISpecialItemRenderer renderer;
	private int left, top, height, width, amountPosLeft, amountPosTop, amountWidth;
	private int itemsPerPage;
	private final int[] amountChangeMode;
	private final boolean shiftPageChange;
	private final Minecraft mc = Minecraft.getInstance();
	private static DisplayOption option = DisplayOption.ID;
	private final ItemStackRenderer stackRenderer = new ItemStackRenderer(0, 0, 100.0F, false, false);

	public ItemDisplay(IItemSearch search, Font font, LogisticsBaseGuiScreen screen, ISpecialItemRenderer renderer, int left, int top, int width, int height, int amountPosLeft, int amountPosTop, int amountWidth, int[] amountChangeMode, boolean shiftPageChange) {
		this.search = search;
		this.font = font;
		this.screen = screen;
		this.renderer = renderer;
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
		this.amountPosLeft = amountPosLeft;
		this.amountPosTop = amountPosTop;
		this.amountWidth = amountWidth;
		itemsPerPage = this.width * this.height / (20 * 20);
		if (amountChangeMode.length != 4) {
			throw new UnsupportedOperationException("amountChangeMode.length needs to be 4");
		}
		this.amountChangeMode = amountChangeMode;
		this.shiftPageChange = shiftPageChange;
		this.requestCountBar = new InputBar(this.font, screen, amountPosLeft - (amountWidth / 2), amountPosTop - 5, amountWidth, 12, false, true, InputBar.Align.CENTER);
		this.requestCountBar.minNumber = 1;
		this.requestCountBar.putInt(1);
	}

	public void reposition(int left, int top, int width, int height, int amountPosLeft, int amountPosTop) {
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
		this.amountPosLeft = amountPosLeft;
		this.amountPosTop = amountPosTop;
		itemsPerPage = this.width * this.height / (20 * 20);
		this.requestCountBar.reposition(amountPosLeft - (this.amountWidth / 2), amountPosTop - 2, this.amountWidth, 12);
	}

	public void setItemList(Collection<ItemIdentifierStack> allItems) {
		listbyserver = true;
		_allItems.clear();
		allItems.stream().sorted(new ItemidStackDisplayOptionComparator()).forEach(_allItems::add);
		boolean found = false;
		if (selectedItem == null) {
			return;
		}
		for (ItemIdentifierStack itemStack : _allItems) {
			if (itemStack.getItem().equals(selectedItem.getItem())) {
				selectedItem = itemStack;
				found = true;
				break;
			}
		}
		if (!found) {
			selectedItem = null;
		}
	}

	private static class ItemidStackDisplayOptionComparator implements Comparator<ItemIdentifierStack> {

		@Override
		public int compare(ItemIdentifierStack o1, ItemIdentifierStack o2) {
			if (ItemDisplay.option == DisplayOption.ID) {
				int c = basicCompare(o1, o2);
				if (c != 0) {
					return c;
				}
				return o2.getStackSize() - o1.getStackSize();
			} else if (ItemDisplay.option == DisplayOption.ID_DOWN) {
				int c = basicCompare(o2, o1);
				if (c != 0) {
					return c;
				}
				return o1.getStackSize() - o2.getStackSize();
			} else if (ItemDisplay.option == DisplayOption.SIZE) {
				int c = o2.getStackSize() - o1.getStackSize();
				if (c != 0) {
					return c;
				}
				return basicCompare(o1, o2);
			} else if (ItemDisplay.option == DisplayOption.SIZE_DOWN) {
				int c = o1.getStackSize() - o2.getStackSize();
				if (c != 0) {
					return c;
				}
				return basicCompare(o2, o1);
			} else if (ItemDisplay.option == DisplayOption.NAME) {
				int c = o1.getItem().getFriendlyName().compareToIgnoreCase(o2.getItem().getFriendlyName());
				if (c != 0) {
					return c;
				}
				c = basicCompare(o1, o2);
				if (c != 0) {
					return c;
				}
				return o2.getStackSize() - o1.getStackSize();
			} else if (ItemDisplay.option == DisplayOption.NAME_DOWN) {
				int c = o2.getItem().getFriendlyName().compareToIgnoreCase(o1.getItem().getFriendlyName());
				if (c != 0) {
					return c;
				}
				c = basicCompare(o2, o1);
				if (c != 0) {
					return c;
				}
				return o1.getStackSize() - o2.getStackSize();
			} else {
				int c = basicCompare(o1, o2);
				if (c != 0) {
					return c;
				}
				return o2.getStackSize() - o1.getStackSize();
			}
		}

		private int basicCompare(ItemIdentifierStack o1, ItemIdentifierStack o2) {
			return o1.compareTo(o2);
		}
	}

	public void cycle() {
		int i = ItemDisplay.option.ordinal();
		i++;
		if (i >= DisplayOption.values().length) {
			i = 0;
		}
		ItemDisplay.option = DisplayOption.values()[i];
		_allItems.sort(new ItemidStackDisplayOptionComparator());
	}

	public void renderSortMode(int x, int y) {
		String name = ItemDisplay.option.name();
		boolean up = true;
		if (name.endsWith("_DOWN")) {
			name = name.substring(0, name.length() - 5);
			up = false;
		}
		name += !up ? " /\\" : " \\/";
		screen.guiGraphics.drawString(font, name, x - font.width(name) / 2, y, 0x404040, false);
	}

	public void renderPageNumber(int x, int y) {
		maxPage = (getSearchedItemNumber() - 1) / itemsPerPage;
		if (maxPage == -1) {
			maxPage = 0;
		}
		if (page > maxPage) {
			page = maxPage;
		}
		String pageString = "Page " + (page + 1) + " / " + (maxPage + 1);
		screen.guiGraphics.drawString(font, pageString, x - font.width(pageString) / 2, y, 0x404040, false);
	}

	private int getSearchedItemNumber() {
		int count = 0;
		for (ItemIdentifierStack item : _allItems) {
			if (search == null || search.itemSearched(item.getItem())) {
				count++;
			}
		}
		return count;
	}

	public void renderAmount(int stackAmount) {
		int requestCount = requestCountBar.getInt();
		String StackrequestCount = "" + (requestCount / stackAmount) + "+" + (requestCount % stackAmount);
		//screen.guiGraphics.drawString(font, requestCount + "", x - font.width(requestCount + "") / 2, y, 0x404040, false);
		screen.guiGraphics.drawString(font, StackrequestCount + "", this.amountPosLeft - font.width(StackrequestCount + "") / 2, this.amountPosTop + 11, 0x404040, false);

		requestCountBar.drawTextBox();
	}

	public void renderItemArea(double zLevel) {
		screen.guiGraphics.fill(left, top, left + width, top + height, Color.getValue(Color.GREY));

		tooltip = null;
		int ppi = 0;
		int panelxSize = 20;
		int panelySize = 20;
		int x = 2;
		int y = 2;
		// Mouse coords relative to the display area origin (left, top)
		int mouseX = screen.getCurrentMouseX() - left;
		int mouseY = screen.getCurrentMouseY() - top;

		for (ItemIdentifierStack itemIdentifierStack : _allItems) {
			ItemIdentifier item = itemIdentifierStack.getItem();
			if (search != null && !search.itemSearched(item)) {
				continue;
			}
			ppi++;

			if (ppi <= itemsPerPage * page) {
				continue;
			}
			if (ppi > itemsPerPage * (page + 1)) {
				break;
			}

			// -2 on both, because field starts there (see black rect below)
			int realX = x - 2;
			int realY = y - 2;

			Pair<Integer, Integer> pair = new Pair<>(realX, realY);
			if (map.get(pair) != itemIdentifierStack) {
				map.put(pair, itemIdentifierStack);
			}

			// All fill calls use left/top offset to convert from area-relative to screen coords
			if (mouseX >= realX && mouseX < realX + panelxSize && mouseY >= realY && mouseY < realY + panelySize) {
				screen.guiGraphics.fill(left + x - 2, top + y - 2, left + x + panelxSize - 2, top + y + panelySize - 2, Color.getValue(Color.BLACK));
				screen.guiGraphics.fill(left + x - 1, top + y - 1, left + x + panelxSize - 3, top + y + panelySize - 3, Color.getValue(Color.DARKER_GREY));

				if (itemIdentifierStack.getStackSize() > 0) {
					tooltip = new Object[] { mouseX + left, mouseY + top, itemIdentifierStack.unsafeMakeNormalStack() };
				} else {
					tooltip = new Object[] { mouseX + left, mouseY + top, itemIdentifierStack.getItem().unsafeMakeNormalStack(1) };
				}
			}

			if (selectedItem == itemIdentifierStack) {
				screen.guiGraphics.fill(left + x - 2, top + y - 2, left + x + panelxSize - 2, top + y + panelySize - 2, Color.getValue(Color.BLACK));
				screen.guiGraphics.fill(left + x - 1, top + y - 1, left + x + panelxSize - 3, top + y + panelySize - 3, Color.getValue(Color.LIGHTER_GREY));
				screen.guiGraphics.fill(left + x, top + y, left + x + panelxSize - 4, top + y + panelySize - 4, Color.getValue(Color.DARKER_GREY));
				if (renderer != null) {
					renderer.specialItemRendering(itemIdentifierStack.getItem(), left + x, top + y);
				}
			}

			stackRenderer.setPosX(left + x).setPosY(top + y).setItemIdentStack(itemIdentifierStack).setDisplayAmount(DisplayAmount.HIDE_ONE).renderInGui();

			x += panelxSize;
			if (x > width) {
				x = 2;
				y += panelySize;
			}
		}
	}

	public void handleMouse() {
		// Legacy no-arg entry — scroll now routes through mouseScrolled → handleMouse(double delta).
	}

	public void handleMouse(double delta) {
		boolean isShift = Screen.hasShiftDown();
		boolean isControl = Screen.hasControlDown();
		int wheel = (int)(delta);
		if (wheel == 0) {
			return;
		}

		if (isShift && !isControl && isShiftPageChange()) {
			if (wheel > 0) {
				if (!Configs.LOGISTICS_ORDERER_PAGE_INVERTWHEEL) {
					prevPage();
				} else {
					nextPage();
				}
			} else {
				if (!Configs.LOGISTICS_ORDERER_PAGE_INVERTWHEEL) {
					nextPage();
				} else {
					prevPage();
				}
			}
		} else if (!requestCountBar.isFocused()) {
			int requestCount = requestCountBar.getInt();
			if (isShift && !isControl && !isShiftPageChange()) {
				if (wheel > 0) {
					if (!Configs.LOGISTICS_ORDERER_COUNT_INVERTWHEEL) {
						requestCount = Math.max(1, requestCount - (wheel * getAmountChangeMode(4)));
					} else {
						if (requestCount == 1) {
							requestCount -= 1;
						}
						requestCount += wheel * getAmountChangeMode(4);
					}
				} else {
					if (!Configs.LOGISTICS_ORDERER_COUNT_INVERTWHEEL) {
						if (requestCount == 1) {
							requestCount -= 1;
						}
						requestCount += -(wheel * getAmountChangeMode(4));
					} else {
						requestCount = Math.max(1, requestCount + wheel * getAmountChangeMode(4));
					}
				}
			} else if (!isControl) {
				if (wheel > 0) {
					if (!Configs.LOGISTICS_ORDERER_COUNT_INVERTWHEEL) {
						requestCount = Math.max(1, requestCount - (wheel * getAmountChangeMode(1)));
					} else {
						requestCount += wheel * getAmountChangeMode(1);
					}
				} else {
					if (!Configs.LOGISTICS_ORDERER_COUNT_INVERTWHEEL) {
						requestCount += -(wheel * getAmountChangeMode(1));
					} else {
						requestCount = Math.max(1, requestCount + wheel * getAmountChangeMode(1));
					}
				}
			} else if (isControl && !isShift) {
				if (wheel > 0) {
					if (!Configs.LOGISTICS_ORDERER_COUNT_INVERTWHEEL) {
						requestCount = Math.max(1, requestCount - wheel * getAmountChangeMode(2));
					} else {
						if (requestCount == 1) {
							requestCount -= 1;
						}
						requestCount += wheel * getAmountChangeMode(2);
					}
				} else {
					if (!Configs.LOGISTICS_ORDERER_COUNT_INVERTWHEEL) {
						if (requestCount == 1) {
							requestCount -= 1;
						}
						requestCount += -wheel * getAmountChangeMode(2);
					} else {
						requestCount = Math.max(1, requestCount + wheel * getAmountChangeMode(2));
					}
				}
			} else if (isControl && isShift) {
				if (wheel > 0) {
					if (!Configs.LOGISTICS_ORDERER_COUNT_INVERTWHEEL) {
						requestCount = Math.max(1, requestCount - wheel * getAmountChangeMode(3));
					} else {
						if (requestCount == 1) {
							requestCount -= 1;
						}
						requestCount += wheel * getAmountChangeMode(3);
					}
				} else {
					if (!Configs.LOGISTICS_ORDERER_COUNT_INVERTWHEEL) {
						if (requestCount == 1) {
							requestCount -= 1;
						}
						requestCount += -wheel * getAmountChangeMode(3);
					} else {
						requestCount = Math.max(1, requestCount + wheel * getAmountChangeMode(3));
					}
				}
			}
			requestCountBar.putInt(requestCount);
		}
	}

	private int getAmountChangeMode(int step) {
		return amountChangeMode[step - 1];
	}

	private boolean isShiftPageChange() {
		return shiftPageChange;
	}

	public Object[] getToolTip() {
		return tooltip;
	}

	public void resetAmount() {
		requestCountBar.putInt(1);
	}

	public void setMaxAmount() {
		if (selectedItem != null && selectedItem.getStackSize() != 0) {
			requestCountBar.putInt(selectedItem.getStackSize());
		}
	}

	public void nextPage() {
		if (page < maxPage) {
			page++;
		} else {
			page = 0;
		}
	}

	public void prevPage() {
		if (page > 0) {
			page--;
		} else {
			page = maxPage;
		}
	}

	public void add(int i) {
		int requestCount = requestCountBar.getInt();
		if (i != 1 && requestCount == 1) {
			requestCount -= 1;
		}
		requestCountBar.putInt(requestCount + getAmountChangeMode(i));
	}

	public void sub(int i) {
		requestCountBar.putInt(requestCountBar.getInt() - getAmountChangeMode(i));
	}

	public ItemIdentifierStack getSelectedItem() {
		return selectedItem;
	}

	public int getRequestCount() {
		return requestCountBar.getInt();
	}

	public boolean handleClick(int x, int y, int k) {
		if (requestCountBar.handleClick(x, y, k)) {
			return true;
		}
		x -= left;
		y -= top;
		if (x < 0 || y < 0 || x > width || y > height) {
			return false;
		}
		selectedItem = null;
		for (Entry<Pair<Integer, Integer>, ItemIdentifierStack> entry : map.entrySet()) {
			if (x >= entry.getKey().getValue1() && x < entry.getKey().getValue1() + ItemDisplay.PANELSIZEX && y >= entry.getKey().getValue2() && y < entry.getKey().getValue2() + ItemDisplay.PANELSIZEY) {
				selectedItem = entry.getValue();
				return true;
			}
		}
		return false;
	}

	public boolean keyTyped(char c, int i) {
		if (!requestCountBar.handleKey(c, i)) {
			if (i == 30 && Screen.hasControlDown()) { //Ctrl-a
				setMaxAmount();
				return true;
			} else if (i == 32 && Screen.hasControlDown()) { //Ctrl-d
				resetAmount();
				return true;
			} else if (i == 201) { //PgUp
				prevPage();
				return true;
			} else if (i == 209) { //PgDn
				nextPage();
				return true;
			}
			return false;
		}
		return true;
	}

	public void setFocused(boolean value) {
		requestCountBar.setFocused(value);
	}
}
