package logisticspipes.blocks.powertile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import logisticspipes.LPRegistries;

/** Testing block: bottomless FE source, pushes to all neighbors every tick. */
public class CreativePowerSourceTileEntity extends BlockEntity implements IEnergyStorage {

	private static final int PUSH_PER_TICK = 1000000;

	private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> this);

	public CreativePowerSourceTileEntity(BlockPos pos, BlockState state) {
		super(LPRegistries.BE_CREATIVE_POWER_SOURCE.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CreativePowerSourceTileEntity be) {
		for (Direction dir : Direction.values()) {
			BlockEntity tile = level.getBlockEntity(pos.relative(dir));
			if (tile == null) continue;
			tile.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(neighbor -> {
				if (neighbor.canReceive()) neighbor.receiveEnergy(PUSH_PER_TICK, false);
			});
		}
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY) return energy.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energy.invalidate();
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
