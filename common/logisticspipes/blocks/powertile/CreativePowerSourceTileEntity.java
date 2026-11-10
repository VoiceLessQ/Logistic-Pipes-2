package logisticspipes.blocks.powertile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


import logisticspipes.proxy.interfaces.IEnergyStorage;
import logisticspipes.LPRegistries;

/** Testing block: bottomless FE source, pushes to all neighbors every tick. */
public class CreativePowerSourceTileEntity extends BlockEntity implements IEnergyStorage {

	private static final int PUSH_PER_TICK = 1000000;

	public CreativePowerSourceTileEntity(BlockPos pos, BlockState state) {
		super(LPRegistries.BE_CREATIVE_POWER_SOURCE.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CreativePowerSourceTileEntity be) {
		for (Direction dir : Direction.values()) {
			BlockEntity tile = level.getBlockEntity(pos.relative(dir));
			if (tile == null) continue;
			IEnergyStorage neighbor = IEnergyStorage.SIDED.find(level, tile.getBlockPos(), tile.getBlockState(), tile, dir.getOpposite());
			if (neighbor != null && neighbor.canReceive()) neighbor.receiveEnergy(PUSH_PER_TICK, false);
		}
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		return 0;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		return maxExtract;
	}

	@Override
	public int getEnergyStored() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int getMaxEnergyStored() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean canExtract() {
		return true;
	}

	@Override
	public boolean canReceive() {
		return false;
	}
}
