package com.Morph.logisticspipes.blocks.power;

import org.jetbrains.annotations.Nullable;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.Morph.logisticspipes.gui.PowerJunctionMenu;

public class LogisticsPowerJunctionBlock extends Block implements EntityBlock {

    public LogisticsPowerJunctionBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsPowerJunctionBlockEntity(pos, state);
    }

    /** Right-click opens the Power Junction menu. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LogisticsPowerJunctionBlockEntity)) return InteractionResult.PASS;

        MenuRegistry.openExtendedMenu(sp, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {
                buf.writeBlockPos(pos);
            }
            @Override
            public Component getDisplayName() { return Component.translatable("block.morph.power_junction"); }
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new PowerJunctionMenu(id, inv, pos);
            }
        });
        return InteractionResult.CONSUME;
    }
}
