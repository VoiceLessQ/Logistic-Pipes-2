package logisticspipes.blocks.powertile;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import logisticspipes.LPRegistries;

/** Testing block: creative FE source, see CreativePowerSourceTileEntity. */
public class CreativePowerSourceBlock extends Block implements EntityBlock {

	public CreativePowerSourceBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.METAL));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CreativePowerSourceTileEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide || type != LPRegistries.BE_CREATIVE_POWER_SOURCE.get()) {
			return null;
		}
		return (lvl, pos, st, be) -> CreativePowerSourceTileEntity.serverTick(lvl, pos, st, (CreativePowerSourceTileEntity) be);
	}
}
