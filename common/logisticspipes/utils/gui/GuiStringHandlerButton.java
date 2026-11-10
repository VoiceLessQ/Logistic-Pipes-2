package logisticspipes.utils.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class GuiStringHandlerButton extends Button.Plain {

	private final StringHandler handler;

	// par1 = legacy id (discarded), par2 = x, par3 = y
	public GuiStringHandlerButton(int par1, int par2, int par3, StringHandler handler) {
		super(par2, par3, 200, 20, Component.literal(handler.getContent()), b -> {}, DEFAULT_NARRATION);
		this.handler = handler;
	}

	// par1 = legacy id (discarded), par2 = x, par3 = y, with onPress callback
	public GuiStringHandlerButton(int par1, int par2, int par3, StringHandler handler, Runnable onPress) {
		super(par2, par3, 200, 20, Component.literal(handler.getContent()), b -> onPress.run(), DEFAULT_NARRATION);
		this.handler = handler;
	}

	// par1 = legacy id (discarded), par2 = x, par3 = y, par4 = width, par5 = height
	public GuiStringHandlerButton(int par1, int par2, int par3, int par4, int par5, StringHandler handler) {
		super(par2, par3, par4, par5, Component.literal(handler.getContent()), b -> {}, DEFAULT_NARRATION);
		this.handler = handler;
	}

	// par1 = legacy id (discarded), par2 = x, par3 = y, par4 = width, par5 = height, with onPress callback
	public GuiStringHandlerButton(int par1, int par2, int par3, int par4, int par5, StringHandler handler, Runnable onPress) {
		super(par2, par3, par4, par5, Component.literal(handler.getContent()), b -> onPress.run(), DEFAULT_NARRATION);
		this.handler = handler;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		setMessage(Component.literal(handler.getContent()));
		super.extractContents(guiGraphics, mouseX, mouseY, partialTick);
	}

	public interface StringHandler {

		String getContent();
	}

}
