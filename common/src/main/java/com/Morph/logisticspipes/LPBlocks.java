package com.Morph.logisticspipes;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlock;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;

public final class LPBlocks {

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(LPConstants.MOD_ID, Registries.BLOCK);

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(LPConstants.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<LogisticsPipeBlock> PIPE_BLOCK =
            BLOCKS.register("pipe", () -> new LogisticsPipeBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .sound(SoundType.METAL)
                            .strength(1.5f)
                            .noOcclusion()
            ));

    public static final RegistrySupplier<BlockEntityType<LogisticsPipeBlockEntity>> PIPE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("pipe", () ->
                    BlockEntityType.Builder.of(LogisticsPipeBlockEntity::new, PIPE_BLOCK.get()).build(null)
            );

    public static void register() {
        BLOCKS.register();
        BLOCK_ENTITIES.register();
    }
}
