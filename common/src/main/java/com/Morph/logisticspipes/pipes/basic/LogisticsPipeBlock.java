package com.Morph.logisticspipes.pipes.basic;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;

import com.Morph.logisticspipes.LPBlocks;
import com.Morph.logisticspipes.LPConstants;
import com.Morph.logisticspipes.gui.ChassisPipeMenu;
import com.Morph.logisticspipes.gui.RequestPipeMenu;
import com.Morph.logisticspipes.gui.SupplierPipeMenu;
import com.Morph.logisticspipes.pipes.PipeItemsProviderLogistics;
import com.Morph.logisticspipes.pipes.PipeItemsRequestLogistics;
import com.Morph.logisticspipes.pipes.PipeItemsSupplierLogistics;
import com.Morph.logisticspipes.pipes.PipeLogisticsChassis;
import com.Morph.logisticspipes.pipes.PipeRegistry;

/**
 * The pipe block. Handles connection state and voxel shape.
 * Ported from LP 1.12.2 LogisticsBlockGenericPipe — Phase 2 core only.
 *
 * Connection state stored as 6 BooleanProperty (one per Direction),
 * matching the original LP pipe connection system.
 */
public class LogisticsPipeBlock extends BaseEntityBlock {

    public static final EnumProperty<PipeType> PIPE_TYPE =
            EnumProperty.create("pipe_type", PipeType.class);

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
        def = def.setValue(PIPE_TYPE, PipeType.BASIC);
        this.registerDefaultState(def);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PIPE_TYPE, CONNECTED_DOWN, CONNECTED_UP, CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_WEST, CONNECTED_EAST);
    }

    private static PipeType typeOf(CoreUnroutedPipe pipe) {
        if (pipe instanceof PipeItemsRequestLogistics)  return PipeType.REQUEST;
        if (pipe instanceof PipeItemsProviderLogistics) return PipeType.PROVIDER;
        if (pipe instanceof PipeItemsSupplierLogistics) return PipeType.SUPPLIER;
        if (pipe instanceof PipeLogisticsChassis)       return PipeType.CHASSIS;
        return PipeType.BASIC;
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
    public MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
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
    // Placement — wire the correct pipe type into the block entity
    // -------------------------------------------------------------------------

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LogisticsPipeBlockEntity lpbe) {
                CoreUnroutedPipe pipe = PipeRegistry.createFor(stack.getItem());
                if (pipe != null) {
                    lpbe.setPipe(pipe);
                    level.setBlock(pos, level.getBlockState(pos).setValue(PIPE_TYPE, typeOf(pipe)), 2);
                    lpbe.updateConnections();
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Right-click interaction — open pipe GUI
    // -------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LogisticsPipeBlockEntity lpbe)) return InteractionResult.PASS;
        CoreUnroutedPipe pipe = lpbe.getPipe();
        if (pipe == null) return InteractionResult.PASS;

        if (pipe instanceof PipeItemsRequestLogistics req) {
            MenuRegistry.openExtendedMenu(sp, new ExtendedMenuProvider() {
                @Override
                public void saveExtraData(FriendlyByteBuf buf) {
                    buf.writeBlockPos(pos);
                    RequestPipeMenu.writeItemsToBuf(buf, req.getNetworkItems());
                }
                @Override
                public Component getDisplayName() { return Component.literal("Request Pipe"); }
                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new RequestPipeMenu(id, inv, pos);
                }
            });
            return InteractionResult.CONSUME;
        }

        if (pipe instanceof PipeLogisticsChassis chassis) {
            MenuRegistry.openExtendedMenu(sp, new ExtendedMenuProvider() {
                @Override
                public void saveExtraData(FriendlyByteBuf buf) {
                    buf.writeBlockPos(pos);
                    buf.writeInt(chassis.getChassisSize());
                }
                @Override
                public Component getDisplayName() { return Component.literal("Chassis Pipe"); }
                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new ChassisPipeMenu(id, inv, pos);
                }
            });
            return InteractionResult.CONSUME;
        }

        if (pipe instanceof PipeItemsSupplierLogistics) {
            MenuRegistry.openExtendedMenu(sp, new ExtendedMenuProvider() {
                @Override
                public void saveExtraData(FriendlyByteBuf buf) { buf.writeBlockPos(pos); }
                @Override
                public Component getDisplayName() { return Component.literal("Supplier Pipe"); }
                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new SupplierPipeMenu(id, inv, pos);
                }
            });
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
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
