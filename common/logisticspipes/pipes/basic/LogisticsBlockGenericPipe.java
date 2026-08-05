package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.util.StringRepresentable;

// BlockStateContainer removed — use StateDefinition.Builder in createBlockStateDefinition()
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
// Particle/ParticleEngine/TextureAtlasSprite imports removed — rendering deferred (see addHitEffects/addDestroyEffects TODOs)
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import logisticspipes.interfaces.ITickable;


import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;


import net.minecraft.core.NonNullList;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;


import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static logisticspipes.LPConstants.PIPE_MAX_POS;
import static logisticspipes.LPConstants.PIPE_MIN_POS;
import lombok.AllArgsConstructor;
import lombok.Data;

import logisticspipes.LPBlocks;
import logisticspipes.LogisticsPipes;
import logisticspipes.config.Configs;
import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.ltgpmodcompat.LPMicroblockBlock;
import logisticspipes.proxy.MainProxy;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;
import logisticspipes.renderer.newpipe.PropertyCache;
import logisticspipes.renderer.newpipe.PropertyRenderList;
import logisticspipes.ticks.QueuedTasks;
import logisticspipes.utils.LPPositionSet;
// ClientConfiguration import removed — used only in deferred rendering methods
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.DoubleCoordinatesType;

public class LogisticsBlockGenericPipe extends LPMicroblockBlock {

	public static boolean ignoreSideRayTrace = false;
	public static Map<Item, Function<Item, ? extends CoreUnroutedPipe>> pipes = new HashMap<>();
	public static Map<DoubleCoordinates, CoreUnroutedPipe> pipeRemoved = new HashMap<>();
	public static Map<DoubleCoordinates, BlockPos> pipeSubMultiRemoved = new HashMap<>();
	private static long lastRemovedDate = -1;
	protected final Random rand = new Random();

	public static final IntegerProperty rotationProperty = IntegerProperty.create("rotation", 0, 3);
	public static final EnumProperty<PipeRenderModel> modelTypeProperty = EnumProperty.create("model_type", PipeRenderModel.class);
	public static final Map<Direction, BooleanProperty> connectionPropertys = Arrays.stream(Direction.values()).collect(Collectors
			.toMap(key -> key, key -> BooleanProperty.create("connection_" + key.ordinal())));

	public static final PropertyRenderList propertyRenderList = new PropertyRenderList();
	public static final PropertyCache propertyCache = new PropertyCache();

	public static final AABB PIPE_CENTER_BB = new AABB(PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MAX_POS, PIPE_MAX_POS);
	public static final List<AABB> PIPE_CONN_BB = Arrays.asList(
			new AABB(PIPE_MIN_POS, 0, PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MIN_POS, PIPE_MAX_POS),
			new AABB(PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MIN_POS, PIPE_MAX_POS, 1, PIPE_MAX_POS),
			new AABB(PIPE_MIN_POS, PIPE_MIN_POS, 0, PIPE_MAX_POS, PIPE_MAX_POS, PIPE_MIN_POS),
			new AABB(PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MAX_POS, PIPE_MAX_POS, 1),
			new AABB(0, PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MIN_POS, PIPE_MAX_POS, PIPE_MAX_POS),
			new AABB(PIPE_MAX_POS, PIPE_MIN_POS, PIPE_MIN_POS, 1, PIPE_MAX_POS, PIPE_MAX_POS)
	);

	/**
	 * Pre-built VoxelShapes for all 64 connection combinations (one bit per Direction ordinal).
	 * Index 0 = unconnected (center cube only). Built once at class-load time.
	 */
	private static final VoxelShape[] PIPE_SHAPES;
	static {
		VoxelShape center = Shapes.create(PIPE_CENTER_BB);
		PIPE_SHAPES = new VoxelShape[64];
		for (int mask = 0; mask < 64; mask++) {
			VoxelShape shape = center;
			for (int i = 0; i < 6; i++) {
				if ((mask & (1 << i)) != 0) {
					shape = Shapes.or(shape, Shapes.create(PIPE_CONN_BB.get(i)));
				}
			}
			PIPE_SHAPES[mask] = shape;
		}
	}

	public enum PipeRenderModel implements StringRepresentable {
		NONE,
		REQUEST_TABLE;

		@Override
		@Nonnull
		public String getSerializedName() {
			return name().toLowerCase();
		}
	}

