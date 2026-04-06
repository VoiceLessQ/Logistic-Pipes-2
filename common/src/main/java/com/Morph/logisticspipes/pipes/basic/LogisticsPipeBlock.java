package com.Morph.logisticspipes.pipes.basic;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.Morph.logisticspipes.LPBlocks;
import com.Morph.logisticspipes.LPConstants;

/**
 * The pipe block. Handles connection state and voxel shape.
 * Ported from LP 1.12.2 LogisticsBlockGenericPipe — Phase 2 core only.
 *
 * Connection state stored as 6 BooleanProperty (one per Direction),
 * matching the original LP pipe connection system.
 */
public class LogisticsPipeBlock extends BaseEntityBlock {

    // One property per direction — true = pipe arm extends in that direction
    public static final BooleanProperty CONNECTED_DOWN  = BooleanProperty.create("connected_down");
    public static final BooleanProperty CONNECTED_UP    = BooleanProperty.create("connected_up");
    public static final BooleanProperty CONNECTED_NORTH = BooleanProperty.create("connected_north");
    public static final BooleanProperty CONNECTED_SOUTH = BooleanProperty.create("connected_south");
    public static final BooleanProperty CONNECTED_WEST  = BooleanProperty.create("connected_west");
    public static final BooleanProperty CONNECTED_EAST  = BooleanProperty.create("connected_east");

    public static final Map<Direction, BooleanProperty> CONNECTED = new EnumMap<>(Map.of(
            Direction.DOWN,  CONNECTED_DOWN,
            Direction.UP,    CONNECTED_UP,
            Direction.NORTH, CONNECTED_NORTH,
            Direction.SOUTH, CONNECTED_SOUTH,
            Direction.WEST,  CONNECTED_WEST,
            Direction.EAST,  CONNECTED_EAST
    ));

    // VoxelShapes — center cube + arm per direction
    private static final double MIN = LPConstants.PIPE_MIN; // 4
    private static final double MAX = LPConstants.PIPE_MAX; // 12

    private static final VoxelShape CENTER = Block.box(MIN, MIN, MIN, MAX, MAX, MAX);

    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Map.of(
            Direction.DOWN,  Block.box(MIN, 0,   MIN, MAX, MIN, MAX),
            Direction.UP,    Block.box(MIN, MAX, MIN, MAX, 16,  MAX),
            Direction.NORTH, Block.box(MIN, MIN, 0,   MAX, MAX, MIN),
            Direction.SOUTH, Block.box(MIN, MIN, MAX, MAX, MAX, 16),
            Direction.WEST,  Block.box(0,   MIN, MIN, MIN, MAX, MAX),
            Direction.EAST,  Block.box(MAX, MIN, MIN, 16,  MAX, MAX)
    ));

    public LogisticsPipeBlock(Properties props) {
        super(props);
        BlockState def = this.stateDefinition.any();
        for (BooleanProperty prop : CONNECTED.values()) {
            def = def.setValue(prop, false);
        }
        this.registerDefaultState(def);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED_DOWN, CONNECTED_UP, CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_WEST, CONNECTED_EAST);
    }

    // -------------------------------------------------------------------------
    // Shape
    // -------------------------------------------------------------------------

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = CENTER;
        for (Direction dir : Direction.values()) {
            if (state.getValue(CONNECTED.get(dir))) {
                shape = Shapes.or(shape, ARMS.get(dir));
            }
        }
        return shape;
    }

    // -------------------------------------------------------------------------
    // Block entity
    // -------------------------------------------------------------------------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, LPBlocks.PIPE_BLOCK_ENTITY.get(), LogisticsPipeBlockEntity::serverTick);
    }

    // -------------------------------------------------------------------------
    // Neighbor change — update connections
    // -------------------------------------------------------------------------

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LogisticsPipeBlockEntity pipe) {
                pipe.updateConnections();
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LogisticsPipeBlockEntity pipe) {
                pipe.updateConnections();
            }
        }
    }
}
