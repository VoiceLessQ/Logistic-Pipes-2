/*
 * Copyright (c) 2015  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/mc16/LICENSE.md
 */

package logisticspipes.utils.gui;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;




import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.Color;

/**
 * Utils class for GUI-related drawing methods.
 */
@Environment(EnvType.CLIENT)
public final class LPGuiGraphics {

	public static final Identifier WIDGETS_TEXTURE = Identifier.parse("textures/gui/widgets.png");
	public static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/slot.png");
	public static final Identifier BIG_SLOT_TEXTURE = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/slot-big.png");
	public static final Identifier SMALL_SLOT_TEXTURE = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/slot-small.png");
	public static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/guibackground.png");
	public static final Identifier LOCK_ICON = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/lock.png");
	public static final Identifier LINES_ICON = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/lines.png");
	public static final Identifier STATS_ICON = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/stats.png");
	public static final Identifier SLOT_DISK_TEXTURE = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/slot_disk.png");
	public static final Identifier SLOT_PROGRAMMER_TEXTURE = Identifier.fromNamespaceAndPath("logisticspipes", "textures/gui/slot_programmer.png");
	public static float zLevel = 0.0F;

	private LPGuiGraphics() {}

	/**
	 * Draws the durability bar for GUI items.
	 *
	 * @param itemstack the itemstack, from which the durability bar should be drawn
	 * @param x         the x-coordinate for the bar
	 * @param y         the y-coordinate for the bar
	 * @param zLevel    the z-level for the bar
	 * TextureManager, ItemStack, int, int, String)
	 */
	public static void drawDurabilityBar(@Nonnull ItemStack itemstack, int x, int y, double zLevel) {
		if (itemstack.getItem().isBarVisible(itemstack)) {
			double health = itemstack.getMaxDamage() == 0 ? 0.0D : (double) itemstack.getDamageValue() / itemstack.getMaxDamage();
			int j1 = (int) Math.round(13.0D - health * 13.0D);
			int k = (int) Math.round(255.0D - health * 255.0D);
			int l = 255 - k << 16 | k << 8 | 255 << 24;
			int i1 = (255 - k) / 4 << 16 | 16128 | 255 << 24;
			SimpleGraphics.guiGraphics.fill(x + 2, y + 15, x + 15, y + 17, Color.BLACK.getValue());
			SimpleGraphics.guiGraphics.fill(x + 2, y + 15, x + 14, y + 16, i1);
			SimpleGraphics.guiGraphics.fill(x + 2, y + 15, x + 2 + j1, y + 16, l);
		}
	}

	@SuppressWarnings("unchecked")
	public static void displayItemToolTip(Object[] tooltip, float pzLevel, int guiLeft, int guiTop, boolean forceAdd) {
		if (tooltip == null) {
			return;
		}

		LPGuiGraphics.zLevel = pzLevel;

		Minecraft mc = Minecraft.getInstance();
		ItemStack stack = (ItemStack) tooltip[2];
		if (stack == null) stack = ItemStack.EMPTY;

		List<String> tooltipLines;
		if (mc.gui.screen() instanceof AbstractContainerScreen) {
			// NEI is not available on 1.20.1 — the former dummy proxy always returned an empty mutable list.
			tooltipLines = new java.util.ArrayList<>();
		} else {
			tooltipLines = java.util.Collections.emptyList();
		}

		if (tooltip.length > 4) {
			tooltipLines.addAll(1, (List<String>) tooltip[4]);
		}

		if ((Minecraft.getInstance().hasControlDown()) && (tooltip.length < 4 || (Boolean) tooltip[3])) {
			tooltipLines.add(1, "\u00a77" + ((ItemStack) tooltip[2]).getCount());
		}

		int x = (Integer) tooltip[0] - (forceAdd ? 0 : guiLeft) + 12;
		int y = (Integer) tooltip[1] - (forceAdd ? 0 : guiTop) - 12;
		// NEI render hook removed (former dummy always returned false) — always draw our own tooltip.
		LPGuiGraphics.drawToolTip(x, y, tooltipLines, stack.getRarity().color());

		LPGuiGraphics.zLevel = 0;
	}

