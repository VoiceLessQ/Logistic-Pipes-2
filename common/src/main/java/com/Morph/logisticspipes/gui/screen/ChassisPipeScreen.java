package com.Morph.logisticspipes.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.Morph.logisticspipes.gui.ChassisPipeMenu;

/**
 * Screen for chassis pipes. Shows module slots and player inventory.
 */
@Environment(EnvType.CLIENT)
public class ChassisPipeScreen extends AbstractContainerScreen<ChassisPipeMenu> {

    public ChassisPipeScreen(ChassisPipeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF_C6C6C6);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF_8B8B8B);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF_C6C6C6);

        // Module slot backgrounds
        int slots = menu.getChassisSize();
        for (int i = 0; i < slots; i++) {
            int sx = x + 8 + i * 18;
            int sy = y + 18;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF_373737);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF_8B8B8B);
        }

        // Player inventory slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 8 + col * 18;
                int sy = y + 84 + row * 18;
                graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF_373737);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF_8B8B8B);
            }
        }
        for (int col = 0; col < 9; col++) {
            int sx = x + 8 + col * 18;
            int sy = y + 142;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF_373737);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF_8B8B8B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "Chassis Pipe", 7, 6, 0x404040, false);
        graphics.drawString(font, "Inventory", 7, 72, 0x404040, false);
    }
}
