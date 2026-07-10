/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.items;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import lombok.Getter;
// import org.apache.logging.log4j.Level; // conflicts with net.minecraft.world.level.Level — use fully qualified

import logisticspipes.LPBlocks;
import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.renderer.IIconProvider;
import logisticspipes.utils.LPPositionSet;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.DoubleCoordinatesType;

/**
 * A logistics pipe Item
 */
public class ItemLogisticsPipe extends LogisticsItem {

	private int newPipeIconIndex;
	private int newPipeRenderList = -1;
	@Getter
	private CoreUnroutedPipe dummyPipe;

	public ItemLogisticsPipe() {
		super();
	}

	@Nonnull
	@Override
	public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext _ctx) {
		Player player = _ctx.getPlayer();
		Level world = _ctx.getLevel();
		BlockPos pos = _ctx.getClickedPos();
		InteractionHand hand = _ctx.getHand();
		Direction facing = _ctx.getClickedFace();
		Block block = LPBlocks.pipe.get();

		BlockState iblockstate = world.getBlockState(pos);
		Block worldBlock = iblockstate.getBlock();

		if (!iblockstate.canBeReplaced()) {
			pos = pos.relative(facing);
		}

		ItemStack itemstack = player.getItemInHand(hand);

		if (itemstack.isEmpty()) {
			return InteractionResult.FAIL;
		}

		if (!dummyPipe.isMultiBlock()) {
			if (player.mayUseItemAt(pos, facing, itemstack) && world.isEmptyBlock(pos)) {
				CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.createPipe(this);

				if (pipe == null) {
					LogisticsPipes.log.warn("Pipe failed to create during placement at {},{},{}", pos.getX(), pos.getY(), pos.getZ());
					return InteractionResult.PASS;
				}

				if (LogisticsBlockGenericPipe.placePipe(pipe, world, pos, block, null)) {
					BlockState state = world.getBlockState(pos);
					if (state.getBlock() == block) {
						//setTileEntityNBT(world, player, pos, stack);
						block.setPlacedBy(world, pos, state, player, itemstack);

						if (player instanceof ServerPlayer)
							CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, pos, itemstack);

						BlockState newBlockState = world.getBlockState(pos);
						SoundType soundtype = newBlockState.getBlock().getSoundType(newBlockState, world, pos, player);
						world.playSound(player, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F,
								soundtype.getPitch() * 0.8F);

						itemstack.shrink(1);
					}
				}

				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.FAIL;
			}
		} else {
			CoreMultiBlockPipe multiPipe = (CoreMultiBlockPipe) dummyPipe;
			boolean isFreeSpace = true;
			DoubleCoordinates placeAt = new DoubleCoordinates(pos);
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> globalPos = new LPPositionSet<>(DoubleCoordinatesType.class);
			globalPos.add(new DoubleCoordinatesType<>(placeAt, CoreMultiBlockPipe.SubBlockTypeForShare.NON_SHARE));
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> positions = multiPipe.getSubBlocks();
			ITubeOrientation orientation = multiPipe.getTubeOrientation(player, pos.getX(), pos.getZ());
			if (orientation == null) {
				return InteractionResult.FAIL;
			}
			orientation.rotatePositions(positions);
			positions.stream().map(iPos -> iPos.add(placeAt)).forEach(globalPos::add);
			globalPos.addToAll(orientation.getOffset());
			placeAt.add(orientation.getOffset());

			for (DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare> iPos : globalPos) {
				if (!player.mayUseItemAt(iPos.getBlockPos(), facing, itemstack) || !world.isEmptyBlock(iPos.getBlockPos())) {
					BlockEntity tile = world.getBlockEntity(iPos.getBlockPos());
					boolean canPlace = false;
					if (tile instanceof LogisticsTileGenericSubMultiBlock) {
						if (CoreMultiBlockPipe.canShare(((LogisticsTileGenericSubMultiBlock) tile).getSubTypes(), iPos.getType())) {
							canPlace = true;
						}
					}
					if (!canPlace) {
						isFreeSpace = false;
						break;
					}
				}
			}
			if (isFreeSpace) {
				CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.createPipe(this);

				if (pipe == null) {
					LogisticsPipes.log.warn("Pipe failed to create during placement at {},{},{}", pos.getX(), pos.getY(), pos.getZ());
					return InteractionResult.SUCCESS;
				}

				if (LogisticsBlockGenericPipe.placePipe(pipe, world, placeAt.getBlockPos(), block, orientation)) {
					BlockState state = world.getBlockState(placeAt.getBlockPos());
					if (state.getBlock() == block) {
						//setTileEntityNBT(world, player, pos, stack);
						block.setPlacedBy(world, pos, state, player, itemstack);

						if (player instanceof ServerPlayer)
							CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, placeAt.getBlockPos(), itemstack);

						BlockState newBlockState = world.getBlockState(placeAt.getBlockPos());
						SoundType soundtype = newBlockState.getBlock().getSoundType(newBlockState, world, placeAt.getBlockPos(), player);
						world.playSound(player, placeAt.getBlockPos(), soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F,
								soundtype.getPitch() * 0.8F);

						itemstack.shrink(1);
					}
				}

				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.FAIL;
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void setPipesIcons(IIconProvider iconProvider) {
	}

	public void setPipeIconIndex(int index, int newIndex) {
		newPipeIconIndex = newIndex;
	}

	public int getNewPipeIconIndex() {
		return newPipeIconIndex;
	}

	public int getNewPipeRenderList() {
		return newPipeRenderList;
	}

	public void setNewPipeRenderList(int list) {
		if (newPipeRenderList != -1) {
			throw new UnsupportedOperationException("Can't reset this");
		}
		newPipeRenderList = list;
	}

	public void setDummyPipe(CoreUnroutedPipe pipe) {
		dummyPipe = pipe;
	}

	@Override
	public void initializeClient(Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
		consumer.accept(ClientExtensionsHolder.EXTENSIONS);
	}

	/** Holds client-only references; loaded lazily so dedicated servers never touch
	 *  client-only classes like BlockEntityWithoutLevelRenderer. */
	private static final class ClientExtensionsHolder {
		static final net.neoforged.neoforge.client.extensions.common.IClientItemExtensions EXTENSIONS =
			new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
				@Override
				public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
					return logisticspipes.renderer.LogisticsPipeItemRenderer.instance();
				}
			};
	}
}
