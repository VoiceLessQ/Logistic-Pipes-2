package network.rs485.logisticspipes.util.items;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import logisticspipes.utils.TagUtil;

public class ItemStackLoader {

	@Nonnull
	public static ItemStack loadAndFixItemStackFromNBT(CompoundTag nbt) {
		return TagUtil.loadStack(nbt);
	}
}
