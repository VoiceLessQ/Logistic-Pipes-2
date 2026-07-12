package logisticspipes.utils.item;

import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 1.20.5+ shim for the removed ItemStack NBT accessors. LP's identity model is
 * whole-tag based; the tag now lives in the CUSTOM_DATA component.
 * NOTE: getTag returns a COPY; mutate-then-setTag, never mutate the result in place.
 */
public final class StackTag {

	private StackTag() {}

	public static boolean hasTag(ItemStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA);
	}

	@Nullable
	public static CompoundTag getTag(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag();
	}

	public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
		if (tag == null || tag.isEmpty()) {
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}

	public static boolean hasTag(net.neoforged.neoforge.fluids.FluidStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA);
	}

	@Nullable
	public static CompoundTag getTag(net.neoforged.neoforge.fluids.FluidStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag();
	}

	public static void setTag(net.neoforged.neoforge.fluids.FluidStack stack, @Nullable CompoundTag tag) {
		if (tag == null || tag.isEmpty()) {
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}
}
