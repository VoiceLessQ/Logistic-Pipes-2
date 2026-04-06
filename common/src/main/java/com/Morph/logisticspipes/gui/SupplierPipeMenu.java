package com.Morph.logisticspipes.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.Morph.logisticspipes.LPMenuTypes;
import com.Morph.logisticspipes.pipes.PipeItemsSupplierLogistics;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;

/**
 * Menu for the supplier pipe. Shows 9 supply-config slots + player inventory.
 * Each slot: place an item with a stack count to set the supply target for that item.
 */
public class SupplierPipeMenu extends AbstractContainerMenu {

    private static final int PLAYER_INV_START = 9;

    private final BlockPos pipePos;

    /** Server-side constructor. */
    public SupplierPipeMenu(int containerId, Inventory playerInv, BlockPos pos) {
        super(LPMenuTypes.SUPPLIER_PIPE.get(), containerId);
        this.pipePos = pos;

        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        PipeItemsSupplierLogistics supplier = (be instanceof LogisticsPipeBlockEntity lpbe
                && lpbe.getPipe() instanceof PipeItemsSupplierLogistics s) ? s : null;

        if (supplier != null) {
            addSupplySlots(supplier);
        }
        addPlayerInventorySlots(playerInv);
    }

    /** Client-side constructor. */
    public SupplierPipeMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        super(LPMenuTypes.SUPPLIER_PIPE.get(), containerId);
        this.pipePos = buf.readBlockPos();
        addPlayerInventorySlots(playerInv);
    }

    private void addSupplySlots(PipeItemsSupplierLogistics supplier) {
        int startX = 8;
        int startY = 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int i = row * 3 + col;
                addSlot(new Slot(supplier.supplyContainer, i,
                        startX + col * 18, startY + row * 18));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInv) {
        int startY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, PLAYER_INV_START + row * 9 + col,
                        8 + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, startY + 58));
        }
    }

    public BlockPos getPipePos() { return pipePos; }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
