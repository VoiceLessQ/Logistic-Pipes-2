package logisticspipes.items;

import logisticspipes.utils.item.CustomDataUtil;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import network.rs485.logisticspipes.util.TextUtil;

public class ItemLogisticsProgrammer extends LogisticsItem {

	public static final String RECIPE_TARGET = "LogisticsRecipeTarget";

	public ItemLogisticsProgrammer() {
		super(logisticspipes.LPRegistries.itemProperties());
	}

	/** The programmer survives crafting with its target intact (Fabric's stack-aware remainder hook). */
	@Override
	public net.minecraft.world.item.ItemStackTemplate getCraftingRemainder(@Nonnull ItemStack itemStack) {
		ItemStack remainder = new ItemStack(this);
		if (CustomDataUtil.hasTag(itemStack)) {
			CustomDataUtil.setTag(remainder, CustomDataUtil.getTag(itemStack));
		}
		return net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(remainder);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltipConsumer, net.minecraft.world.item.TooltipFlag flagIn) {
		java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
		if (!stack.isEmpty()) {
			if (CustomDataUtil.hasTag(stack)) {
				CompoundTag nbt = CustomDataUtil.getTag(stack);
				String target = nbt.getStringOr(RECIPE_TARGET, "");
				if (!target.isEmpty()) {
					Item targetItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse(target));
					if (targetItem instanceof ItemModule) {
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForModule")));
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate(targetItem.getDescriptionId())));
					} else if (targetItem instanceof ItemUpgrade) {
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForUpgrade")));
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate(targetItem.getDescriptionId())));
					} else if (targetItem instanceof ItemLogisticsPipe) {
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForPipe")));
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate(targetItem.getDescriptionId())));
					} else {
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForUnknown.1")));
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForUnknown.2")));
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForUnknown.3")));
					}
				}
			} else {
				tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForUnknown.1")));
				tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForUnknown.2")));
				tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForUnknown.3")));
			}
		}
		super.appendHoverText(stack, context, display, tooltipConsumer, flagIn);
		tooltip.forEach(tooltipConsumer);
	}
}
