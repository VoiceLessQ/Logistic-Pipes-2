package logisticspipes.pipes.basic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import logisticspipes.LPBlocks;
import logisticspipes.interfaces.ITickable;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsBlockGenericSubMultiBlock extends Block implements EntityBlock {

	public static boolean redirectedToMainPipe = false;

	public LogisticsBlockGenericSubMultiBlock() {
		super(BlockBehaviour.Properties.of().strength(0.5F).noOcclusion());
	}

	@Override
	@Nonnull
	public net.minecraft.world.level.block.RenderShape getRenderShape(@Nonnull BlockState state) {
		// Sub-multiblocks are invisible helpers; main pipe BER renders the visible geometry.
		return net.minecraft.world.level.block.RenderShape.INVISIBLE;
	}

	/** Fallback so the helper block stays targetable when no main pipe geometry is found. */
	private static final VoxelShape FALLBACK_SHAPE = Shapes.box(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

	@Override
	@Nonnull
	public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
		BlockEntity tile = world.getBlockEntity(pos);
		if (tile instanceof LogisticsTileGenericSubMultiBlock) {
			VoxelShape shape = Shapes.empty();
			for (LogisticsTileGenericPipe mainPipe : ((LogisticsTileGenericSubMultiBlock) tile).getConnectedMainPipes()) {
				if (mainPipe.isMultiBlock() && mainPipe.pipe instanceof CoreMultiBlockPipe) {
					shape = Shapes.or(shape, LogisticsBlockGenericPipe.getMultiBlockShape((CoreMultiBlockPipe) mainPipe.pipe, pos));
				}
			}
			if (!shape.isEmpty()) {
				return shape;
			}
		}
		return FALLBACK_SHAPE;
	}

	@Override
	@Nonnull
	public VoxelShape getCollisionShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
		return getShape(state, world, pos, context);
	}

	@Override
	@Nonnull
	public ItemStack getCloneItemStack(@Nonnull BlockState state, HitResult target, @Nonnull BlockGetter level, @Nonnull BlockPos pos, Player player) {
		BlockEntity tile = level.getBlockEntity(pos);
		if (tile instanceof LogisticsTileGenericSubMultiBlock) {
			for (LogisticsTileGenericPipe mainPipe : ((LogisticsTileGenericSubMultiBlock) tile).getConnectedMainPipes()) {
				if (mainPipe.isMultiBlock()) {
					BlockState mainState = level.getBlockState(mainPipe.getBlockPos());
					ItemStack pick = LPBlocks.pipe.get().getCloneItemStack(mainState, target, level, mainPipe.getBlockPos(), player);
					if (!pick.isEmpty()) {
						return pick;
					}
				}
			}
		}
		return super.getCloneItemStack(state, target, level, pos, player);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
		if (LogisticsBlockGenericSubMultiBlock.currentCreatedMultiBlock == null && logisticspipes.proxy.MainProxy.isServer(null)) {
			new RuntimeException("Unknown MultiBlock controller").printStackTrace();
		}
		return new LogisticsTileGenericSubMultiBlock(pos, state, LogisticsBlockGenericSubMultiBlock.currentCreatedMultiBlock);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
		return (lvl, pos, st, be) -> {
			if (be instanceof ITickable) ((ITickable) be).update();
		};
	}

	@Override
	@Nonnull
	public java.util.List<ItemStack> getDrops(@Nonnull net.minecraft.world.level.block.state.BlockState state, @Nonnull net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		// Sub-blocks drop nothing; the main pipe block entity handles drops.
		return Collections.emptyList();
	}

	/*
	@Override
	public TextureAtlasSprite getIcon(int p_149691_1_, int p_149691_2_) {
		return LogisticsPipes.LogisticsPipeBlock.getIcon(p_149691_1_, p_149691_2_);
	}

	// TODO: getIcon(IBlockAccess, int, int, int, int) removed in 1.20.1 — rendering rewrite needed (deferred)
	// @Override
	@OnlyIn(Dist.CLIENT)
	@SuppressWarnings({ "all" })
	// getIcon_DEAD — stub removed; IBlockAccess and TextureAtlasSprite-based getIcon removed in 1.20.1
	*/

	public static DoubleCoordinates currentCreatedMultiBlock;

	@Override
	public void onRemove(@Nonnull BlockState state, @Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			if (redirectedToMainPipe) {
				super.onRemove(state, worldIn, pos, newState, isMoving);
				return;
			}
			BlockEntity tile = worldIn.getBlockEntity(pos);
			if (tile instanceof LogisticsTileGenericSubMultiBlock) {
				List<LogisticsTileGenericPipe> mainPipeList = ((LogisticsTileGenericSubMultiBlock) tile).getMainPipe();
				mainPipeList.stream()
						.filter(Objects::nonNull)
						.filter(LogisticsTileGenericPipe::isMultiBlock)
						.forEach(mainPipe -> {
							redirectedToMainPipe = true;
							BlockState mainState = worldIn.getBlockState(mainPipe.getBlockPos());
							LPBlocks.pipe.get().onRemove(mainState, worldIn, mainPipe.getBlockPos(), mainState, false);
							redirectedToMainPipe = false;
							worldIn.removeBlock(mainPipe.getBlockPos(), false);
						});
			}
		}
		super.onRemove(state, worldIn, pos, newState, isMoving);
	}

	@Override
	public void neighborChanged(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, world, pos, block, fromPos, isMoving);
		BlockEntity tile = world.getBlockEntity(pos);
		if (tile instanceof LogisticsTileGenericSubMultiBlock) {
			((LogisticsTileGenericSubMultiBlock) tile).scheduleNeighborChange();
		}
	}

}
