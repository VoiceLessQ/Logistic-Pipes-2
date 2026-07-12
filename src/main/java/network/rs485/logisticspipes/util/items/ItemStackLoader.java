package network.rs485.logisticspipes.util.items;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public class ItemStackLoader {

	@Nonnull
	public static ItemStack loadAndFixItemStackFromNBT(CompoundTag nbt) {
		return ItemStack.parseOptional(logisticspipes.utils.RegistryAccessUtil.registries(), nbt);
	}
}
