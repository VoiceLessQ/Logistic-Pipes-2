/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.gui;

import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;

// NEI imports removed — NEI has no 1.20.1 port; interface added at runtime via @ModDependentInterface ASM
import lombok.Getter;



import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.asm.ModDependentInterface;
import logisticspipes.asm.ModDependentMethod;
import logisticspipes.interfaces.IChainAddList;
import logisticspipes.interfaces.IFuzzySlot;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.DummyContainerSlotClick;
import logisticspipes.network.packets.gui.FuzzySlotSettingsPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.ChainAddArrayList;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.extension.GuiExtensionController;
import logisticspipes.utils.gui.extension.GuiExtensionController.GuiSide;
import network.rs485.logisticspipes.property.IBitSet;
import network.rs485.logisticspipes.util.FuzzyFlag;
import network.rs485.logisticspipes.util.FuzzyUtil;
import network.rs485.logisticspipes.util.TextUtil;

@ModDependentInterface(modId = { LPConstants.neiModID }, interfacePath = { "codechicken.nei.api.INEIGuiHandler" })
public abstract class LogisticsBaseGuiScreen extends AbstractContainerScreen implements ISubGuiControler,
		// INEIGuiHandler — added at runtime by @ModDependentInterface ASM when NEI is present
		IGuiAccess {

	protected static final ResourceLocation ITEMSINK = ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/gui/itemsink.png");

	@Getter
	protected int right;
	@Getter
	protected int bottom;
	protected int xCenter;
	protected int yCenter;
	protected final int xCenterOffset;
	protected final int yCenterOffset;

	private SubGuiScreen subGui;
	protected List<IRenderSlot> slots = new ArrayList<>();
	protected GuiExtensionController extensionControllerLeft = new GuiExtensionController(GuiSide.LEFT);
	protected GuiExtensionController extensionControllerRight = new GuiExtensionController(GuiSide.RIGHT);
	private net.minecraft.client.gui.components.AbstractWidget selectedButton;
	/** Compatibility bridge: mirrors widgets added via addRenderableWidget so old buttonList.get(i) still works. */
	protected java.util.List<net.minecraft.client.gui.components.AbstractWidget> buttonList = new java.util.ArrayList<>();

	private int currentDrawScreenMouseX;
	private int currentDrawScreenMouseY;
	/** Stored during rendering so non-render methods (drawPoint, fillRect, etc.) can use it. */
	protected GuiGraphics guiGraphics;

	public int getCurrentMouseX() { return currentDrawScreenMouseX; }
	public int getCurrentMouseY() { return currentDrawScreenMouseY; }

	private IFuzzySlot fuzzySlot;
	private boolean fuzzySlotActiveGui;
	private int fuzzySlotGuiHoverTime;
	private Queue<Runnable> renderAtTheEnd = new LinkedList<>();

	public LogisticsBaseGuiScreen(int imageWidth, int imageHeight, int xCenterOffset, int yCenterOffset) {
		this(new DummyContainer(null, null), imageWidth, imageHeight, xCenterOffset, yCenterOffset);
	}

	public LogisticsBaseGuiScreen(AbstractContainerMenu container) {
		super(container, net.minecraft.client.Minecraft.getInstance().player.getInventory(), net.minecraft.network.chat.Component.empty());
		xCenterOffset = 0;
		yCenterOffset = 0;
	}

	public LogisticsBaseGuiScreen(AbstractContainerMenu container, int imageWidth, int imageHeight, int xCenterOffset, int yCenterOffset) {
		super(container, net.minecraft.client.Minecraft.getInstance().player.getInventory(), net.minecraft.network.chat.Component.empty());
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.xCenterOffset = xCenterOffset;
		this.yCenterOffset = yCenterOffset;
	}

	@Override
	public void init() {
		super.init();
		buttonList.clear();
		leftPos = width / 2 - imageWidth / 2 + xCenterOffset;
		topPos = height / 2 - imageHeight / 2 + yCenterOffset;

		right = width / 2 + imageWidth / 2 + xCenterOffset;
		bottom = height / 2 + imageHeight / 2 + yCenterOffset;

		xCenter = (right + leftPos) / 2;
		yCenter = (bottom + topPos) / 2;
		extensionControllerLeft.setMaxBottom(bottom);
		extensionControllerRight.setMaxBottom(bottom);
	}

	@Override
	public boolean hasSubGui() {
		return subGui != null;
	}

	@Override
	public SubGuiScreen getSubGui() {
		return subGui;
	}

	@Override
	public void setSubGui(SubGuiScreen gui) {
		if (subGui == null) {
			subGui = gui;
			subGui.register(this);
			subGui.init(minecraft, width, height);
		}
	}

	@Override
	public void resize(Minecraft mc, int width, int height) {
		super.resize(mc, width, height);
		if (subGui != null) {
			subGui.resize(mc, width, height);
		}
	}

	@Override
	public void resetSubGui() {
		subGui = null;
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		if (subGui == null) {
			super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.guiGraphics = guiGraphics;
		SimpleGraphics.guiGraphics = guiGraphics;
		currentDrawScreenMouseX = mouseX;
		currentDrawScreenMouseY = mouseY;
		checkButtons();
		if (subGui != null) {
			// In 1.20.1, Mouse hack removed — subGui renders directly
			super.render(guiGraphics, 0, 0, partialTicks);
			if (!subGui.hasSubGui()) {
				super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
			}
			subGui.render(guiGraphics, mouseX, mouseY, partialTicks);
		} else {
			renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
			super.render(guiGraphics, mouseX, mouseY, partialTicks);
			for (IRenderSlot slot : slots) {
				int localMouseX = mouseX - leftPos;
				int localMouseY = mouseY - topPos;
				int mouseXMax = localMouseX - slot.getSize();
				int mouseYMax = localMouseY - slot.getSize();
				if (slot.getXPos() < localMouseX && slot.getXPos() > mouseXMax && slot.getYPos() < localMouseY && slot.getYPos() > mouseYMax) {
					if (slot.displayToolTip()) {
						if (slot.getToolTipText() != null && !slot.getToolTipText().equals("")) {
							ArrayList<String> list = new ArrayList<>();
							list.add(slot.getToolTipText());
							LPGuiGraphics.drawToolTip(mouseX, mouseY, list, ChatFormatting.WHITE);
						}
					}
				}
			}
			this.renderTooltip(guiGraphics, mouseX, mouseY);
		}
		Runnable run = renderAtTheEnd.poll();
		while (run != null) {
			run.run();
			run = renderAtTheEnd.poll();
		}
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float f, int i, int j) {
		renderExtensions();
	}

	protected void renderExtensions() {
		extensionControllerLeft.render(leftPos, topPos);
		extensionControllerRight.render(leftPos + imageWidth, topPos);
	}

	// drawSlot removed in 1.20.1 — slot rendering handled via renderSlot or renderLabels
	protected void drawSlot(Slot slot) {
		if (extensionControllerLeft.renderSlot(slot) && extensionControllerRight.renderSlot(slot)) {
			if (subGui == null) {
				onRenderSlot(slot);
			}
		}
	}

	private void onRenderSlot(Slot slot) {
		if (slot instanceof IFuzzySlot) {
			final IBitSet set = ((IFuzzySlot) slot).getFuzzyFlags();
			int x1 = slot.x;
			int y1 = slot.y;
			// GL_LIGHTING removed — use shaders
			final boolean useOreDict = FuzzyUtil.INSTANCE.get(set, FuzzyFlag.USE_ORE_DICT);
			if (useOreDict) {
				guiGraphics.fill(x1 + 8, y1 - 1, x1 + 17, y1, 0xFFFF4040);
				guiGraphics.fill(x1 + 16, y1, x1 + 17, y1 + 8, 0xFFFF4040);
			}
			final boolean ignoreDamage = FuzzyUtil.INSTANCE.get(set, FuzzyFlag.IGNORE_DAMAGE);
			if (ignoreDamage) {
				guiGraphics.fill(x1 - 1, y1 - 1, x1 + 8, y1, 0xFF40FF40);
				guiGraphics.fill(x1 - 1, y1, x1, y1 + 8, 0xFF40FF40);
			}
			final boolean ignoreNBT = FuzzyUtil.INSTANCE.get(set, FuzzyFlag.IGNORE_NBT);
			if (ignoreNBT) {
				guiGraphics.fill(x1 - 1, y1 + 16, x1 + 8, y1 + 17, 0xFF4040FF);
				guiGraphics.fill(x1 - 1, y1 + 8, x1, y1 + 17, 0xFF4040FF);
			}
			final boolean useOreCategory = FuzzyUtil.INSTANCE.get(set, FuzzyFlag.USE_ORE_CATEGORY);
			if (useOreCategory) {
				guiGraphics.fill(x1 + 8, y1 + 16, x1 + 17, y1 + 17, 0xFF7F7F40);
				guiGraphics.fill(x1 + 16, y1 + 8, x1 + 17, y1 + 17, 0xFF7F7F40);
			}
			final boolean mouseOver = this.isMouseOverSlot(slot, currentDrawScreenMouseX, currentDrawScreenMouseY);
			if (mouseOver) {
				if (fuzzySlot == slot) {
					fuzzySlotGuiHoverTime++;
					if (fuzzySlotGuiHoverTime >= 10) {
						fuzzySlotActiveGui = true;
					}
				} else {
					fuzzySlot = (IFuzzySlot) slot;
					fuzzySlotGuiHoverTime = 0;
					fuzzySlotActiveGui = false;
				}
			}
			if (fuzzySlotActiveGui && fuzzySlot == slot) {
				if (!mouseOver) {
					//Check within FuzzyGui
					if (!isHovering(slot.x, slot.y + 16, 60, 52, currentDrawScreenMouseX, currentDrawScreenMouseY)) {
						fuzzySlotActiveGui = false;
						fuzzySlot = null;
					}
				}
				final int posX = slot.x + leftPos;
				final int posY = slot.y + 17 + topPos;
				renderAtTheEnd.add(() -> {
					com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
					LPGuiGraphics.drawGuiBackGround(minecraft, posX, posY, posX + 61, posY + 47, 0.0f, true, true, true, true, true);
					final String PREFIX = "gui.crafting.";
					guiGraphics.drawString(minecraft.font, TextUtil.translate(PREFIX + "OreDict"), posX + 5, posY + 5,
							(useOreDict ? 0xFF4040 : 0x404040), false);
					guiGraphics.drawString(minecraft.font, TextUtil.translate(PREFIX + "IgnDamage"), posX + 5, posY + 15,
							(ignoreDamage ? 0x40FF40 : 0x404040), false);
					guiGraphics.drawString(minecraft.font, TextUtil.translate(PREFIX + "IgnNBT"), posX + 5, posY + 25,
							(ignoreNBT ? 0x4040FF : 0x404040), false);
					guiGraphics.drawString(minecraft.font, TextUtil.translate(PREFIX + "OrePrefix"), posX + 5, posY + 35,
							(useOreCategory ? 0x7F7F40 : 0x404040), false);
					com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
				});
			}
		}
	}

	// isMouseOverSlot(Slot, int, int) removed in 1.20.1 — use isHovering(Slot, double, double)
	protected boolean isMouseOverSlot(Slot par1Slot, int par2, int par3) {
		if (!extensionControllerLeft.renderSelectSlot(par1Slot)) {
			return false;
		}
		if (!extensionControllerRight.renderSelectSlot(par1Slot)) {
			return false;
		}
		if (isMouseInFuzzyPanel(currentDrawScreenMouseX, currentDrawScreenMouseY)) return false;
		return isHovering(par1Slot.x, par1Slot.y, 16, 16, (double)par2, (double)par3);
	}

	private boolean isMouseInFuzzyPanel(int x, int y) {
		if (!fuzzySlotActiveGui || fuzzySlot == null) return false;
		return isHovering(fuzzySlot.getX(), fuzzySlot.getY() + 16, 60, 52, x, y);
	}

	protected void checkButtons() {
		for (net.minecraft.client.gui.components.AbstractWidget button : buttonList) {
			if (extensionControllerLeft.renderButtonControlled(button)) {
				button.visible = extensionControllerLeft.renderButton(button);
			}
			if (extensionControllerRight.renderButtonControlled(button)) {
				button.visible = extensionControllerRight.renderButton(button);
			}
		}
	}

	@Nonnull
	public <T extends net.minecraft.client.gui.components.AbstractWidget> T addRenderableWidget(@Nonnull T button) {
		buttonList.add(button);
		return super.addRenderableWidget(button);
	}

	public void addRenderSlot(IRenderSlot slot) {
		slots.add(slot);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		if (par1 < leftPos) {
			extensionControllerLeft.mouseOver(par1, par2);
		}
		if (par1 > leftPos + imageWidth) {
			extensionControllerRight.mouseOver(par1, par2);
		}
		for (IRenderSlot slot : slots) {
			if (slot instanceof IItemTextureRenderSlot) {
				if (slot.drawSlotBackground()) {
					LPGuiGraphics.drawSlotBackground(minecraft, slot.getXPos(), slot.getYPos());
				}
				if (((IItemTextureRenderSlot) slot).drawSlotIcon() && !((IItemTextureRenderSlot) slot).customRender(minecraft, 0.0f)) {
					LPGuiGraphics.renderIconAt(minecraft, slot.getXPos() + 1, slot.getYPos() + 1, 0.0f, ((IItemTextureRenderSlot) slot).getTextureIcon());
				}
			} else if (slot instanceof ISmallColorRenderSlot) {
				if (slot.drawSlotBackground()) {
					LPGuiGraphics.drawSmallSlotBackground(minecraft, slot.getXPos(), slot.getYPos());
				}
				if (((ISmallColorRenderSlot) slot).drawColor()) {
					guiGraphics.fill(slot.getXPos() + 1, slot.getYPos() + 1, slot.getXPos() + 7, slot.getYPos() + 7, ((ISmallColorRenderSlot) slot).getColor());
				}
			}
		}
	}

	@Override
	public boolean mouseClicked(double par1, double par2, int par3) {
		// Popups are modal: route all input to the innermost sub-GUI (LP1 parity)
		if (subGui != null) {
			return subGui.mouseClicked(par1, par2, par3);
		}
		for (IRenderSlot slot : slots) {
			int mouseX = (int) par1 - leftPos;
			int mouseY = (int) par2 - topPos;
			int mouseXMax = mouseX - slot.getSize();
			int mouseYMax = mouseY - slot.getSize();
			if (slot.getXPos() < mouseX && slot.getXPos() > mouseXMax && slot.getYPos() < mouseY && slot.getYPos() > mouseYMax) {
				slot.mouseClicked(par3);
				return true;
			}
		}
		if (isMouseInFuzzyPanel((int)par1, (int)par2)) {
			final int posX = fuzzySlot.getX() + leftPos;
			final int posY = fuzzySlot.getY() + 17 + topPos;
			int sel = -1;
			if (par1 >= posX + 5 && par1 <= posX + 56) {
				if (par2 >= posY + 5 && par2 <= posY + 45) {
					sel = (int) (par2 - posY - 4) / 10;
				}
			}
			IBitSet set = fuzzySlot.getFuzzyFlags();
			FuzzyFlag flag = null;
			switch (sel) {
				case 0:
					flag = FuzzyFlag.USE_ORE_DICT;
					break;
				case 1:
					flag = FuzzyFlag.IGNORE_DAMAGE;
					break;
				case 2:
					flag = FuzzyFlag.IGNORE_NBT;
					break;
				case 3:
					flag = FuzzyFlag.USE_ORE_CATEGORY;
					break;
			}
			if (flag == null) return false;
			set.flip(flag.getBit());
			MainProxy.sendPacketToServer(
					PacketHandler.getPacket(FuzzySlotSettingsPacket.class)
							.setSlotNumber(fuzzySlot.getSlotId())
							.setFlags(set.copyValue()));
			return true;
		}
		// Button presses are handled by AbstractContainerScreen/Screen's own widget dispatch
		// (addRenderableWidget wires SmallGuiButton/GuiCheckBox press listeners in this class).
		boolean handled = super.mouseClicked(par1, par2, par3);
		// LP1 parity: clicks beside the window toggle the side extensions.
		if (par3 == 0 && !mouseCanPressButton((int) par1, (int) par2) && !isOverSlot((int) par1, (int) par2)) {
			if (par1 < leftPos) {
				extensionControllerLeft.mouseClicked(par1, par2, par3);
			} else if (par1 > leftPos + imageWidth) {
				extensionControllerRight.mouseClicked(par1, par2, par3);
			}
		}
		return handled;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (subGui != null) {
			return subGui.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		// LP1 parity: wheel over a ghost slot steps its amount (DummyContainer buttons 1000/1001).
		if (scrollY != 0) {
			Optional<DummySlot> slotOpt = menu.slots.stream().filter(it -> it instanceof DummySlot).map(it -> (DummySlot) it)
					.filter(it -> isMouseOverSlot(it, (int) mouseX, (int) mouseY)).findFirst();
			if (slotOpt.isPresent()) {
				DummySlot slot = slotOpt.get();
				slot.setRedirectCall(true);
				if (slot.getMaxStackSize() > 0 && slot.hasItem()) {
					int buttonActionID = scrollY > 0 ? 1000 : 1001;
					minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, buttonActionID, ClickType.SWAP, minecraft.player);
				}
				slot.setRedirectCall(false);
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (subGui != null) {
			return subGui.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (subGui != null) {
			return subGui.keyPressed(keyCode, scanCode, modifiers);
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char c, int modifiers) {
		if (subGui != null) {
			return subGui.charTyped(c, modifiers);
		}
		return super.charTyped(c, modifiers);
	}

	@Override
	public boolean mouseReleased(double par1, double par2, int par3) {
		if (subGui != null) {
			return subGui.mouseReleased(par1, par2, par3);
		}
		if (selectedButton != null && par3 == 0) {
			selectedButton.mouseReleased(par1, par2, 0);
			selectedButton = null;
			return true;
		} else if (isMouseInFuzzyPanel((int)(par1 - leftPos), (int)(par2 - topPos))) {
			return false;
		} else {
			return super.mouseReleased(par1, par2, par3);
		}
	}

	private boolean mouseCanPressButton(int par1, int par2) {
		for (net.minecraft.client.gui.components.AbstractWidget b : buttonList) {
			if (b.visible && b.isMouseOver(par1, par2)) {
				return true;
			}
		}
		return false;
	}

	private boolean isOverSlot(int par1, int par2) {
		for (int k = 0; k < menu.slots.size(); ++k) {
			Slot slot = menu.slots.get(k);
			if (isMouseOverSlot(slot, par1, par2)) {
				return true;
			}
		}
		return false;
	}

	public void drawPoint(int x, int y, int color) {
		guiGraphics.fill(x, y, x + 1, y + 1, color);
	}

	public void drawPoint(int x, int y, Color color) {
		guiGraphics.fill(x, y, x + 1, y + 1, Color.getValue(color));
	}

	public void fillRect(int x1, int y1, int x2, int y2, Color color) {
		guiGraphics.fill(x1, y1, x2, y2, Color.getValue(color));
	}

	public void drawLine(int x1, int y1, int x2, int y2, Color color) {
		int lasty = y1;
		for (int dx = 0; x1 + dx < x2; dx++) {
			int plotx = x1 + dx;
			int ploty;

			if (x2 - x1 == 1) ploty = y1 + (y2 - y1) / (x2 - x1) * dx;
			else ploty = y1 + (y2 - y1) / (x2 - x1 - 1) * dx;

			drawPoint(plotx, ploty, color);
			while (lasty < ploty) {
				drawPoint(plotx, ++lasty, color);
			}
			while (lasty > ploty) {
				drawPoint(plotx, --lasty, color);
			}
		}
		while (lasty < y2) {
			drawPoint(x2, ++lasty, color);
		}
		while (lasty > y2) {
			drawPoint(x2, --lasty, color);
		}
	}

	public void closeGui() throws IOException {
		onClose();
	}

	@Override
	public Minecraft getMC() {
		return minecraft;
	}

	@Override
	public net.minecraft.client.gui.GuiGraphics getGuiGraphics() {
		return guiGraphics;
	}

	@Override
	public LogisticsBaseGuiScreen getBaseScreen() {
		return this;
	}

	// @Override removed — INEIGuiHandler not in implements (added at runtime by ASM)
	@ModDependentMethod(modId = LPConstants.neiModID)
	public List<Object> getInventoryAreas(AbstractContainerScreen gui) { // was: List<TaggedInventoryArea>
		return null;
	}

	// @Override removed — INEIGuiHandler not in implements
	@ModDependentMethod(modId = LPConstants.neiModID)
	public Iterable<Integer> getItemSpawnSlots(AbstractContainerScreen gui, @Nonnull ItemStack stack) {
		return null;
	}

	// @Override removed — INEIGuiHandler not in implements
	@ModDependentMethod(modId = LPConstants.neiModID)
	public boolean handleDragNDrop(AbstractContainerScreen gui, int mouseX, int mouseY, @Nonnull ItemStack stack, int button) {
		if (gui instanceof LogisticsBaseGuiScreen && gui.getMenu() instanceof DummyContainer && !stack.isEmpty()) {
			Slot result = null;
			int pos = -1;
			for (int k = 0; k < menu.slots.size(); ++k) {
				Slot slot = menu.slots.get(k);
				if (isMouseOverSlot(slot, mouseX, mouseY)) {
					result = slot;
					pos = k;
					break;
				}
			}
			if (result != null) {
				if (result instanceof DummySlot || result instanceof ColorSlot || result instanceof FluidSlot) {
					((DummyContainer) gui.getMenu()).handleDummyClick(result, pos, stack, button, ClickType.PICKUP, minecraft.player);
					MainProxy.sendPacketToServer(PacketHandler.getPacket(DummyContainerSlotClick.class).setSlotId(pos).setStack(stack).setButton(button));
					return true;
				}
			}
		}
		return false;
	}

	// @Override removed — INEIGuiHandler not in implements
	@ModDependentMethod(modId = LPConstants.neiModID)
	public boolean hideItemPanelSlot(AbstractContainerScreen gui, int x, int y, int w, int h) {
		if (gui instanceof LogisticsBaseGuiScreen) {
			return ((LogisticsBaseGuiScreen) gui).extensionControllerRight.isOverPanel(x, y, w, h);
		}
		return false;
	}

	public IChainAddList<EventListener> onGuiEvents = new ChainAddArrayList<>();

	public List<Rectangle> getGuiExtraAreas() {
		return Stream.concat(
			extensionControllerLeft.getGuiExtraAreas().stream(), extensionControllerRight.getGuiExtraAreas().stream()).collect(Collectors.toList());
	}

	public interface EventListener {

		void onUpdateScreen();

		boolean onKeyboardInput();

	}

	public void updateScreen() {
		for (EventListener el : onGuiEvents)
			el.onUpdateScreen();
	}

	public void drawCenteredString(String text, int x, int y, int color) {
		int actualX = x - minecraft.font.width(text) / 2;
		guiGraphics.drawString(minecraft.font, text, actualX, y, color, false);
	}

}
