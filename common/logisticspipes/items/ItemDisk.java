package logisticspipes.items;

import net.minecraft.world.item.Item;
import logisticspipes.utils.item.CustomDataUtil;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class ItemDisk extends LogisticsItem {

	public ItemDisk() {
		super(logisticspipes.LPRegistries.itemProperties().stacksTo(1));
	}

	@Override
	public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltipConsumer, net.minecraft.world.item.TooltipFlag flagIn) {
		java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
		if (!stack.isEmpty() && CustomDataUtil.hasTag(stack)) {
			final CompoundTag tag = CustomDataUtil.getOrCreateTag(stack);
			if (tag.contains("name")) {
				String name = "\u00a78" + tag.getString("name");
				tooltip.add(net.minecraft.network.chat.Component.literal(name));
			}
		}
		tooltip.forEach(tooltipConsumer);
	}
}
