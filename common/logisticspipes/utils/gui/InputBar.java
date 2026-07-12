package logisticspipes.utils.gui;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class InputBar extends EditBox implements LogisticsBaseGuiScreen.EventListener {

	public enum Align {
		LEFT,
		CENTER,
		RIGHT
	}

	public int minNumber = 0;

	public InputBar(Font font, LogisticsBaseGuiScreen screen, int left, int top, int width, int height) {
		this(font, screen, left, top, width, height, true);
	}

	public InputBar(Font font, LogisticsBaseGuiScreen screen, int left, int top, int width, int height, boolean isActive) {
		this(font, screen, left, top, width, height, isActive, false);
	}

	public InputBar(Font font, LogisticsBaseGuiScreen screen, int left, int top, int width, int height, boolean isActive, boolean numberOnly) {
		this(font, screen, left, top, width, height, isActive, numberOnly, Align.LEFT);
	}

	public InputBar(Font font, LogisticsBaseGuiScreen screen, int left, int top, int width, int height, boolean isActive, boolean numberOnly, Align align) {
		super(font, left+2, top, width-4, height-2, Component.empty());
		screen.onGuiEvents.add(this);
		if (numberOnly) {
			setFilter((String s) -> {
				try {
					return Integer.parseInt(s) >= minNumber;
				} catch (NumberFormatException ignored) {
					return false;
				}
			});
			setMaxLength(5);
		} else
			setMaxLength(128);
	}

	public void reposition(int left, int top, int width, int height) {
		setX(left+2);
		setY(top);
		setWidth(width-4);
		// height set at construction
	}

	@Override
	public void onUpdateScreen() {
		// EditBox.tick() removed in 1.20.2; cursor blink is time-based now.
	}

	@Override
	public boolean onKeyboardInput() {
		return (isFocused() || Screen.hasAltDown()) && net.minecraft.util.StringUtil.isAllowedChatCharacter(' ');
	}

	/**
	 * @return Boolean, true if click was handled.
	 */
	public boolean handleClick(double x, double y, int k) {
		if (k == 1 && x >= getX() && x < getX() + width && y >= getY() && y < getY() + height)
			setValue("");
		return mouseClicked(x, y, k);
	}

	/**
	 * @return Boolean, true if key was handled.
	 */
	public boolean handleKey(char c, int i) {
		return charTyped(c, 0); // was: textboxKeyTyped(c, i) in 1.12.2
	}

	public void setInteger(int newValue) {
		setValue(Integer.toString(Math.max(minNumber, newValue)));
	}

	public int getInteger() {
		try {
			return Math.max(minNumber, Integer.parseInt(getValue()));
		} catch (NumberFormatException ignored) {
			return minNumber;
		}
	}

	public boolean isEmpty() {
		return getValue().isEmpty();
	}

	/** @deprecated Use getInteger() */
	public int getInt() {
		return getInteger();
	}

	/** @deprecated Use setInteger() */
	public void putInt(int value) {
		setInteger(value);
	}

	/** Backward compat wrapper for getValue() */
	public String getText() {
		return getValue();
	}

	/** Backward compat wrapper for setValue() */
	public void setText(String text) {
		setValue(text);
	}

	/** @deprecated Use renderWidget() via parent */
	public void drawTextBox() {
		// no-op: rendering handled by addRenderableWidget in 1.20.1
	}

}
