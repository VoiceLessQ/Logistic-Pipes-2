package com.Morph.logisticspipes.blocks.power;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class LogisticsPowerJunctionBlock extends Block implements EntityBlock {

    public LogisticsPowerJunctionBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsPowerJunctionBlockEntity(pos, state);
    }

    /**
     * Right-clicking reports the stored LP and equivalent RF as an action-bar message.
     * Placeholder for the future GUI — verifies that the junction is actually accepting energy.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LogisticsPowerJunctionBlockEntity junction)) return InteractionResult.PASS;

        int lp = junction.getPowerLevel();
        int rf = junction.getEnergyStoredRf();
        int rfMax = junction.getMaxEnergyStoredRf();
        int pct = rfMax > 0 ? (int) ((long) rf * 100L / rfMax) : 0;
        player.displayClientMessage(
                Component.literal(String.format("Power Junction: %,d LP  (%,d / %,d RF — %d%%)",
                        lp, rf, rfMax, pct)),
                true);
        return InteractionResult.CONSUME;
    }
}
