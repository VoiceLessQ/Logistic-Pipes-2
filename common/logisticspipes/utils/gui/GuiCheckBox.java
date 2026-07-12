package logisticspipes.utils.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GuiCheckBox extends AbstractButton {

	/** Replaces the old Button.id field removed in 1.20.1 */
	public final int id;
	private boolean state;
	private java.util.function.Consumer<GuiCheckBox> pressListener = b -> {};

	public GuiCheckBox(int par1, int par2, int par3, int par4, int par5, boolean startState) {
		super(par2, par3, par4, par5, Component.empty());
		this.id = par1;
		state = startState;
	}

	public void setPressListener(java.util.function.Consumer<GuiCheckBox> listener) {
		this.pressListener = listener;
	}

	@Override
	public void onPress() {
		// State is owned by the press listener (they call change()/setState themselves);
		// toggling here too made every click a double flip, i.e. a no-op.
		pressListener.accept(this);
	}

	@Override
	public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
		if (visible) {
			boolean hover = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
			ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/gui/checkbox-" + (state ? "on" : "out") + (hover ? "-mouse" : "") + ".png");
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			guiGraphics.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
		}
	}

	@Override
	protected void updateWidgetNarration(@Nonnull NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}

	public boolean change() {
		return state = !state;
	}

	public boolean getState() {
		return state;
	}

	public void setState(boolean flag) {
		state = flag;
	}
}
