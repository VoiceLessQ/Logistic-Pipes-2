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
import com.Morph.logisticspipes.modules.ModuleRegistry;
import com.Morph.logisticspipes.pipes.PipeLogisticsChassis;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;

/**
 * Menu for chassis pipes. Shows N module slots + player inventory.
 */
public class ChassisPipeMenu extends AbstractContainerMenu {

    private static final int PLAYER_INV_START = 9;

    private final BlockPos pipePos;
    private final int chassisSize;
    private final Player player;

    /** Server-side constructor. */
    public ChassisPipeMenu(int containerId, Inventory playerInv, BlockPos pos) {
        super(LPMenuTypes.CHASSIS_PIPE.get(), containerId);
        this.pipePos = pos;
        this.player = playerInv.player;

        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        PipeLogisticsChassis chassis = (be instanceof LogisticsPipeBlockEntity lpbe
                && lpbe.getPipe() instanceof PipeLogisticsChassis c) ? c : null;

        this.chassisSize = chassis != null ? chassis.getChassisSize() : 0;

        if (chassis != null) {
            addModuleSlots(chassis, playerInv);
        }
        addPlayerInventorySlots(playerInv);
    }

    /** Client-side constructor. */
    public ChassisPipeMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        super(LPMenuTypes.CHASSIS_PIPE.get(), containerId);
        this.pipePos = buf.readBlockPos();
        this.chassisSize = buf.readInt();
        this.player = playerInv.player;
        // Client doesn't have the container reference; slots are display-only placeholders
        addPlayerInventorySlots(playerInv);
    }

    private void addModuleSlots(PipeLogisticsChassis chassis, Inventory playerInv) {
        int startX = 8;
        int startY = 18;
        for (int i = 0; i < chassis.getChassisSize(); i++) {
            final int slotIndex = i;
            addSlot(new Slot(chassis.moduleContainer, i, startX + i * 18, startY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ModuleRegistry.isModuleItem(stack.getItem());
                }
                @Override
                public int getMaxStackSize() { return 1; }
            });
        }
    }

    private void addPlayerInventorySlots(Inventory playerInv) {
        int startY = 84;
        // 3 rows of main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, PLAYER_INV_START + row * 9 + col,
                        8 + col * 18, startY + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, startY + 58));
        }
    }

    public BlockPos getPipePos() { return pipePos; }
    public int getChassisSize() { return chassisSize; }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // TODO: shift-click transfer
    }
}
