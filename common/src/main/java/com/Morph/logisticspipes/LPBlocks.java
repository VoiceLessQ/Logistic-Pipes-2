package com.Morph.logisticspipes;

import java.util.function.Supplier;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import com.Morph.logisticspipes.blocks.power.LogisticsPowerJunctionBlock;
import com.Morph.logisticspipes.blocks.power.LogisticsPowerJunctionBlockEntity;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlock;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;
import com.Morph.logisticspipes.platform.Registrar;

public final class LPBlocks {

    private LPBlocks() {}

    public static Supplier<LogisticsPipeBlock> PIPE_BLOCK;
    public static Supplier<BlockEntityType<LogisticsPipeBlockEntity>> PIPE_BLOCK_ENTITY;

    public static Supplier<LogisticsPowerJunctionBlock> POWER_JUNCTION_BLOCK;
    public static Supplier<BlockEntityType<LogisticsPowerJunctionBlockEntity>> POWER_JUNCTION_BLOCK_ENTITY;

    public static void init(Registrar r) {
        PIPE_BLOCK = r.registerBlock("pipe", () -> new LogisticsPipeBlock(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .sound(SoundType.METAL)
                        .strength(1.5f)
                        .noOcclusion()));

        POWER_JUNCTION_BLOCK = r.registerBlock("power_junction", () -> new LogisticsPowerJunctionBlock(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .sound(SoundType.METAL)
                        .strength(2.0f)));

        PIPE_BLOCK_ENTITY = r.registerBlockEntity("pipe", () ->
                BlockEntityType.Builder.of(LogisticsPipeBlockEntity::new, PIPE_BLOCK.get()).build(null));

        POWER_JUNCTION_BLOCK_ENTITY = r.registerBlockEntity("power_junction", () ->
                BlockEntityType.Builder.of(LogisticsPowerJunctionBlockEntity::new, POWER_JUNCTION_BLOCK.get()).build(null));
    }
}
