package com.Morph.logisticspipes.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.Morph.logisticspipes.gui.PowerJunctionMenu;

/**
 * Screen for the Power Junction. Renders a vertical fill bar driven by the
 * menu's three live DataSlots (LP, RF, RF cap) and prints the numeric values.
 *
 * No texture file — drawn procedurally with vanilla {@code graphics.fill}
 * for consistency with the other LP2 screens.
 */
@Environment(EnvType.CLIENT)
public class PowerJunctionScreen extends AbstractContainerScreen<PowerJunctionMenu> {

    // Bar geometry within the panel
    private static final int BAR_X = 80;
    private static final int BAR_Y = 24;
    private static final int BAR_W = 16;
    private static final int BAR_H = 100;

    // Colors
    private static final int FRAME_LIGHT = 0xFF_C6C6C6;
    private static final int FRAME_DARK  = 0xFF_8B8B8B;
    private static final int BAR_BG      = 0xFF_373737;
    private static final int BAR_FILL    = 0xFF_C82828; // RF red
    private static final int TEXT_DARK   = 0x404040;

    public PowerJunctionScreen(PowerJunctionMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 140;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // 3-layer panel border (matches SupplierPipeScreen style)
        g.fill(x,     y,     x + imageWidth,     y + imageHeight,     FRAME_LIGHT);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, FRAME_DARK);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, FRAME_LIGHT);

        // Bar frame + background
        int bx = x + BAR_X, by = y + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, BAR_BG);
        g.fill(bx,     by,     bx + BAR_W,     by + BAR_H,     0xFF_111111);

        // Bar fill (bottom-up)
        int max = Math.max(1, menu.getMaxRf());
        int filledH = (int) ((long) BAR_H * menu.getStoredRf() / max);
        if (filledH > 0) {
            g.fill(bx, by + BAR_H - filledH, bx + BAR_W, by + BAR_H, BAR_FILL);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, "Power Junction", 7, 6, TEXT_DARK, false);

        int lp    = menu.getStoredLp();
        int rf    = menu.getStoredRf();
        int rfMax = menu.getMaxRf();
        int pct   = rfMax > 0 ? (int) ((long) rf * 100L / rfMax) : 0;

        g.drawString(font, String.format("%,d LP", lp),         8, 26, TEXT_DARK, false);
        g.drawString(font, String.format("%,d RF", rf),         8, 40, TEXT_DARK, false);
        g.drawString(font, String.format("/ %,d", rfMax),       8, 52, TEXT_DARK, false);
        g.drawString(font, pct + "%",                            8, 70, TEXT_DARK, false);
    }
}
