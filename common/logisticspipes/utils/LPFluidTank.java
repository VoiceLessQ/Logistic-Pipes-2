package logisticspipes.utils;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;

/**
 * Single fluid tank sized in millibuckets. LP code uses the mB methods outside transactions;
 * other mods reach it as a Transfer API storage.
 */
public class LPFluidTank extends SingleVariantStorage<FluidVariant> {

	public static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

	private final int capacityMb;

	public LPFluidTank(int capacityMb) {
		this.capacityMb = capacityMb;
	}

	public static int toMb(long droplets) {
		return (int) Math.min(Integer.MAX_VALUE, droplets / DROPLETS_PER_MB);
	}

	public static long toDroplets(int mb) {
		return mb * DROPLETS_PER_MB;
	}

	@Override
	protected FluidVariant getBlankVariant() {
		return FluidVariant.blank();
	}

	@Override
	protected long getCapacity(FluidVariant variant) {
		return toDroplets(capacityMb);
	}

	public int getCapacityMb() {
		return capacityMb;
	}

	public int getFluidAmount() {
		return isResourceBlank() ? 0 : toMb(amount);
	}

	public boolean isEmpty() {
		return getFluidAmount() <= 0;
	}

	@Nullable
	public FluidIdentifierStack getFluid() {
		if (isEmpty()) return null;
		return FluidIdentifierStack.getFromVariant(variant, amount);
	}

	public void setFluid(@Nullable FluidIdentifierStack stack) {
		if (stack == null || stack.getAmount() <= 0) {
			variant = FluidVariant.blank();
			amount = 0;
		} else {
			variant = stack.makeFluidVariant();
			amount = stack.getDroplets();
		}
	}

	/** @return millibuckets accepted */
	public int fill(@Nullable FluidIdentifierStack stack, boolean doFill) {
		if (stack == null || stack.getAmount() <= 0) return 0;
		FluidVariant incoming = stack.makeFluidVariant();
		if (!isResourceBlank() && !variant.equals(incoming)) return 0;
		int filled = Math.min(stack.getAmount(), capacityMb - getFluidAmount());
		if (filled <= 0) return 0;
		if (doFill) {
			variant = incoming;
			amount += toDroplets(filled);
		}
		return filled;
	}

	@Nullable
	public FluidIdentifierStack drain(int maxMb, boolean doDrain) {
		if (isEmpty() || maxMb <= 0) return null;
		int drained = Math.min(maxMb, getFluidAmount());
		FluidIdentifierStack result = FluidIdentifierStack.getFromVariant(variant, toDroplets(drained));
		if (doDrain) {
			amount -= toDroplets(drained);
			if (amount <= 0) {
				amount = 0;
				variant = FluidVariant.blank();
			}
		}
		return result;
	}

	@Nullable
	public FluidIdentifierStack drain(@Nullable FluidIdentifierStack resource, boolean doDrain) {
		if (isEmpty() || resource == null || !variant.equals(resource.makeFluidVariant())) return null;
		return drain(resource.getAmount(), doDrain);
	}

	public void readFromNBT(CompoundTag nbt) {
		setFluid(FluidIdentifierStack.readFromNBT(nbt));
	}

	public CompoundTag writeToNBT(CompoundTag nbt) {
		FluidIdentifierStack stack = getFluid();
		return stack == null ? nbt : stack.writeToNBT(nbt);
	}
}
