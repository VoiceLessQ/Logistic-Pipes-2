package logisticspipes.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;

import logisticspipes.utils.item.ItemIdentifierStack;

public class FluidIdentifierStack implements Comparable<FluidIdentifierStack> {

	private Object ccType;
	private final FluidIdentifier _fluid;
	private int milliBuckets;

	public FluidIdentifierStack(FluidIdentifier fluid, int milliBuckets) {
		_fluid = fluid;
		setAmount(milliBuckets);
	}

	/** {@code droplets} is a Transfer API amount; LP counts whole millibuckets. */
	public static FluidIdentifierStack getFromVariant(FluidVariant variant, long droplets) {
		FluidIdentifier fluid = FluidIdentifier.get(variant);
		if (fluid == null) return null;
		return new FluidIdentifierStack(fluid, LPFluidTank.toMb(droplets));
	}

	/** Fluid held by an item through the Transfer API (buckets, tanks); null if none. */
	public static FluidIdentifierStack getFromContainer(ItemStack stack) {
		if (stack.isEmpty()) return null;
		Storage<FluidVariant> storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
		if (storage == null) return null;
		for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
			return getFromVariant(view.getResource(), view.getAmount());
		}
		return null;
	}

	public static FluidIdentifierStack getFromStack(ItemIdentifierStack stack) {
		FluidIdentifier fluid = FluidIdentifier.get(stack);
		if (fluid == null) return null;
		return new FluidIdentifierStack(fluid, stack.getStackSize());
	}

	public FluidIdentifier getFluid() {
		return _fluid;
	}

	/**
	 * @return the stackSize
	 */
	public int getAmount() {
		return milliBuckets;
	}

	public void setAmount(int milliBuckets) {
		this.milliBuckets = milliBuckets;
	}

	public void lowerAmount(int milliBuckets) {
		this.milliBuckets -= milliBuckets;
	}

	public void raiseAmount(int milliBuckets) {
		this.milliBuckets += milliBuckets;
	}

	public FluidVariant makeFluidVariant() {
		return _fluid.makeFluidVariant();
	}

	/** Same keys as Forge's FluidStack NBT ("FluidName", "Amount", "Tag"). */
	public CompoundTag writeToNBT(CompoundTag nbt) {
		nbt.putString("FluidName", _fluid.fluidID);
		nbt.putInt("Amount", milliBuckets);
		if (_fluid.tag != null) nbt.put("Tag", _fluid.tag.copy());
		return nbt;
	}

	public static FluidIdentifierStack readFromNBT(CompoundTag nbt) {
		FluidIdentifier ident = nbt.getString("FluidName")
				.map(id -> FluidIdentifier.get(BuiltInRegistries.FLUID.getValue(Identifier.parse(id)), nbt.getCompound("Tag").orElse(null), null))
				.orElse(null);
		return ident == null ? null : new FluidIdentifierStack(ident, nbt.getIntOr("Amount", 0));
	}

	public boolean isEmpty() {
		return milliBuckets <= 0;
	}

	public long getDroplets() {
		return LPFluidTank.toDroplets(milliBuckets);
	}

	public FluidIdentifierStack copy() {
		return new FluidIdentifierStack(_fluid, milliBuckets);
	}

	@Override
	public int compareTo(FluidIdentifierStack o) {
		int c = _fluid.compareTo(o._fluid);
		if (c == 0) {
			return Integer.compare(getAmount(), o.getAmount());
		}
		return c;
	}
}
