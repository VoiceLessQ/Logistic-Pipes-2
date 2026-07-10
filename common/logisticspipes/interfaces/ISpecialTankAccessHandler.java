package logisticspipes.interfaces;

import java.util.Map;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.fluids.FluidStack;

import logisticspipes.utils.FluidIdentifier;

public interface ISpecialTankAccessHandler extends ISpecialTankHandler {

	Map<FluidIdentifier, Long> getAvailableLiquid(BlockEntity tile);

	FluidStack drainFrom(BlockEntity tile, FluidIdentifier ident, Integer amount, boolean drain);
}
