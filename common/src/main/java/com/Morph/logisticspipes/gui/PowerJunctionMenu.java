package com.Morph.logisticspipes.gui;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.Morph.logisticspipes.LPMenuTypes;
import com.Morph.logisticspipes.blocks.power.LogisticsPowerJunctionBlockEntity;

/**
 * Menu for the Power Junction block. No inventory slots — three synced ints
 * (stored LP, stored RF, max RF) drive the client-side energy bar.
 *
 * Values are refreshed each {@link #broadcastChanges()} call so the screen
 * stays live as cables push RF in or pipes draw power out.
 */
public class PowerJunctionMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;
    @Nullable private final Level level;
    private final DataSlot storedLp = DataSlot.standalone();
    private final DataSlot storedRf = DataSlot.standalone();
    private final DataSlot maxRf    = DataSlot.standalone();

    /** Server-side constructor. */
    public PowerJunctionMenu(int containerId, Inventory inv, BlockPos pos) {
        super(LPMenuTypes.POWER_JUNCTION.get(), containerId);
        this.blockPos = pos;
        this.level = inv.player.level();
        addDataSlot(storedLp);
        addDataSlot(storedRf);
        addDataSlot(maxRf);
        refreshFromBlockEntity();
    }

    /** Client-side constructor — reads the BlockPos written by the ExtendedMenuProvider. */
    public PowerJunctionMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, buf.readBlockPos());
    }

    public BlockPos getBlockPos() { return blockPos; }
    public int getStoredLp()      { return storedLp.get(); }
    public int getStoredRf()      { return storedRf.get(); }
    public int getMaxRf()         { return maxRf.get(); }

    private void refreshFromBlockEntity() {
        if (level == null) return;
        BlockEntity be = level.getBlockEntity(blockPos);
        if (be instanceof LogisticsPowerJunctionBlockEntity junction) {
            storedLp.set(junction.getPowerLevel());
            storedRf.set(junction.getEnergyStoredRf());
            maxRf.set(junction.getMaxEnergyStoredRf());
        }
    }

    @Override
    public void broadcastChanges() {
        refreshFromBlockEntity();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
