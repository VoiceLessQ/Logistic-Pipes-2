package logisticspipes.utils;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import logisticspipes.interfaces.ISpecialTankAccessHandler;
import logisticspipes.interfaces.ISpecialTankUtil;

public class SpecialTankUtil extends TankUtil implements ISpecialTankUtil {

	private BlockEntity tile;
	private ISpecialTankAccessHandler handler;

	public SpecialTankUtil(IFluidHandler fluid, BlockEntity tile, ISpecialTankAccessHandler handler) {
		super(fluid);
		this.tile = tile;
		this.handler = handler;
	}

	@Override
	public BlockEntity getTileEntity() {
		return tile;
	}

	@Override
	public ISpecialTankAccessHandler getSpecialHandler() {
		return handler;
	}
}
