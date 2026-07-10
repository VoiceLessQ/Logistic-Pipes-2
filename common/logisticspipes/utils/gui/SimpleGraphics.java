/*
 * Copyright (c) 2015  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/mc16/LICENSE.md
 */

package logisticspipes.utils.gui;

import net.minecraft.client.gui.Font;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;



import logisticspipes.utils.Color;

/**
 * Utils class for simple drawing methods.
 */
@SuppressWarnings("JavadocReference")
@OnlyIn(Dist.CLIENT)
public final class SimpleGraphics {

	public static net.minecraft.client.gui.GuiGraphics guiGraphics = null;

	private SimpleGraphics() {}

	/**
	 * Takes colors as enum values from {@link logisticspipes.utils.Color}.
	 *
	 * @see #drawHorizontalLine(int, int, int, int, int)
	 */
	public static void drawHorizontalLine(int x1, int x2, int y, Color color, int thickness) {
		SimpleGraphics.drawHorizontalLine(x1, x2, y, Color.getValue(color), thickness);
	}

	/**
	 * Draws a horizontal line from x1 to x2.
	 *
	 * @param x1        the start coordinate
	 * @param x2        the end coordinate
	 * @param y         the y-coordinate the line is on
	 * @param color     the color, which the line will have
	 * @param thickness the thickness, which the line will have
	 * @see net.minecraft.client.gui.Gui#drawHorizontalLine(int, int, int, int)
	 */
	public static void drawHorizontalLine(int x1, int x2, int y, int color, int thickness) {
		if (x2 < x1) {
			int temp = x1;
			x1 = x2;
			x2 = temp;
		}

		guiGraphics.fill(x1, y, x2 + 1, y + thickness, color);
	}

	/**
	 * Takes colors as enum values from {@link logisticspipes.utils.Color}.
	 *
	 * @see #drawVerticalLine(int, int, int, int, int)
	 */
	public static void drawVerticalLine(int x, int y1, int y2, Color color, int thickness) {
		SimpleGraphics.drawVerticalLine(x, y1, y2, Color.getValue(color), thickness);
	}

	/**
	 * Draws a vertical line from y1 to y2.
	 *
	 * @param x         the x-coordinate the line is on
	 * @param y1        the start coordinate
	 * @param y2        the end coordinate
	 * @param color     the color, which the line will have
	 * @param thickness the thickness, which the line will have
	 * @see net.minecraft.client.gui.Gui#drawVerticalLine(int, int, int, int)
	 */
	public static void drawVerticalLine(int x, int y1, int y2, int color, int thickness) {
		if (y2 < y1) {
			int temp = y1;
			y1 = y2;
			y2 = temp;
		}

		guiGraphics.fill(x, y1 + 1, x + thickness, y2, color);
	}

	/**
	 * Takes colors as enum values from {@link logisticspipes.utils.Color}.
	 *
	 * @see #drawRectNoBlend(int, int, int, int, int, double)
	 */
	public static void drawRectNoBlend(int x1, int y1, int x2, int y2, Color color, double zLevel) {
		SimpleGraphics.drawRectNoBlend(x1, y1, x2, y2, Color.getValue(color), zLevel);
	}

