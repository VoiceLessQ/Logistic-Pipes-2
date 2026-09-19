package logisticspipes.items;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import network.rs485.logisticspipes.util.TextUtil;

public class ItemLogisticsProgrammer extends LogisticsItem {

	public static final String RECIPE_TARGET = "LogisticsRecipeTarget";

	public ItemLogisticsProgrammer() {
		super(new Item.Properties().stacksTo(1));
	}

	// LP1 setContainerItem(this): the programmer stays in the grid, program included.
	@Override
	public boolean hasCraftingRemainingItem(@Nonnull ItemStack itemStack) {
		return true;
	}

	@Nonnull
	@Override
	public ItemStack getCraftingRemainingItem(@Nonnull ItemStack itemStack) {
		return itemStack.copyWithCount(1);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, net.minecraft.world.item.Item.TooltipContext worldIn, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flagIn) {
		if (!stack.isEmpty()) {
			if (logisticspipes.utils.item.StackTag.hasTag(stack)) {
				CompoundTag nbt = logisticspipes.utils.item.StackTag.getTag(stack);
				String target = nbt.getString(RECIPE_TARGET);
				if (!target.isEmpty()) {
					Item targetItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(target));
					if (targetItem instanceof ItemModule) {
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForModule")));
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate(targetItem.getDescriptionId() + ".name")));
					} else if (targetItem instanceof ItemUpgrade) {
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForUpgrade")));
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate(targetItem.getDescriptionId() + ".name")));
					} else if (targetItem instanceof ItemLogisticsPipe) {
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.programmerForPipe")));
						tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate(targetItem.getDescriptionId() + ".name")));
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
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
	}
}
