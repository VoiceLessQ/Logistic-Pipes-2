package logisticspipes.gui.popup;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Map;


import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;



import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.IItemSearch;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.TextUtil;

public class SelectItemOutOfList extends SubGuiScreen implements IItemSearch {

	public interface IHandleItemChoice {

		void handleItemChoice(int slot);
	}

	private final List<ItemIdentifierStack> candidate;
	private final IHandleItemChoice handler;
	private ItemDisplay itemDisplay = null;
	private InputBar search;

	public SelectItemOutOfList(List<ItemIdentifierStack> candidate, IHandleItemChoice handler) {
		super(156, 188, 0, 0);
		this.candidate = candidate;
		this.handler = handler;
	}

	@Override
	public void init() {
		
		super.init();
		SmallGuiButton prev = new SmallGuiButton(0, guiLeft + 70, guiTop + 5, 10, 10, "<");
		prev.setPressListener(b -> itemDisplay.prevPage());
		addRenderableWidget(prev);
		SmallGuiButton next = new SmallGuiButton(1, guiLeft + 138, guiTop + 5, 10, 10, ">");
		next.setPressListener(b -> itemDisplay.nextPage());
		addRenderableWidget(next);
		SmallGuiButton sel = new SmallGuiButton(2, guiLeft + 100, bottom - 26, 50, 20, "Select");
		sel.setPressListener(b -> {
			ItemIdentifierStack stack = itemDisplay.getSelectedItem();
			int index = candidate.indexOf(stack);
			if (index >= 0) {
				handler.handleItemChoice(index);
			}
			exitGui();
		});
		addRenderableWidget(sel);

		if (search == null) {
			search = new InputBar(font, this.getBaseScreen(), guiLeft + 7, bottom - 23, right - guiLeft - 64, 15, false);
		}
		search.reposition(guiLeft + 7, bottom - 23, right - guiLeft - 64, 15);

		if (itemDisplay == null) {
			itemDisplay = new ItemDisplay(this, font, this.getBaseScreen(), null, guiLeft + 10, guiTop + 18, xSize - 20, ySize - 48, 0, 0, 0, new int[] { 1, 10, 64, 64 }, true);
			itemDisplay.setItemList(candidate);
		}
		itemDisplay.reposition(guiLeft + 8, guiTop + 18, xSize - 16, ySize - 48, 0, 0);
	}

	@Override
	public void exitGui() {
		super.exitGui();
		
		getBaseScreen().init();
	}

	@Override
	protected void renderToolTips(int mouseX, int mouseY, float par3) {
		Object[] tip = itemDisplay != null ? itemDisplay.getToolTip() : null;
		if (tip != null && tip.length >= 3) {
			getGuiGraphics().renderTooltip(minecraft.font, (net.minecraft.world.item.ItemStack) tip[2], (int) tip[0], (int) tip[1]);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(minecraft, guiLeft, guiTop, right, bottom, 0.0f, true);
		getGuiGraphics().drawString(font, TextUtil.translate("misc.selectType"), guiLeft + 8, guiTop + 6, 0x404040, false);

		itemDisplay.renderPageNumber(right - 47, guiTop + 6);

		//SearchInput
		search.drawTextBox();

		//itemDisplay.renderSortMode(xCenter, bottom - 52);
		itemDisplay.renderItemArea(0.0f);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			itemDisplay.handleMouse(scrollY);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_A && net.minecraft.client.gui.screens.Screen.hasControlDown()) {
			itemDisplay.setMaxAmount();
			return true;
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_D && net.minecraft.client.gui.screens.Screen.hasControlDown()) {
			itemDisplay.resetAmount();
			return true;
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) {
			itemDisplay.prevPage();
			return true;
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) {
			itemDisplay.nextPage();
			return true;
		}
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || !search.keyPressed(keyCode, scanCode, modifiers)) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		return true;
	}

	@Override
	public boolean charTyped(char par1, int par2) {
		if (!itemDisplay.keyTyped(par1, par2)) {
			if (!search.handleKey(par1, par2)) {
				return super.charTyped(par1, par2);
			}
		}
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (itemDisplay.handleClick((int) mouseX, (int) mouseY, button)) return true;
		if (search.handleClick((int) mouseX, (int) mouseY, button)) return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean itemSearched(ItemIdentifier item) {
		if (search.isEmpty()) {
			return true;
		}
		if (isSearched(item.getFriendlyName().toLowerCase(Locale.US), search.getText().toLowerCase(Locale.US))) {
			return true;
		}
		//if(isSearched(String.valueOf(BuiltInRegistries.ITEM.getId(item.item)), search.getContent())) return true;
		//Enchantment? Enchantment!
		net.minecraft.world.item.enchantment.ItemEnchantments enchantIdLvlMap = item.unsafeMakeNormalStack(1).getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
		for (var e : enchantIdLvlMap.entrySet()) {
			String enchantName = e.getKey().value().description().getString();
			if (enchantName != null) {
				if (isSearched(enchantName.toLowerCase(Locale.US), search.getText().toLowerCase(Locale.US))) {
					return true;
				}
			}
		}
		return false;
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
}
