package com.Morph.logisticspipes.gui.screen;

import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.architectury.networking.NetworkManager;

import com.Morph.logisticspipes.gui.RequestPipeMenu;
import com.Morph.logisticspipes.network.LPNetworking;
import com.Morph.logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Screen for the request pipe. Shows available network items in a grid.
 * Click an item to request 1 stack; shift-click to request 1 item.
 */
@Environment(EnvType.CLIENT)
public class RequestPipeScreen extends AbstractContainerScreen<RequestPipeMenu> {

    private static final int COLS = 9;
    private static final int ROWS = 5;
    private static final int CELL = 18;

    public RequestPipeScreen(RequestPipeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = COLS * CELL + 14;
        imageHeight = ROWS * CELL + 17;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // Window background
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF_C6C6C6);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF_8B8B8B);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF_C6C6C6);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "Request Items", 7, 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        List<ItemIdentifierStack> items = menu.getNetworkItems();
        int x = leftPos + 7;
        int y = topPos + 17;
        for (int i = 0; i < items.size() && i < COLS * ROWS; i++) {
            ItemIdentifierStack entry = items.get(i);
            int col = i % COLS;
            int row = i / COLS;
            int px = x + col * CELL;
            int py = y + row * CELL;
            graphics.renderItem(entry.makeStack(), px, py);
            graphics.renderItemDecorations(font, entry.makeStack(), px, py,
                    entry.stackSize > 1 ? String.valueOf(entry.stackSize) : null);
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<ItemIdentifierStack> items = menu.getNetworkItems();
        int x = leftPos + 7;
        int y = topPos + 17;
        for (int i = 0; i < items.size() && i < COLS * ROWS; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int px = x + col * CELL;
            int py = y + row * CELL;
            if (mouseX >= px && mouseX < px + 16 && mouseY >= py && mouseY < py + 16) {
                ItemIdentifierStack entry = items.get(i);
                int amount = hasShiftDown() ? 1 : Math.min(entry.stackSize,
                        entry.item.item.getDefaultMaxStackSize());
                NetworkManager.sendToServer(LPNetworking.REQUEST_ITEM,
                        LPNetworking.buildRequestItemPacket(menu.getPipePos(), entry.item, amount));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
