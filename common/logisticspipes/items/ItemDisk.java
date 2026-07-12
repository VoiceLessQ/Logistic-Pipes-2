package logisticspipes.items;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class ItemDisk extends LogisticsItem {

	@Override
	public int getMaxStackSize(@Nonnull ItemStack stack) {
		return 1;
	}

	@Override
	public void appendHoverText(@Nonnull ItemStack stack, net.minecraft.world.item.Item.TooltipContext worldIn, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flagIn) {
		if (!stack.isEmpty() && logisticspipes.utils.item.StackTag.hasTag(stack)) {
			final CompoundTag tag = Objects.requireNonNull(logisticspipes.utils.item.StackTag.getTag(stack));
			if (tag.contains("name")) {
				String name = "\u00a78" + tag.getString("name");
				tooltip.add(net.minecraft.network.chat.Component.literal(name));
			}
		}
	}
}
