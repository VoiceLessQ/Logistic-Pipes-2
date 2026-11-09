package logisticspipes.utils.item;

import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CustomDataUtil {

	private CustomDataUtil() {}

	/** Copy of the stack's custom data; changes only stick after {@link #setTag}. */
	@Nullable
	public static CompoundTag getTag(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag();
	}

	public static CompoundTag getOrCreateTag(ItemStack stack) {
		CompoundTag tag = getTag(stack);
		return tag == null ? new CompoundTag() : tag;
	}

	public static boolean hasTag(ItemStack stack) {
		return stack.has(DataComponents.CUSTOM_DATA);
	}

	public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
		if (tag == null) {
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
		}
	}
}
