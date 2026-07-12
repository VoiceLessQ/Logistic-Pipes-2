package logisticspipes.utils.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

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
	protected net.minecraft.client.gui.GuiGraphics storedGuiGraphics;

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

	@Override
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

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (subGui != null) {
			return subGui.keyPressed(keyCode, scanCode, modifiers);
		}
		if (keyCode == 256) { // GLFW_KEY_ESCAPE: close only this popup, not the whole GUI
			exitGui();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (subGui != null) {
			return subGui.mouseClicked(mouseX, mouseY, button);
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (subGui != null) {
			return subGui.mouseReleased(mouseX, mouseY, button);
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (subGui != null) {
			return subGui.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (subGui != null) {
			return subGui.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public net.minecraft.client.gui.GuiGraphics getGuiGraphics() {
		return storedGuiGraphics;
	}

	@Override
	public void renderBackground(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Background is drawn by renderGuiBackground() — suppress Screen's renderMenuBackground overlay
	}

	@Override
	public final void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.storedGuiGraphics = guiGraphics;
		SimpleGraphics.guiGraphics = guiGraphics;
		renderGuiBackground(mouseX, mouseY);
		RenderSystem.disableDepthTest();
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		this.renderLabels(guiGraphics, mouseX, mouseY);
		RenderSystem.enableDepthTest();
		if (subGui != null) {
			if (!subGui.hasSubGui()) {
				super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
			}
			subGui.render(guiGraphics, mouseX, mouseY, partialTicks);
		}
		renderToolTips(mouseX, mouseY, partialTicks);
	}

	protected void renderToolTips(int mouseX, int mouseY, float par3) {}

	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

	protected abstract void renderGuiBackground(int mouseX, int mouseY);

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
	public LogisticsBaseGuiScreen getBaseScreen() {
		return controler.getBaseScreen();
	}

	@Override
	public Minecraft getMC() {
		return minecraft;
	}
}
