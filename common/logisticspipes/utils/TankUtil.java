package logisticspipes.utils;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import logisticspipes.interfaces.ITankUtil;

public class TankUtil implements ITankUtil {

	private final IFluidHandler fluidhandler;

	public TankUtil(IFluidHandler fluidhandler) {
		this.fluidhandler = fluidhandler;
	}

	@Override
	public boolean containsTanks() {
		return fluidhandler.getTanks() > 0;
	}

	@Override
	public int fill(FluidIdentifierStack stack, boolean doFill) {
		return fluidhandler.fill(stack.makeFluidStack(),
				doFill ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
	}

	@Override
	public FluidIdentifierStack drain(FluidIdentifierStack stack, boolean doDrain) {
		return FluidIdentifierStack.getFromStack(fluidhandler.drain(stack.makeFluidStack(),
				doDrain ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE));
	}

	@Override
	public FluidIdentifierStack drain(int amount, boolean doDrain) {
		return FluidIdentifierStack.getFromStack(fluidhandler.drain(amount,
				doDrain ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE));
	}

	@Override
	public Stream<FluidStack> tanks() {
		return IntStream.range(0, fluidhandler.getTanks())
				.mapToObj(fluidhandler::getFluidInTank)
				.filter(stack -> !stack.isEmpty());
	}

	@Override
	public boolean canDrain(FluidIdentifier fluid) {
		FluidStack probe = fluid.makeFluidStack(1);
		return !fluidhandler.drain(probe, IFluidHandler.FluidAction.SIMULATE).isEmpty();
	}

	@Override
	public int getFreeSpaceInsideTank(FluidIdentifier type) {
		int free = 0;
		for (int i = 0; i < fluidhandler.getTanks(); i++) {
			FluidStack content = fluidhandler.getFluidInTank(i);
			if (content.isEmpty() || FluidIdentifier.get(content) == type) {
				free += fluidhandler.getTankCapacity(i) - content.getAmount();
			}
		}
		return free;
	}
}