	/**
	 * Draws a solid color rectangle with the specified coordinates and color.
	 * This variation does not use GL_BLEND.
	 *
	 * @param x1     the first x-coordinate of the rectangle
	 * @param y1     the first y-coordinate of the rectangle
	 * @param x2     the second x-coordinate of the rectangle
	 * @param y2     the second y-coordinate of the rectangle
	 * @param color  the color of the rectangle
	 * @param zLevel the z-level of the graphic
	 * @see net.minecraft.client.gui.Gui#guiGraphics.fill(int, int, int, int, int)
	 */
	public static void drawRectNoBlend(int x1, int y1, int x2, int y2, int color, double zLevel) {
		int temp;

		if (x1 < x2) {
			temp = x1;
			x1 = x2;
			x2 = temp;
		}

		if (y1 < y2) {
			temp = y1;
			y1 = y2;
			y2 = temp;
		}

		guiGraphics.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), color);
	}

	/**
	 * Takes colors as enum values from {@link logisticspipes.utils.Color}.
	 *
	 * @see #drawGradientRect(int, int, int, int, int, int, double)
	 */
	public static void drawGradientRect(int x1, int y1, int x2, int y2, Color colorA, Color colorB, double zLevel) {
		SimpleGraphics.drawGradientRect(x1, y1, x2, y2, Color.getValue(colorA), Color.getValue(colorB), zLevel);
	}

	/**
	 * Draws a rectangle with a vertical gradient between the specified colors.
	 *
	 * @param x1     the first x-coordinate of the rectangle
	 * @param y1     the first y-coordinate of the rectangle
	 * @param x2     the second x-coordinate of the rectangle
	 * @param y2     the second y-coordinate of the rectangle
	 * @param colorA the first color, starting from y1
	 * @param colorB the second color, ending in y2
	 * @param zLevel the z-level of the graphic
	 * @see net.minecraft.client.gui.Gui#drawGradientRect(int, int, int, int, int, int)
	 */
	public static void drawGradientRect(int x1, int y1, int x2, int y2, int colorA, int colorB, double zLevel) {
		guiGraphics.fillGradient(x1, y1, x2, y2, colorA, colorB);
	}

	/**
	 * Draws a textured rectangle.
	 *
	 * @param x      the x-coordinate of the rectangle
	 * @param y      the y-coordinate of the rectangle
	 * @param u      the u-coordinate of the texture
	 * @param v      the v-coordinate of the texture
	 * @param width  the width of the rectangle
	 * @param height the height of the rectangle
	 * @param zLevel the z-level of the graphic
	 * @see net.minecraft.client.gui.Gui#drawTexturedModalRect(int, int, int, int, int, int)
	 */
	/**
	 * @deprecated 1.12.2 signature — callers must migrate to
	 *   {@code guiGraphics.blit(ResourceLocation, x, y, u, v, w, h)}.
	 *   Kept as a no-op so legacy call sites still compile until migrated.
	 */
	@Deprecated
	public static void drawTexturedModalRect(int x, int y, int u, int v, int width, int height, double zLevel) {
		// no-op: texture binding is callsite-specific in 1.20.1; see GuiGraphics.blit
	}

	/**
	 * Draws the specified string with a z-translated drop shadow.
	 *
	 * @param font the font renderer to render the string with
	 * @param s            the string to render
	 * @param x            the x-coordinate of the string
	 * @param y            the y-coordinate of the string
	 * @param color        the color of the string
	 * @return the stop x-coordinate of the drawn string
	 */
	public static int drawStringWithTranslatedShadow(Font font, String s, int x, int y, int color) {
		int endX;

		// make color gray-ish and draw shadow
		int grayColor = (color & 16579836) >> 2 | color & -16777216;
		endX = guiGraphics.drawString(font, s, x + 1, y + 1, grayColor, false);

		// move to foreground and draw actual string
		endX = Math.max(endX, guiGraphics.drawString(font, s, x, y, color, false));

		return endX;
	}

	/**
	 * Takes colors as enum values from {@link logisticspipes.utils.Color}.
	 *
	 * @see #drawQuad(Object, int, int, int, int, int, double)
	 */
	public static void drawQuad(Object tessellator, int x, int y, int width, int height, Color color, double zLevel) {
		SimpleGraphics.drawQuad(tessellator, x, y, width, height, Color.getValue(color), zLevel);
	}

	/**
	 * Draws a solid-color rectangle. The {@code tessellator} parameter is kept for
	 * source compatibility with 1.12.2 call sites and is ignored; the rectangle is
	 * painted via {@link net.minecraft.client.gui.GuiGraphics#fill}.
	 */
	public static void drawQuad(Object tessellator, int x, int y, int width, int height, int color, double zLevel) {
		guiGraphics.fill(x, y, x + width, y + height, color);
	}
}
