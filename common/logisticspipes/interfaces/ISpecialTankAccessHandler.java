package logisticspipes.interfaces;

import java.util.Map;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.utils.FluidIdentifierStack;

import logisticspipes.utils.FluidIdentifier;

public interface ISpecialTankAccessHandler extends ISpecialTankHandler {

	Map<FluidIdentifier, Long> getAvailableLiquid(BlockEntity tile);

	FluidIdentifierStack drainFrom(BlockEntity tile, FluidIdentifier ident, Integer amount, boolean drain);
}