	public LogisticsBlockGenericPipe() {
		super(BlockBehaviour.Properties.of().strength(0.5F).noOcclusion());
		registerDefaultState(this.stateDefinition.any()
				.setValue(rotationProperty, 0)
				.setValue(modelTypeProperty, PipeRenderModel.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(rotationProperty, modelTypeProperty);
		connectionPropertys.values().forEach(builder::add);
		// TODO: propertyRenderList / propertyCache were IExtendedBlockState properties — removed in 1.20.1; rendering rewrite needed
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
		return new LogisticsTileGenericPipe(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
			@Nonnull net.minecraft.world.level.Level level,
			@Nonnull BlockState state,
			@Nonnull BlockEntityType<T> type) {
		// Without a ticker registered on the owning block, BlockEntity.tick equivalents
		// (here: LogisticsTileGenericPipe.update via ITickable) are never called and the
		// entire mod — routing graph updates, module logic, item transport — sits idle.
		return (lvl, pos, st, be) -> {
			if (be instanceof ITickable tickable) tickable.update();
		};
	}

	@Override
	@Nonnull
	public net.minecraft.world.level.block.RenderShape getRenderShape(@Nonnull BlockState state) {
		// Pipe geometry is emitted by LogisticsRenderPipe (BlockEntityRenderer) via the
		// CCL-replacement pipeline in logisticspipes.proxy.object3d.impl — not a JSON model.
		return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
	}

	public static void removePipe(CoreUnroutedPipe pipe) {
		if (!LogisticsBlockGenericPipe.isValid(pipe)) {
			return;
		}

		if (pipe.canBeDestroyed() || pipe.destroyByPlayer()) {
			pipe.onBlockRemoval();
		} else if (pipe.preventRemove()) {
			LogisticsBlockGenericPipe.cacheTileToPreventRemoval(pipe);
		}

		Level world = pipe.container.getLevel();

		if (LogisticsBlockGenericPipe.lastRemovedDate != world.getGameTime()) {
			LogisticsBlockGenericPipe.lastRemovedDate = world.getGameTime();
			LogisticsBlockGenericPipe.pipeRemoved.clear();
			LogisticsBlockGenericPipe.pipeSubMultiRemoved.clear();
		}

		if (pipe.isMultiBlock()) {
			if (pipe.preventRemove()) {
				throw new UnsupportedOperationException("A multi block can't be protected against removal.");
			}
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> list = ((CoreMultiBlockPipe) pipe).getRotatedSubBlocks();
			list.forEach(pos -> pos.add(new DoubleCoordinates(pipe)));
			for (DoubleCoordinates pos : pipe.container.subMultiBlock) {
				BlockEntity tile = pos.getTileEntity(world);
				if (tile instanceof LogisticsTileGenericSubMultiBlock) {
					DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare> equ = list.findClosest(pos);
					if (equ != null) {
						((LogisticsTileGenericSubMultiBlock) tile).removeSubType(equ.getType());
					}
					if (((LogisticsTileGenericSubMultiBlock) tile).removeMainPipe(new DoubleCoordinates(pipe))) {
						LogisticsBlockGenericSubMultiBlock.redirectedToMainPipe = true;
						pos.setBlockToAir(world);
						LogisticsBlockGenericSubMultiBlock.redirectedToMainPipe = false;
						LogisticsBlockGenericPipe.pipeSubMultiRemoved.put(new DoubleCoordinates(pos), pipe.container.getBlockPos());
					} else {
						MainProxy.sendPacketToAllWatchingChunk(tile, ((LogisticsTileGenericSubMultiBlock) tile).getLPDescriptionPacket());
					}
				}
			}
		}

		BlockPos pos = pipe.container.getBlockPos();
		LogisticsBlockGenericPipe.pipeRemoved.put(new DoubleCoordinates(pos), pipe);
		world.removeBlockEntity(pos);
	}

	/* Registration ******************************************************** */

	/**
	 * Registers a pipe item via DeferredRegister and adds it to the {@code pipes} map.
	 * The registry name will be {@code pipe_<name>}.
	 *
	 * <p>Icon/dummy-pipe setup (setPipeIconIndex, setIconProviderFromPipe, setDummyPipe)
	 * is intentionally deferred — those methods belong to the 1.12.2 rendering system
	 * and will be addressed when ItemLogisticsPipe is migrated to 1.20.1.</p>
	 */
	public static RegistryObject<ItemLogisticsPipe> registerPipe(
			DeferredRegister<Item> registry,
			String name,
			Function<Item, ? extends CoreUnroutedPipe> constructor) {
		return registry.register("pipe_" + name, () -> {
			ItemLogisticsPipe item = new ItemLogisticsPipe();
			LogisticsBlockGenericPipe.pipes.put(item, constructor);
			// Create a dummy pipe instance for type/size queries (isMultiBlock, etc.)
			// used by ItemLogisticsPipe.useOn before a real pipe is placed.
			try {
				item.setDummyPipe(constructor.apply(item));
			} catch (Exception e) {
				LogisticsPipes.log.error("Failed to create dummy pipe for {}", name, e);
			}
			return item;
		});
	}

	public static CoreUnroutedPipe createPipe(Item key) {
		Function<Item, ? extends CoreUnroutedPipe> pipe = LogisticsBlockGenericPipe.pipes.get(key);
		if (pipe != null) {
			return pipe.apply(key);
		} else {
			LogisticsPipes.log.warn("Detected pipe with unknown key (" + key + "). This should not have happend.");
		}

		return null;
	}

	public static boolean placePipe(CoreUnroutedPipe pipe, Level world, BlockPos blockPos, Block block) {
		return LogisticsBlockGenericPipe.placePipe(pipe, world, blockPos, block, null);
	}

	public static boolean placePipe(CoreUnroutedPipe pipe, Level world, BlockPos blockPos, Block block, ITubeOrientation orientation) {
		BlockState oldBlockState = world.getBlockState(blockPos);
		boolean placed = world.setBlock(blockPos, block.defaultBlockState(), 3);

		if (world.isClientSide) {
			return placed;
		}

		if (placed) {
			BlockEntity tile = world.getBlockEntity(blockPos);
			if (tile instanceof LogisticsTileGenericPipe) {
				LogisticsTileGenericPipe tilePipe = (LogisticsTileGenericPipe) tile;
				if (pipe instanceof CoreMultiBlockPipe) {
					if (orientation == null) {
						throw new NullPointerException();
					}
					CoreMultiBlockPipe mPipe = (CoreMultiBlockPipe) pipe;
					orientation.setOnPipe(mPipe);
					DoubleCoordinates placeAt = new DoubleCoordinates(blockPos);
					LogisticsBlockGenericSubMultiBlock.currentCreatedMultiBlock = placeAt;
					LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> positions = ((CoreMultiBlockPipe) pipe).getSubBlocks();
					orientation.rotatePositions(positions);
					for (DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare> pos : positions) {
						pos.add(placeAt);
						BlockEntity subTile = world.getBlockEntity(pos.getBlockPos());
						BlockState oldSubBlockState = world.getBlockState(pos.getBlockPos());
						if (subTile instanceof LogisticsTileGenericSubMultiBlock) {
							((LogisticsTileGenericSubMultiBlock) subTile).addMultiBlockMainPos(placeAt);
							((LogisticsTileGenericSubMultiBlock) subTile).addSubTypeTo(pos.getType());
							MainProxy.sendPacketToAllWatchingChunk(subTile, ((LogisticsTileGenericSubMultiBlock) subTile).getLPDescriptionPacket());
						} else {
							world.setBlock(pos.getBlockPos(), LPBlocks.subMultiblock.get().defaultBlockState(), 3);
							subTile = world.getBlockEntity(pos.getBlockPos());
							if (subTile instanceof LogisticsTileGenericSubMultiBlock) {
								((LogisticsTileGenericSubMultiBlock) subTile).addSubTypeTo(pos.getType());
							}
						}
						world.markAndNotifyBlock(pos.getBlockPos(), world.getChunkAt(pos.getBlockPos()), oldSubBlockState, world.getBlockState(pos.getBlockPos()), 3, 512);
					}
					LogisticsBlockGenericSubMultiBlock.currentCreatedMultiBlock = null;
				}
				tilePipe.initialize(pipe);
				//				tilePipe.sendUpdateToClient();
			}
			world.markAndNotifyBlock(blockPos, world.getChunkAt(blockPos), oldBlockState, world.getBlockState(blockPos), 3, 512);
		}

		return placed;
	}

	public static CoreUnroutedPipe getPipe(BlockGetter blockAccess, BlockPos pos) {
		BlockEntity tile = blockAccess.getBlockEntity(pos);

		if (!(tile instanceof LogisticsTileGenericPipe) || tile.isRemoved()) {
			return null;
		} else {
			return ((LogisticsTileGenericPipe) tile).pipe;
		}
	}

	public static boolean isFullyDefined(CoreUnroutedPipe pipe) {
		return pipe != null && pipe.transport != null && pipe.container != null;
	}

	public static boolean isValid(CoreUnroutedPipe pipe) {
		return LogisticsBlockGenericPipe.isFullyDefined(pipe);
	}

	private static void cacheTileToPreventRemoval(CoreUnroutedPipe pipe) {
		final Level worldCache = pipe.getWorld();
		final BlockPos posCache = pipe.getPos();
		final BlockEntity tileCache = pipe.container;
		final CoreUnroutedPipe fPipe = pipe;
		fPipe.setPreventRemove(true);
		QueuedTasks.queueTask(() -> {
			if (!fPipe.preventRemove()) {
				return null;
			}
			boolean changed = false;
			if (worldCache.getBlockState(posCache).getBlock() != LPBlocks.pipe.get()) {
				worldCache.setBlock(posCache, LPBlocks.pipe.get().defaultBlockState(), 3);
				changed = true;
			}
			if (worldCache.getBlockEntity(posCache) != tileCache) {
				worldCache.setBlockEntity(tileCache);
				changed = true;
			}
			if (changed) {
				worldCache.markAndNotifyBlock(posCache, worldCache.getChunkAt(posCache), worldCache.getBlockState(posCache), worldCache.getBlockState(posCache), 3, 512);
			}
			fPipe.setPreventRemove(false);
			return null;
		});
	}

	@Nonnull
	// @Override removed — getDrops(BlockGetter...) does not match 1.20.1 Block API
	public NonNullList<ItemStack> getDrops(@Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull BlockState state, int fortune) {
		NonNullList<ItemStack> list = NonNullList.create();
		if (world instanceof Level && MainProxy.isClient((Level) world)) {
			return list;
		}

		int count = 1; // quantityDropped removed in 1.20.1
		for (int i = 0; i < count; i++) {
			CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(world, pos);

			if (pipe == null) {
				pipe = LogisticsBlockGenericPipe.pipeRemoved.get(new DoubleCoordinates(pos));
			}

			if (pipe != null) {
				if (pipe.item != null && (pipe.canBeDestroyed() || pipe.destroyByPlayer())) {
					list.addAll(pipe.dropContents());
					list.add(new ItemStack(pipe.item, 1));
				} else if (pipe.item != null) {
					LogisticsBlockGenericPipe.cacheTileToPreventRemoval(pipe);
				}
			}
		}
		return list;
	}

	@Override
	@Nonnull
	public List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		BlockEntity tile = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (tile instanceof LogisticsTileGenericPipe logisticsTile && logisticsTile.pipe != null && logisticsTile.pipe.item != null) {
			return List.of(new ItemStack(logisticsTile.pipe.item));
		}
		return List.of();
	}

	// getBlockFaceShape removed in 1.20.1; dead stub kept for reference
	@Nonnull
	public Object /* BlockFaceShape */ getBlockFaceShape_DEAD(BlockGetter worldIn, BlockState state, BlockPos pos, Direction face) {
		return null; // BlockFaceShape.UNDEFINED — removed in 1.20.1
	}

	public void addCollisionBoxToList(LogisticsTileGenericPipe pipe, AABB entityBox, List<AABB> collidingBoxes, Entity entityIn, boolean isActualState) {
		addCollisionBoxToList(pipe.getLevel().getBlockState(pipe.getBlockPos()), pipe.getLevel(), pipe.getBlockPos(), entityBox, collidingBoxes, entityIn, isActualState);
	}

	/** Replaces removed Block.addCollisionBoxToList — offsets the box by pos, then adds to list if it intersects entityBox. */
	private static void addCollisionBoxToList(BlockPos pos, AABB entityBox, List<AABB> collidingBoxes, AABB box) {
		AABB offsetBox = box.move(pos.getX(), pos.getY(), pos.getZ());
		if (entityBox.intersects(offsetBox)) {
			collidingBoxes.add(offsetBox);
		}
	}

	// Internal collision helper — addCollisionBoxToList is no longer an MC override in 1.20.1; called from the overload above
	public void addCollisionBoxToList(@Nonnull BlockState state, Level world, @Nonnull BlockPos pos, @Nonnull AABB entityBox, @Nonnull List<AABB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
		BlockEntity te = world.getBlockEntity(pos);
		if (te instanceof LogisticsTileGenericPipe) {
			LogisticsTileGenericPipe tile = (LogisticsTileGenericPipe) te;
			CoreUnroutedPipe pipe = tile.pipe;
			if (pipe != null && pipe.isPipeBlock()) {
				addCollisionBoxToList(pos, entityBox, collidingBoxes, new AABB(0, 0, 0, 1, 1, 1)); // Block.FULL_BLOCK_AABB removed in 1.20.1
				return;
			}
			if (pipe != null && pipe.isMultiBlock()) {
				((CoreMultiBlockPipe) pipe).addCollisionBoxesToList(collidingBoxes, entityBox);
				if (!pipe.actAsNormalPipe()) return;
			}

			Arrays.stream(Direction.values())
					.filter(tile::isPipeConnectedCached)
					.map(f -> PIPE_CONN_BB.get(f.ordinal()))
					.forEach(bb -> addCollisionBoxToList(pos, entityBox, collidingBoxes, bb));
		}
		addCollisionBoxToList(pos, entityBox, collidingBoxes, PIPE_CENTER_BB);
	}

	@Override
	@Nonnull
	public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
		BlockEntity te = world.getBlockEntity(pos);
		if (!(te instanceof LogisticsTileGenericPipe)) return Shapes.block();
		LogisticsTileGenericPipe tile = (LogisticsTileGenericPipe) te;
		CoreUnroutedPipe pipe = tile.pipe;
		if (pipe == null || pipe.isPipeBlock()) return Shapes.block();
		if (pipe.isMultiBlock() && !pipe.actAsNormalPipe()) {
			// The pipe centre stays included so the main block is always targetable,
			// matching doRayTraceMultiblock's centre-box test.
			return Shapes.or(Shapes.create(PIPE_CENTER_BB), getMultiBlockShape((CoreMultiBlockPipe) pipe, pos));
		}
		int mask = 0;
		for (Direction dir : Direction.values()) {
			if (tile.isPipeConnectedCached(dir)) {
				mask |= (1 << dir.ordinal());
			}
		}
		return PIPE_SHAPES[mask];
	}

	@Override
	@Nonnull
	public VoxelShape getCollisionShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
		return getShape(state, world, pos, context);
	}

