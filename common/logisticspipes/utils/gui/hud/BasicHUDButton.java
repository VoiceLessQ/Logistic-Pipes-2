package logisticspipes.utils.gui.hud;





import net.minecraft.client.Minecraft;
import logisticspipes.interfaces.IHUDButton;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SimpleGraphics;

public abstract class BasicHUDButton implements IHUDButton {

	protected final int posX;
	protected final int posY;
	protected final int sizeX;
	protected final int sizeY;
	protected long focusedTimeStart = 0;
	protected final String label;

	public BasicHUDButton(String name, int x, int y, int width, int heigth) {
		label = name;
		posX = x;
		posY = y;
		sizeX = width;
		sizeY = heigth;
	}

	@Override
	public int getX() {
		return posX;
	}

	@Override
	public int getY() {
		return posY;
	}

	@Override
	public int sizeX() {
		return sizeX;
	}

	@Override
	public int sizeY() {
		return sizeY;
	}

	@Override
	public void blockFocused() {
		focusedTimeStart = -1;
	}

	@Override
	public boolean isblockFocused() {
		return focusedTimeStart == -1;
	}

	@Override
	public void setFocused() {
		focusedTimeStart = System.currentTimeMillis();
	}

	@Override
	public boolean isFocused() {
		return focusedTimeStart != 0;
	}

	@Override
	public void clearFocused() {
		focusedTimeStart = 0;
	}

	@Override
	public int focusedTime() {
		return (int) (System.currentTimeMillis() - focusedTimeStart);
	}

	@Override
	public void renderButton(boolean hover, boolean clicked, boolean shifted) {
		net.minecraft.client.gui.GuiGraphicsExtractor gg = SimpleGraphics.guiGraphics;
		if (gg == null) return;
		int bg = clicked ? 0xaa333333 : hover ? 0xaa555555 : 0xaa444444;
		gg.fill(posX, posY, posX + sizeX, posY + sizeY, bg);
		gg.fill(posX, posY, posX + sizeX, posY + 1, 0xffaaaaaa);
		gg.fill(posX, posY + sizeY - 1, posX + sizeX, posY + sizeY, 0xff333333);
		SimpleGraphics.drawCenteredString(gg, net.minecraft.client.Minecraft.getInstance().font, label,
			posX + sizeX / 2, posY + (sizeY - 8) / 2, Color.getValue(Color.LIGHTER_GREY));
	}

	@Override
	public void renderAlways(boolean shifted) {

	}
}
