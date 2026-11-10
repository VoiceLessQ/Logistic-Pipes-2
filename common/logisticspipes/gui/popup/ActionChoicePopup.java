package logisticspipes.gui.popup;

import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.client.Minecraft;


import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SubGuiScreen;

public class ActionChoicePopup extends SubGuiScreen {

	private final String message;
	private final String leftButton;
	private final Runnable leftAction;
	private final String rightButton;
	private final Runnable rightAction;
	private final boolean buttonMin;

	public ActionChoicePopup(String message, String leftButton, Runnable leftAction, String rightButton, Runnable rightAction) {
		super(100, 100, 0, 0);
		this.message = message;
		this.leftButton = leftButton;
		this.leftAction = leftAction;
		this.rightButton = rightButton;
		this.rightAction = rightAction;
		int sizeX = Minecraft.getInstance().font.width(message);
		int leftX = Minecraft.getInstance().font.width(leftButton);
		int rightX = Minecraft.getInstance().font.width(rightButton);
		this.xSize = Math.max(sizeX + 20, leftX + rightX + 70);
		this.ySize = 55;
		this.buttonMin = xSize == leftX + rightX + 70;
	}

	@Override
	public void init() {
		super.init();
		int lW = Minecraft.getInstance().font.width(leftButton) + 20;
		int rW = Minecraft.getInstance().font.width(rightButton) + 20;
		int lX, rX;
		if (buttonMin) {
			lX = guiLeft + 10;
			rX = guiLeft + Minecraft.getInstance().font.width(leftButton) + 40;
		} else {
			lX = guiLeft + (this.xSize / 4) - (lW / 2);
			rX = guiLeft + (this.xSize * 3 / 4) - (rW / 2);
		}
		logisticspipes.utils.gui.SmallGuiButton lBtn = new logisticspipes.utils.gui.SmallGuiButton(0, lX, guiTop + 25, lW, 20, leftButton);
		lBtn.setPressListener(b -> { leftAction.run(); exitGui(); });
		addRenderableWidget(lBtn);
		logisticspipes.utils.gui.SmallGuiButton rBtn = new logisticspipes.utils.gui.SmallGuiButton(1, rX, guiTop + 25, rW, 20, rightButton);
		rBtn.setPressListener(b -> { rightAction.run(); exitGui(); });
		addRenderableWidget(rBtn);
	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(minecraft, guiLeft, guiTop, right, bottom, 0.0f, true);
		SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, message, xCenter - minecraft.font.width(message) / 2, guiTop + 6, 0xFFFFFF, true);
	}

}