	public static void drawToolTip(int posX, int posY, List<String> msg, ChatFormatting rarityColor) {
		if (msg.isEmpty()) {
			return;
		}

		// use vanilla Minecraft code
		int boxWidth = 0;

		for (String str : msg) {
			int width = Minecraft.getInstance().font.width(str);

			if (width > boxWidth) {
				boxWidth = width;
			}
		}

		int x = posX + 12;
		int y = posY - 12;
		int yHeight = 8;

		if (msg.size() > 1) {
			yHeight += 2 + (msg.size() - 1) * 10;
		}


		LPGuiGraphics.zLevel = 300.0F;
		int bgColor = 0xf0100010;
		int frameColor1 = 0x505000ff;
		int frameColor2 = (frameColor1 & 0xfefefe) >> 1 | frameColor1 & 0xff000000;

		SimpleGraphics.drawGradientRect(x - 3, y - 4, x + boxWidth + 3, y - 3, bgColor, bgColor, 0.0);
		SimpleGraphics.drawGradientRect(x - 3, y + yHeight + 3, x + boxWidth + 3, y + yHeight + 4, bgColor, bgColor, 0.0);
		SimpleGraphics.drawGradientRect(x - 3, y - 3, x + boxWidth + 3, y + yHeight + 3, bgColor, bgColor, 0.0);
		SimpleGraphics.drawGradientRect(x - 4, y - 3, x - 3, y + yHeight + 3, bgColor, bgColor, 0.0);
		SimpleGraphics.drawGradientRect(x + boxWidth + 3, y - 3, x + boxWidth + 4, y + yHeight + 3, bgColor, bgColor, 0.0);
		SimpleGraphics.drawGradientRect(x - 3, y - 3 + 1, x - 3 + 1, y + yHeight + 3 - 1, frameColor1, frameColor2, 0.0);
		SimpleGraphics.drawGradientRect(x + boxWidth + 2, y - 3 + 1, x + boxWidth + 3, y + yHeight + 3 - 1, frameColor1, frameColor2, 0.0);
		SimpleGraphics.drawGradientRect(x - 3, y - 3, x + boxWidth + 3, y - 3 + 1, frameColor1, frameColor1, 0.0);
		SimpleGraphics.drawGradientRect(x - 3, y + yHeight + 2, x + boxWidth + 3, y + yHeight + 3, frameColor2, frameColor2, 0.0);

		for (int i = 0; i < msg.size(); ++i) {
			String line = msg.get(i);

			if (i == 0) {
				line = rarityColor.toString() + line;
			} else {
				line = "\u00a77" + line;
			}

			SimpleGraphics.drawString(SimpleGraphics.guiGraphics, Minecraft.getInstance().font, line, x, y, 0xFFFFFF | -16777216, false);

			if (i == 0) {
				y += 2;
			}

			y += 10;
		}

		LPGuiGraphics.zLevel = 0.0F;

	}

