package com.Morph.logisticspipes.pipes.basic;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.Morph.logisticspipes.LPBlocks;
import com.Morph.logisticspipes.transport.PipeTransportLogistics;

/**
 * Block entity for all LP pipe types.
 * Ported from LP 1.12.2 LogisticsTileGenericPipe — Phase 2 core only.
 *
 * Holds the active CoreUnroutedPipe instance and its transport layer.
 * Connection state is cached here and reflected in block state properties.
 */
public class LogisticsPipeBlockEntity extends BlockEntity {

    /** The active pipe logic for this block entity. Set after placement. */
    private CoreUnroutedPipe pipe;

    /** Cached connection state — updated on neighbor change. */
    private final Map<Direction, Boolean> connected = new EnumMap<>(Direction.class);

    public LogisticsPipeBlockEntity(BlockPos pos, BlockState state) {
        super(LPBlocks.PIPE_BLOCK_ENTITY.get(), pos, state);
        for (Direction dir : Direction.values()) {
            connected.put(dir, false);
        }
    }

    public void setPipe(CoreUnroutedPipe pipe) {
        this.pipe = pipe;
        pipe.setContainer(this);
    }

    public CoreUnroutedPipe getPipe() {
        return pipe;
    }

    public PipeTransportLogistics getTransport() {
        return pipe != null ? pipe.transport : null;
    }

    public boolean isConnected(Direction dir) {
        return Boolean.TRUE.equals(connected.get(dir));
    }

    /** Updates connection cache and block state. Called on neighbor change. */
    public void updateConnections() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        BlockPos pos = getBlockPos();
        BlockState state = getBlockState();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            boolean canConnect = pipe != null && pipe.canPipeConnect(neighbor, dir);
            connected.put(dir, canConnect);
            state = state.setValue(LogisticsPipeBlock.CONNECTED.get(dir), canConnect);
        }

        level.setBlock(pos, state, 3);
        setChanged();
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, LogisticsPipeBlockEntity be) {
        if (be.pipe != null) {
            be.pipe.updateEntity();
        }
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (pipe != null) {
            pipe.transport.save(tag);
            if (pipe instanceof CoreRoutedPipe crp) crp.saveRouterUUID(tag);
        }
        CompoundTag connTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            connTag.putBoolean(dir.getName(), isConnected(dir));
        }
        tag.put("connections", connTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (pipe != null) {
            pipe.transport.load(tag);
            if (pipe instanceof CoreRoutedPipe crp) crp.loadRouterUUID(tag);
        }
        if (tag.contains("connections")) {
            CompoundTag connTag = tag.getCompound("connections");
            for (Direction dir : Direction.values()) {
                connected.put(dir, connTag.getBoolean(dir.getName()));
            }
        }
    }
}
