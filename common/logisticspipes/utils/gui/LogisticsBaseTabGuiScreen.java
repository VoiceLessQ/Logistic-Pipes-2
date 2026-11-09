
package logisticspipes.utils.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import net.minecraft.world.inventory.Slot;
import javax.annotation.Nonnull;




public class LogisticsBaseTabGuiScreen extends LogisticsBaseGuiScreen {

	private int current_Tab;

	private int buttonNextFreeId = 0;

	private final List<TabSubGui> tabList = new ArrayList<>();
	private final List<Slot> hiddenSlots = new ArrayList<>();

	public LogisticsBaseTabGuiScreen(int xSize, int ySize) {
		super(xSize, ySize, 0, 0);
	}

	public LogisticsBaseTabGuiScreen(net.minecraft.world.inventory.AbstractContainerMenu container, int xSize, int ySize) {
		super(container, xSize, ySize, 0, 0);
	}

	@Override
	public void init() {
		super.init();
		clearWidgets();
		tabList.forEach(TabSubGui::initTab);
	}

	@Override
	public void closeGui() throws IOException {
		super.closeGui();
		
		init();
	}

	protected int getFreeButtonId() {
		return buttonNextFreeId++;
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphicsExtractor guiGraphics, float f, int mouse_x, int mouse_y) {
		SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		for (int i = 0; i < tabList.size(); i++) {
			LPGuiGraphics.drawGuiBackGround(minecraft, leftPos + (25 * i) + 2, topPos - 2, leftPos + 27 + (25 * i), topPos + 35, 0.0f, false, true, true, false, true);
		}
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos + 20, right, bottom, 0.0f, true);
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos + (25 * current_Tab) + 2, topPos - 2, leftPos + 27 + (25 * current_Tab), topPos + 38, 0.0f, true, true, true, false, true);
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 10, topPos + 135);

		int x = 6;
		for (TabSubGui aTabList : tabList) {
			aTabList.renderIcon(leftPos + x, topPos + 3);
			x += 25;
		}

		for (int i = 0; i < tabList.size(); i++) {
			if (current_Tab == i) {
				tabList.get(i).renderBackgroundContent();
			}
		}

		super.renderBg(guiGraphics, f, mouse_x, mouse_y);
	}

	@Override
	public boolean mouseClicked(double par1, double par2, int par3) {
		if (par3 == 0 && par1 > leftPos && par1 < leftPos + 220 && par2 > topPos && par2 < topPos + 20) {
			par1 -= leftPos + 3;
			int select = Math.max(0, Math.min((int)(par1 / 25), tabList.size() - 1));
			if (current_Tab != select) {
				tabList.get(current_Tab).leavingTab();
				tabList.get(select).enteringTab();
			}
			current_Tab = select;
			return true;
		} else {
			for (int i = 0; i < tabList.size(); i++) {
				if (current_Tab == i) {
					if (tabList.get(i).handleClick((int)par1, (int)par2, par3)) {
						return true;
					}
				}
			}
			return super.mouseClicked(par1, par2, par3);
		}
	}

	@Override
	public boolean charTyped(char p_73869_1_, int p_73869_2_) {
		for (int i = 0; i < tabList.size(); i++) {
			if (current_Tab == i) {
				if (tabList.get(i).handleKey(p_73869_2_, p_73869_1_)) {
					return true;
				}
			}
		}
		if (p_73869_2_ == 1) {
			tabList.forEach(TabSubGui::guiClose);
		}
		return super.charTyped(p_73869_1_, p_73869_2_);
	}

	@Override
	protected void renderLabels(GuiGraphicsExtractor guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		for (int i = 0; i < tabList.size(); i++) {
			if (current_Tab == i) {
				tabList.get(i).renderForegroundContent();
			}
		}
	}

	@Override
	protected void extractSlot(GuiGraphicsExtractor guiGraphics, Slot slot, int mouseX, int mouseY) {
		if (hiddenSlots.contains(slot)) return;
		for (int i = 0; i < tabList.size(); i++) {
			if (tabList.get(i).isSlotForTab(slot)) {
				if (current_Tab != i || !tabList.get(i).showSlot(slot)) {
					return;
				}
			}
		}
		super.extractSlot(guiGraphics, slot, mouseX, mouseY);
	}

	@Override
	protected boolean isMouseOverSlot(Slot slot, int par2, int par3) {
		if (!super.isMouseOverSlot(slot, par2, par3)) {
			return false;
		}
		if (hiddenSlots.contains(slot)) return false;
		for (int i = 0; i < tabList.size(); i++) {
			if (tabList.get(i).isSlotForTab(slot)) {
				if (current_Tab != i || !tabList.get(i).showSlot(slot)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	protected void checkButtons() {
		super.checkButtons();
		for (net.minecraft.client.gui.components.AbstractWidget widget : buttonList) {
			if (!(widget instanceof net.minecraft.client.gui.components.AbstractButton)) continue;
			net.minecraft.client.gui.components.AbstractButton button = (net.minecraft.client.gui.components.AbstractButton) widget;
			for (int i = 0; i < tabList.size(); i++) {
				if (tabList.get(i).isButtonFromGui(button)) {
					tabList.get(i).checkButton(button, current_Tab == i);
				}
			}
		}
	}

	protected void addTab(TabSubGui gui) {
		tabList.add(gui);
	}

	protected Slot addHiddenSlot(Slot slot) {
		hiddenSlots.add(slot);
		return slot;
	}

	protected abstract class TabSubGui {

		private final List<Slot> TAB_SLOTS = new ArrayList<>();
		private final List<net.minecraft.client.gui.components.AbstractButton> TAB_BUTTONS = new ArrayList<>();

		public abstract void renderIcon(int x, int y);

		public abstract void renderBackgroundContent();

		public abstract void renderForegroundContent();

		public boolean isSlotForTab(Slot slot) {
			return TAB_SLOTS.contains(slot);
		}

		public Slot addSlot(Slot slot) {
			TAB_SLOTS.add(slot);
			return slot;
		}

		public net.minecraft.client.gui.components.AbstractButton addRenderableWidget(net.minecraft.client.gui.components.AbstractButton button) {
			TAB_BUTTONS.add(LogisticsBaseTabGuiScreen.this.addRenderableWidget(button));
			if (button instanceof SmallGuiButton smallButton) {
				smallButton.setPressListener(this::buttonClicked);
			} else if (button instanceof GuiCheckBox checkBox) {
				checkBox.setPressListener(this::buttonClicked);
			}
			return button;
		}

		public boolean isButtonFromGui(net.minecraft.client.gui.components.AbstractButton button) {
			return TAB_BUTTONS.contains(button);
		}

		public void initTab() {}

		public void checkButton(net.minecraft.client.gui.components.AbstractButton button, boolean isTabActive) {
			if (TAB_BUTTONS.contains(button)) {
				button.visible = isTabActive;
			}
		}

		public void buttonClicked(net.minecraft.client.gui.components.AbstractButton button) {}

		public boolean handleClick(int x, int y, int type) {
			return false;
		}

		public boolean handleKey(int code, char c) {
			return false;
		}

		public void guiClose() {}

		public boolean showSlot(Slot slot) {
			return true;
		}

		public void leavingTab() {}

		public void enteringTab() {}
	}
}
