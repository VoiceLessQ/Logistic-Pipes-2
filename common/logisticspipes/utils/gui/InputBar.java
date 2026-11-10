package logisticspipes.utils.gui;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;

public class InputBar extends EditBox implements LogisticsBaseGuiScreen.EventListener {

	public enum Align {
		LEFT,
		CENTER,
		RIGHT
	}

	public int minNumber = 0;
	private final LogisticsBaseGuiScreen screen;
	private Predicate<String> filter = s -> true;

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
		this.screen = screen;
		screen.onGuiEvents.add(this);
		if (numberOnly) {
			filter = (String s) -> {
				try {
					return Integer.parseInt(s) >= minNumber;
				} catch (NumberFormatException ignored) {
					return false;
				}
			};
			setMaxLength(5);
		} else
			setMaxLength(128);
	}

	// EditBox lost its filter in 26.x: undo an edit whose result the filter rejects
	@Override
	public void insertText(String input) {
		String old = getValue();
		int cursor = getCursorPosition();
		super.insertText(input);
		if (!filter.test(getValue())) {
			setValue(old);
			setCursorPosition(cursor);
		}
	}

	@Override
	public void deleteCharsToPos(int pos) {
		String old = getValue();
		int cursor = getCursorPosition();
		super.deleteCharsToPos(pos);
		if (!filter.test(getValue())) {
			setValue(old);
			setCursorPosition(cursor);
		}
	}

	public void reposition(int left, int top, int width, int height) {
		setX(left+2);
		setY(top);
		setWidth(width-4);
		// height set at construction
	}

	@Override
	public void onUpdateScreen() {
		// the cursor blinks by wall-clock time in 26.x, nothing to tick
	}

	@Override
	public boolean onKeyboardInput() {
		return (isFocused() || Minecraft.getInstance().hasAltDown()) && StringUtil.isAllowedChatCharacter(' ');
	}

	/**
	 * @return Boolean, true if click was handled.
	 */
	public boolean handleClick(double x, double y, int k) {
		boolean inside = x >= getX() && x < getX() + width && y >= getY() && y < getY() + height;
		if (k == 1 && inside)
			setValue("");
		// LP1 GuiTextField focused on click; EditBox leaves focus to the parent screen
		setFocused(inside);
		return mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(k, 0)), false);
	}

	/**
	 * @return Boolean, true if key was handled.
	 */
	public boolean handleKey(char c, int i) {
		return charTyped(new CharacterEvent(c));
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
		// LP1's GuiTextField drew itself here; no screen registers these as widgets
		extractRenderState(screen.guiGraphics, 0, 0, 0);
	}

}
