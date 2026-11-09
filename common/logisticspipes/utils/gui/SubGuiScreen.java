package logisticspipes.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import lombok.Getter;


public abstract class SubGuiScreen extends Screen implements ISubGuiControler, IGuiAccess {

	@Getter
	protected int guiLeft;
	@Getter
	protected int guiTop;
	protected int xCenter;
	protected int yCenter;
	@Getter
	protected int right;
	@Getter
	protected int bottom;
	@Getter
	protected int xSize;
	@Getter
	protected int ySize;
	protected int xCenterOffset;
	protected int yCenterOffset;
	protected ISubGuiControler controler;
	private SubGuiScreen subGui;
	private int inputModifiers;
	private boolean inputDoubleClick;
	protected net.minecraft.client.gui.GuiGraphicsExtractor storedGuiGraphics;

	public SubGuiScreen(int xSize, int ySize, int xOffset, int yOffset) {
		super(net.minecraft.network.chat.Component.empty());
		this.xSize = xSize;
		this.ySize = ySize;
		xCenterOffset = xOffset;
		yCenterOffset = yOffset;
	}

	@Override
	public void init() {
		super.init();
		guiLeft = width / 2 - xSize / 2 + xCenterOffset;
		guiTop = height / 2 - ySize / 2 + yCenterOffset;

		right = width / 2 + xSize / 2 + xCenterOffset;
		bottom = height / 2 + ySize / 2 + yCenterOffset;

		xCenter = (right + guiLeft) / 2;
		yCenter = (bottom + guiTop) / 2;
	}


	public void register(ISubGuiControler gui) {
		controler = gui;
	}

	public void exitGui() {
		controler.resetSubGui();
	}

	protected MouseButtonEvent mouseEvent(double x, double y, int button) {
		return new MouseButtonEvent(x, y, new MouseButtonInfo(button, inputModifiers));
	}

	@Override
	public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		inputModifiers = event.modifiers();
		inputDoubleClick = doubleClick;
		return mouseClicked(event.x(), event.y(), event.button());
	}

	@Override
	public final boolean mouseReleased(MouseButtonEvent event) {
		inputModifiers = event.modifiers();
		return mouseReleased(event.x(), event.y(), event.button());
	}

	@Override
	public final boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		inputModifiers = event.modifiers();
		return mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
	}

	@Override
	public final boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return mouseScrolled(mouseX, mouseY, scrollY);
	}

	@Override
	public final boolean keyPressed(KeyEvent event) {
		return keyPressed(event.key(), event.scancode(), event.modifiers());
	}

	@Override
	public final boolean charTyped(CharacterEvent event) {
		return charTyped((char) event.codepoint(), 0);
	}

	public boolean charTyped(char par1, int par2) {
		if (subGui != null) {
			return subGui.charTyped(par1, par2);
		}
		// Legacy 1.12 keyTyped port: keyCode 1 was ESC; kept for callers passing it through
		if (par2 == 1) {
			exitGui();
		}
		return false;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (subGui != null) {
			return subGui.keyPressed(keyCode, scanCode, modifiers);
		}
		if (keyCode == 256) { // GLFW_KEY_ESCAPE: close only this popup, not the whole GUI
			exitGui();
			return true;
		}
		return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (subGui != null) {
			return subGui.mouseClicked(mouseX, mouseY, button);
		}
		return super.mouseClicked(mouseEvent(mouseX, mouseY, button), inputDoubleClick);
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (subGui != null) {
			return subGui.mouseReleased(mouseX, mouseY, button);
		}
		return super.mouseReleased(mouseEvent(mouseX, mouseY, button));
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (subGui != null) {
			return subGui.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		return super.mouseDragged(mouseEvent(mouseX, mouseY, button), dragX, dragY);
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (subGui != null) {
			return subGui.mouseScrolled(mouseX, mouseY, delta);
		}
		return super.mouseScrolled(mouseX, mouseY, 0, delta);
	}

	@Override
	public net.minecraft.client.gui.GuiGraphicsExtractor getGuiGraphics() {
		return storedGuiGraphics;
	}

	@Override
	public final void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.storedGuiGraphics = guiGraphics;
		SimpleGraphics.guiGraphics = guiGraphics;
		renderGuiBackground(mouseX, mouseY);
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		this.renderLabels(guiGraphics, mouseX, mouseY);
		if (subGui != null) {
			guiGraphics.nextStratum();
			if (!subGui.hasSubGui()) {
				extractTransparentBackground(guiGraphics);
			}
			subGui.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		}
		renderToolTips(mouseX, mouseY, partialTicks);
	}

	protected void renderToolTips(int mouseX, int mouseY, float par3) {}

	protected void renderLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {}

	protected abstract void renderGuiBackground(int mouseX, int mouseY);

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (subGui != null) {
			subGui.resize(width, height);
		}
	}

	@Override
	public void resetSubGui() {
		subGui = null;
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
			subGui.init(width, height);
		}
	}

	@Override
	public LogisticsBaseGuiScreen getBaseScreen() {
		return controler.getBaseScreen();
	}

	@Override
	public Minecraft getMC() {
		return minecraft;
	}
}