	/**
	 * Block-local VoxelShape of a multiblock tube for one block cell, built from the pipe's
	 * world-space collision boxes (the same geometry LP1 fed to addCollisionBoxToList).
	 * Returns {@link Shapes#empty()} when no box touches the cell.
	 */
	@Nonnull
	public static VoxelShape getMultiBlockShape(CoreMultiBlockPipe pipe, @Nonnull BlockPos cell) {
		List<AABB> boxes = new ArrayList<>();
		pipe.addCollisionBoxesToList(boxes, new AABB(cell));
		VoxelShape shape = Shapes.empty();
		for (AABB box : boxes) {
			shape = Shapes.or(shape, Shapes.create(box.move(-cell.getX(), -cell.getY(), -cell.getZ())));
		}
		return shape;
	}

	// getSelectedBoundingBox removed in 1.20.1 — replaced by getShape(state, level, pos, context)
	// Renamed to avoid @Override on non-existent method; logic preserved for reference
	@OnlyIn(Dist.CLIENT)
	public AABB getSelectedBoundingBox_DEAD(BlockState state, Level world, @Nonnull BlockPos pos) {
		BlockEntity tile = world.getBlockEntity(pos);
		if (tile instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe) tile).isPipeBlock()) {
			return new AABB((double) pos.getX() + 0, (double) pos.getY() + 0, (double) pos.getZ() + 0,
					(double) pos.getX() + 1, (double) pos.getY() + 1, (double) pos.getZ() + 1);
		}
		InternalRayTraceResult rayTraceResult = doRayTrace(world, pos, Minecraft.getInstance().player);

		if (rayTraceResult != null && rayTraceResult.boundingBox != null) {
			AABB box = rayTraceResult.boundingBox;
			if (rayTraceResult.hitPart == Part.PIPE) {
				float scale = 0.001F;
				box = box.inflate(scale, scale, scale);
			}
			return box.move(pos.getX(), pos.getY(), pos.getZ());
		}
		return new AABB(pos); // fallback
	}

	// collisionRayTrace removed in 1.20.1 — migrated to getShape(BlockState, BlockGetter, BlockPos, CollisionContext)
	// The entire ray-trace API (AABB.calculateIntercept, new HitResult(hitVec, side, pos), Block.FULL_BLOCK_AABB) is gone.
	// @Override
	// public HitResult collisionRayTrace(BlockState state, Level world, BlockPos pos, Vec3 start, Vec3 end) { ... }

	public InternalRayTraceResult doRayTrace(Level world, BlockPos pos, Player player) {
		double reachDistance = player instanceof ServerPlayer
				? player.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get())
				: 5;

		Vec3 lookVec = player.getLookAngle();
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(lookVec.x * reachDistance, lookVec.y * reachDistance, lookVec.z * reachDistance);

		return doRayTrace(world, pos, start, end);
	}

	public InternalRayTraceResult doRayTrace(Level world, BlockPos pos, Vec3 start, Vec3 end) {
		BlockEntity te = world.getBlockEntity(pos);

		if (te instanceof LogisticsTileGenericPipe) {
			LogisticsTileGenericPipe tileG = (LogisticsTileGenericPipe) te;
			CoreUnroutedPipe pipe = tileG.pipe;
			if (!LogisticsBlockGenericPipe.isValid(pipe)) return null;

			if (pipe.isMultiBlock()) {
				InternalRayTraceResult result1 = doRayTraceMultiblock(tileG, (CoreMultiBlockPipe) pipe, start, end);

				if (!pipe.actAsNormalPipe())
					return result1;

				InternalRayTraceResult result2 = doRayTrace(tileG, pipe, start, end);

				return Stream.of(result1, result2)
						.filter(Objects::nonNull)
						.min(Comparator.comparing(r -> r.rayTraceResult.getLocation().distanceToSqr(start)))
						.orElse(null);
			} else {
				return doRayTrace(tileG, pipe, start, end);
			}
		}
		return null;
	}

	@Data
	@AllArgsConstructor
	private static class Hit {

		public HitResult rayTraceResult;
		public AABB box;
		public Direction side;
		public Part part;
	}

	private InternalRayTraceResult doRayTrace(LogisticsTileGenericPipe tileG, CoreUnroutedPipe pipe, Vec3 start, Vec3 end) {
		if (tileG == null) return null;
		if (!LogisticsBlockGenericPipe.isValid(pipe)) return null;

		/*
		 * pipe hits along x, y, and z axis, gate (all 6 sides) [and
		 * wires+facades]
		 */
		ArrayList<Hit> list = new ArrayList<>();

		// pipe
		for (Direction side : LogisticsBlockGenericPipe.DIR_VALUES) {
			if (side == null || tileG.isPipeConnectedCached(side)) {
				if (side != null && ignoreSideRayTrace) continue;
				AABB bb = getPipeBoundingBox(side);
				// rayTrace(pos, start, end, AABB) removed in 1.20.1 — use VoxelShape.clip
				list.add(new Hit(net.minecraft.world.phys.shapes.Shapes.create(bb).clip(start, end, tileG.getBlockPos()), bb, side, Part.PIPE));
			}
		}

		// pluggables

		/*
		for (Direction side : Direction.values()) {
			if (tileG.getBCPipePluggable(side) != null) {
				if(side != null && ignoreSideRayTrace) continue;
				AABB bb = tileG.getBCPipePluggable(side).getBoundingBox(side);
				boxes[7 + side.ordinal()] = bb;
				hits[7 + side.ordinal()] = super.collisionRayTrace(new BoundingBoxDelegateBlockState(bb, state), tileG.getLevel(), tileG.getBlockPos(), start, end);
				sideHit[7 + side.ordinal()] = side;
			}
		}
		*/

		// wire hit-test not implemented

		// get closest hit

		return list.stream()
				.filter(r -> r.rayTraceResult != null)
				.min(Comparator.comparing(r -> r.rayTraceResult.getLocation().distanceToSqr(start)))
				.map(r -> new InternalRayTraceResult(r.part, r.rayTraceResult, r.box, r.side))
				.orElse(null);
	}

	/**
	 * Ray-trace against a multi-block pipe's central bounding box plus the bounding box of each
	 * sub-block position (translated from the main tile's position). Uses {@link Shapes#create}
	 * and {@link net.minecraft.world.phys.shapes.VoxelShape#clip} since the 1.12
	 * {@code AABB.calculateIntercept} API is gone.
	 */
	private InternalRayTraceResult doRayTraceMultiblock(LogisticsTileGenericPipe tileG, CoreMultiBlockPipe pipe, Vec3 start, Vec3 end) {
		ArrayList<Hit> list = new ArrayList<>();
		// Centre block — always test so the player can click the main pipe body itself.
		BlockPos mainPos = tileG.getBlockPos();
		AABB centerBox = PIPE_CENTER_BB.move(mainPos.getX(), mainPos.getY(), mainPos.getZ());
		net.minecraft.world.phys.BlockHitResult centerHit =
				net.minecraft.world.phys.shapes.Shapes.create(centerBox).clip(start, end, mainPos);
		if (centerHit != null) {
			list.add(new Hit(centerHit, centerBox, null, Part.PIPE));
		}

		// Test the tube's real world-space collision boxes so targeting matches the
		// visible geometry instead of full cubes at each sub-block position.
		List<AABB> tubeBoxes = new ArrayList<>();
		pipe.addCollisionBoxesToList(tubeBoxes, null);
		for (AABB box : tubeBoxes) {
			BlockPos cell = BlockPos.containing(box.getCenter());
			net.minecraft.world.phys.BlockHitResult subHit = net.minecraft.world.phys.shapes.Shapes
					.create(box.move(-cell.getX(), -cell.getY(), -cell.getZ()))
					.clip(start, end, cell);
			if (subHit != null) {
				list.add(new Hit(subHit, box.move(-mainPos.getX(), -mainPos.getY(), -mainPos.getZ()), null, Part.PIPE));
			}
		}

		return list.stream()
				.filter(r -> r.rayTraceResult != null)
				.min(Comparator.comparing(r -> r.rayTraceResult.getLocation().distanceToSqr(start)))
				.map(r -> new InternalRayTraceResult(r.part, r.rayTraceResult, r.box, r.side))
				.orElse(null);
	}

	private AABB getPipeBoundingBox(@Nullable Direction side) {
		if (side == null) return PIPE_CENTER_BB;
		return PIPE_CONN_BB.get(side.ordinal());
	}

	// createNewTileEntity removed in 1.20.1 — replaced by newBlockEntity(BlockPos, BlockState) above

	public enum Part {
		PIPE,
		UNKNOWN
	}

	public static class InternalRayTraceResult {

		public final Part hitPart;
		public final HitResult rayTraceResult;
		public final AABB boundingBox;
		public final Direction sideHit;

		InternalRayTraceResult(Part hitPart, HitResult rayTraceResult, AABB boundingBox, Direction side) {
			this.hitPart = hitPart;
			this.rayTraceResult = rayTraceResult;
			this.boundingBox = boundingBox;
			sideHit = side;
		}

		@Override
		public String toString() {
			return String.format("HitResult: %s, %s", hitPart == null ? "null" : hitPart.name(), boundingBox == null ? "null" : boundingBox.toString());
		}
	}

	private static final Direction[] DIR_VALUES;

	static {
		DIR_VALUES = new Direction[Direction.values().length + 1];
		DIR_VALUES[0] = null;
		System.arraycopy(Direction.values(), 0, DIR_VALUES, 1, Direction.values().length);
	}

	// getBlockHardness removed in 1.20.1 — set via BlockBehaviour.Properties.strength() in constructor
	// getRenderType removed in 1.20.1 — MODEL is the default
	// getRenderLayer removed in 1.20.1 — use ItemBlockRenderTypes.setRenderLayer in client setup
	// isFullBlock / isFullCube / isNormalCube / isOpaqueCube / isTopSolid — all removed in 1.20.1

	// @Override removed — canBeReplacedByLeaves removed in 1.20.1
	public boolean canBeReplacedByLeaves_DEAD(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos) {
		return false;
	}

	// @Override removed — isSideSolid(BlockGetter...) removed in 1.20.1
	public boolean isSideSolid_DEAD(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		BlockEntity tile = world.getBlockEntity(pos);

		if (tile instanceof LogisticsTileGenericPipe) {
			if (((LogisticsTileGenericPipe) tile).isPipeBlock()) {
				return true;
			}
		}

		return false; // super.isSideSolid removed
	}

	@Override
	public void onRemove(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			LogisticsBlockGenericPipe.removePipe(LogisticsBlockGenericPipe.getPipe(world, pos));
		}
		super.onRemove(state, world, pos, newState, isMoving);
	}

	// @Override removed — dropBlockAsItemWithChance removed in 1.20.1
	public void dropBlockAsItemWithChance_DEAD(Level world, @Nonnull final BlockPos pos, @Nonnull BlockState state, float chance, int fortune) {

		if (world.isClientSide) {
			return;
		}

		// quantityDropped removed in 1.20.1; drop once if chance passes
		if (world.getRandom().nextFloat() > chance) {
			return;
		}

		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(world, pos);

		if (pipe == null) {
			pipe = LogisticsBlockGenericPipe.pipeRemoved.get(new DoubleCoordinates(pos));
		}

		if (pipe == null) return;

		if (pipe.item != null && (pipe.canBeDestroyed() || pipe.destroyByPlayer())) {
			for (ItemStack stack : pipe.dropContents()) {
				Block.popResource(world, pos, stack);
			}
			Block.popResource(world, pos, new ItemStack(pipe.item, 1));
		} else if (pipe.item != null) {
			LogisticsBlockGenericPipe.cacheTileToPreventRemoval(pipe);
		}
	}

	// @Override removed — getItemDropped removed in 1.20.1
	public Item getItemDropped_DEAD(BlockState state, Random rand, int fortune) {
		// Returns null to be safe - the id does not depend on the meta
		return null;
	}

	@OnlyIn(Dist.CLIENT)
	@Nonnull
	public ItemStack getPickedResult(BlockState state, HitResult target, net.minecraft.world.level.LevelReader levelReader, BlockPos pos, Player player) {
		ItemStack pick = getCloneItemStack(levelReader, pos, state);
		if (!pick.isEmpty()) {
			return pick;
		}
		Level world = (Level) levelReader; // safe — client-only code, LevelReader is always a Level here
		InternalRayTraceResult rayTraceResult = doRayTrace(world, pos, player);

		if (rayTraceResult != null && rayTraceResult.boundingBox != null) {
			if (rayTraceResult.hitPart == Part.PIPE) {
				final CoreUnroutedPipe pipe = Objects.requireNonNull(LogisticsBlockGenericPipe.getPipe(world, pos));
				return new ItemStack(pipe.item);
			}
		}
		return ItemStack.EMPTY;
	}

	// Forge's pick-block hook (middle click). Without this the block falls back to its own
	// (nonexistent) item and the client logs "Picking on: [BLOCK] logisticspipes:pipe gave
	// null item"; the pipe's actual item lives on the pipe object, not the block.
	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, net.minecraft.world.level.BlockGetter level, BlockPos pos, Player player) {
		BlockEntity tile = level.getBlockEntity(pos);
		if (tile instanceof LogisticsTileGenericPipe
				&& LogisticsBlockGenericPipe.isValid(((LogisticsTileGenericPipe) tile).pipe)
				&& ((LogisticsTileGenericPipe) tile).pipe.item != null) {
			return new ItemStack(((LogisticsTileGenericPipe) tile).pipe.item);
		}
		return super.getCloneItemStack(state, target, level, pos, player);
	}

	/* Wrappers ************************************************************ */
	@Override
	public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean movedByPiston) {
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, movedByPiston);

		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(worldIn, pos);

		if (LogisticsBlockGenericPipe.isValid(pipe)) {
			pipe.container.scheduleNeighborChange();
		}
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(world, pos);

		if (LogisticsBlockGenericPipe.isValid(pipe)) {
			pipe.onBlockPlaced();
			pipe.onBlockPlacedBy(placer);
			if (pipe instanceof IRotationProvider) {
				((IRotationProvider) pipe).setFacing(placer.getDirection().getOpposite());
			}
		}
	}

	@Override
	public InteractionResult use(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hitResult) {
		InteractionResult superResult = super.use(state, world, pos, player, hand, hitResult);
		if (superResult != InteractionResult.PASS) return superResult;

		ItemStack heldItem = player.getInventory().items.get(player.getInventory().selected);

		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(world, pos);

		if (LogisticsBlockGenericPipe.isValid(pipe)) {

			if (heldItem.isEmpty()) {
				// Fall through the end of the test
			} else if (heldItem.getItem() instanceof net.minecraft.world.item.SignItem) { // Items.SIGN removed in 1.20.1 — sign items split per wood type
				// Sign will be placed anyway, so lets show the sign gui
				return InteractionResult.PASS;
			} else if (heldItem.getItem() instanceof ItemLogisticsPipe) {
				return InteractionResult.PASS;
			}
			return pipe.blockActivated(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}

		return InteractionResult.PASS;
	}

	@Override
	public boolean isSignalSource(BlockState state) { // canProvidePower renamed to isSignalSource in 1.20.1
		return true;
	}

	// TODO: addHitEffects rendering — migrate to NeoForge IBlockExtension.addHitEffects with ParticleEngine (deferred)
	// @OnlyIn(Dist.CLIENT)
	// @Override
	// public boolean addHitEffects(BlockState state, Level world, HitResult target, ParticleEngine effectRenderer) { ... }

	// TODO: addDestroyEffects rendering — migrate to NeoForge IBlockExtension.addDestroyEffects with ParticleEngine (deferred)
	// @OnlyIn(Dist.CLIENT)
	// @Override
	// public boolean addDestroyEffects(BlockState state, Level world, BlockPos pos, ParticleEngine effectRenderer) { ... }

private void checkForRenderChanges(BlockGetter worldIn, BlockPos blockPos) {
		BlockEntity tile = new DoubleCoordinates(blockPos).getTileEntity(worldIn);
		if (!(tile instanceof LogisticsTileGenericPipe)) return;
		((LogisticsTileGenericPipe) tile).renderState.checkForRenderUpdate(worldIn, blockPos);
	}

	// @Override removed — canRenderInLayer removed in 1.20.1
	public boolean canRenderInLayer_DEAD(BlockState state, Object /* BlockRenderLayer */ layer) {
		return true;
	}
}
