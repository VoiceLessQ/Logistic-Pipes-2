package logisticspipes.items;

import net.minecraft.world.item.Item;
import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

import logisticspipes.interfaces.IItemAdvancedExistance;

public class LogisticsBrokenItem extends LogisticsItem implements IItemAdvancedExistance {

	private static final String PREFIX = "tooltip.brokenItem.";

	@Override
	public boolean canExistInNormalInventory(@Nonnull ItemStack stack) {
		return false;
	}

	@Override
	public boolean canExistInWorld(@Nonnull ItemStack stack) {
		return false;
	}

//	@Override
//	@Environment(EnvType.CLIENT)
//	public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltipConsumer, net.minecraft.world.item.TooltipFlag flagIn) {
//		java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
//		tooltip.add(net.minecraft.network.chat.Component.literal(" - " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "1")));
//		tooltip.add(net.minecraft.network.chat.Component.literal(" - " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "2")));
//		tooltip.add(net.minecraft.network.chat.Component.literal("    " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "3")));
//
//		tooltip.forEach(tooltipConsumer);
//	}
}