	public static void drawPlayerInventoryBackground(Minecraft mc, int xOffset, int yOffset) {
		//Player "backpack"
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				LPGuiGraphics.drawSlotBackground(mc, xOffset + column * 18, yOffset + row * 18);
			}
		}
		//Player "hotbar"
		for (int i1 = 0; i1 < 9; i1++) {
			LPGuiGraphics.drawSlotBackground(mc, xOffset + i1 * 18, yOffset + 58);
		}
	}

	public static void drawPlayerHotbarBackground(Minecraft mc, int xOffset, int yOffset) {
		//Player "hotbar"
		for (int i1 = 0; i1 < 9; i1++) {
			LPGuiGraphics.drawSlotBackground(mc, xOffset + i1 * 18, yOffset);
		}
	}

	public static void drawPlayerArmorBackground(Minecraft mc, int xOffset, int yOffset) {
		//Player "armor"
		for (int i1 = 0; i1 < 4; i1++) {
			LPGuiGraphics.drawSlotBackground(mc, xOffset, yOffset - i1 * 18);
		}
	}

	private static void doDrawSlotBackground(Minecraft mc, int x, int y, Identifier slotDiskTexture) {
		LPGuiGraphics.zLevel = 0;
		SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		GuiGraphicsExtractor gg = SimpleGraphics.guiGraphics;
		gg.blit(RenderPipelines.GUI_TEXTURED, slotDiskTexture, x, y, 0.0f, 0.0f, 18, 18, 18, 18, SimpleGraphics.color);
		// 1-pixel darker inset border so the slot visually separates from the panel on light backgrounds.
		final int borderColor = 0x80373737;
		gg.fill(x,      y,      x + 18, y + 1,  borderColor);
		gg.fill(x,      y + 17, x + 18, y + 18, borderColor);
		gg.fill(x,      y,      x + 1,  y + 18, borderColor);
		gg.fill(x + 17, y,      x + 18, y + 18, borderColor);
	}

	public static void drawSlotDiskBackground(Minecraft mc, int x, int y) {
		doDrawSlotBackground(mc, x, y, LPGuiGraphics.SLOT_DISK_TEXTURE);
	}

	public static void drawSlotProgrammerBackground(Minecraft mc, int x, int y) {
		doDrawSlotBackground(mc, x, y, LPGuiGraphics.SLOT_PROGRAMMER_TEXTURE);
	}

	public static void drawSlotBackground(Minecraft mc, int x, int y) {
		doDrawSlotBackground(mc, x, y, LPGuiGraphics.SLOT_TEXTURE);
	}

	public static void drawSlotBackground(Minecraft mc, int x, int y, int color) {
			doDrawSlotBackground(mc, x, y, LPGuiGraphics.SLOT_TEXTURE);
	}

	public static void drawBigSlotBackground(Minecraft mc, int x, int y) {
		LPGuiGraphics.zLevel = 0;
		SimpleGraphics.guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LPGuiGraphics.BIG_SLOT_TEXTURE, x, y, 0.0f, 0.0f, 26, 26, 26, 26, SimpleGraphics.color);
	}

	public static void drawSmallSlotBackground(Minecraft mc, int x, int y) {
		LPGuiGraphics.zLevel = 0;
		SimpleGraphics.guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LPGuiGraphics.SMALL_SLOT_TEXTURE, x, y, 0.0f, 0.0f, 8, 8, 8, 8, SimpleGraphics.color);
	}

	public static void renderIconAt(Minecraft mc, int x, int y, float zLevel, TextureAtlasSprite icon) {
		if (icon == null) {
			return;
		}
		SimpleGraphics.guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x, y, 16, 16, SimpleGraphics.color);
	}

	public static void drawLockBackground(Minecraft mc, int x, int y) {
		LPGuiGraphics.zLevel = 0;
		SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		SimpleGraphics.guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LPGuiGraphics.LOCK_ICON, x, y, 0.0f, 0.0f, 14, 15, 14, 15, SimpleGraphics.color);
	}

	private static void drawTexture16by16(Minecraft mc, int x, int y, Identifier tex) {
		LPGuiGraphics.zLevel = 0;
		SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		SimpleGraphics.guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, 16, 16, 16, 16, SimpleGraphics.color);
	}

	public static void drawLinesBackground(Minecraft mc, int x, int y) {
		drawTexture16by16(mc, x, y, LPGuiGraphics.LINES_ICON);
	}

	public static void drawStatsBackground(Minecraft mc, int x, int y) {
		drawTexture16by16(mc, x, y, LPGuiGraphics.STATS_ICON);
	}

	/** LP1's glColor4b HUD tint: day darkens to 64/127, alpha is 0..127 like the signed byte. */
	public static void setHudColor(boolean day, int alpha) {
		float c = day ? 64 / 127F : 1F;
		SimpleGraphics.setColor(c, c, c, alpha / 127F);
	}

	/** The removed GuiGraphics.blitRepeating: tiles the (srcX, srcY, srcW, srcH) region over the destination. */
	private static void blitRepeating(GuiGraphicsExtractor gg, Identifier tex, int x, int y, int w, int h, int srcX, int srcY, int srcW, int srcH, int texW, int texH) {
		for (int dx = 0; dx < w; dx += srcW) {
			int tw = Math.min(srcW, w - dx);
			for (int dy = 0; dy < h; dy += srcH) {
				int th = Math.min(srcH, h - dy);
				gg.blit(RenderPipelines.GUI_TEXTURED, tex, x + dx, y + dy, srcX, srcY, tw, th, texW, texH, SimpleGraphics.color);
			}
		}
	}

	public static void drawGuiBackGround(Minecraft mc, int guiLeft, int guiTop, int right, int bottom, float zLevel, boolean resetColor) {
		LPGuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop, right, bottom, zLevel, resetColor, true, true, true, true);
	}

	public static void drawGuiBackGround(Minecraft mc, int guiLeft, int guiTop, int right, int bottom, float zLevel, boolean resetColor, boolean displayTop, boolean displayLeft, boolean displayBottom, boolean displayRight) {
		if (resetColor) {
			SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
		GuiGraphicsExtractor gg = SimpleGraphics.guiGraphics;
		if (gg == null) return;
		final int panelW = right - guiLeft;
		final int panelH = bottom - guiTop;
		if (panelW <= 0 || panelH <= 0) return;

		// 9-slice the 45×45 background texture (15px borders).
		// blit(rl, x, y, u, v, w, h, texW, texH) — no stretching, src == dst size
		// blitRepeating(rl, dstX, dstY, dstW, dstH, srcX, srcY, srcW, srcH, texW, texH) — tiles
		final int BORDER = 15;
		final int TEX = 45;
		final int innerX = guiLeft + BORDER;
		final int innerY = guiTop  + BORDER;
		final int innerW = right  - BORDER - innerX;  // right-15 - (guiLeft+15)
		final int innerH = bottom - BORDER - innerY;  // bottom-15 - (guiTop+15)

		// Corners
		if (displayTop  && displayLeft)  gg.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, guiLeft,       guiTop,        0.0f,        0.0f,        BORDER, BORDER, TEX, TEX, SimpleGraphics.color);
		if (displayTop  && displayRight) gg.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, right - BORDER, guiTop,       30.0f,        0.0f,        BORDER, BORDER, TEX, TEX, SimpleGraphics.color);
		if (displayBottom && displayLeft)  gg.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, guiLeft,       bottom-BORDER, 0.0f,       30.0f,        BORDER, BORDER, TEX, TEX, SimpleGraphics.color);
		if (displayBottom && displayRight) gg.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, right - BORDER, bottom-BORDER,30.0f,      30.0f,        BORDER, BORDER, TEX, TEX, SimpleGraphics.color);

		// Edges (tiled)
		if (innerW > 0) {
			if (displayTop)    blitRepeating(gg, BACKGROUND_TEXTURE, innerX, guiTop,        innerW, BORDER, BORDER, 0,      BORDER, BORDER, TEX, TEX);
			if (displayBottom) blitRepeating(gg, BACKGROUND_TEXTURE, innerX, bottom-BORDER, innerW, BORDER, BORDER, 30,     BORDER, BORDER, TEX, TEX);
		}
		if (innerH > 0) {
			if (displayLeft)   blitRepeating(gg, BACKGROUND_TEXTURE, guiLeft,        innerY, BORDER, innerH, 0,  BORDER, BORDER, BORDER, TEX, TEX);
			if (displayRight)  blitRepeating(gg, BACKGROUND_TEXTURE, right - BORDER, innerY, BORDER, innerH, 30, BORDER, BORDER, BORDER, TEX, TEX);
		}

		// Center (always drawn)
		if (innerW > 0 && innerH > 0) {
			blitRepeating(gg, BACKGROUND_TEXTURE, innerX, innerY, innerW, innerH, BORDER, BORDER, BORDER, BORDER, TEX, TEX);
		}
	}
}
