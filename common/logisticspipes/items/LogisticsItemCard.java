package logisticspipes.items;

import net.minecraft.world.item.Item;
import logisticspipes.utils.item.CustomDataUtil;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;



import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.proxy.SimpleServiceLocator;
import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsItemCard extends LogisticsItem implements IItemAdvancedExistance {

	public static final int FREQ_CARD = 0;
	public static final int SEC_CARD = 1;

	// 1.12 used the damage value; the item has no durability now, so the type lives in NBT.
	public static int getCardType(@Nonnull ItemStack stack) {
		CompoundTag tag = CustomDataUtil.getTag(stack);
		return tag != null ? tag.getIntOr("cardType", FREQ_CARD) : FREQ_CARD;
	}

	public static ItemStack makeSecurityCard(UUID id, int count) {
		ItemStack stack = new ItemStack(logisticspipes.LPItems.itemCard.get(), count);
		CompoundTag tag = new CompoundTag();
		tag.putInt("cardType", SEC_CARD);
		tag.putString("UUID", id.toString());
		CustomDataUtil.setTag(stack, tag);
		return stack;
	}

	public LogisticsItemCard() {
		// hasSubtypes removed in 1.20.1 — item variants handled via DamageValue or separate items
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltipConsumer, net.minecraft.world.item.TooltipFlag flagIn) {
		java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
		super.appendHoverText(stack, context, display, tooltipConsumer, flagIn);
		if (!CustomDataUtil.hasTag(stack)) {
			tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.logisticsItemCard")));
		} else {
			final CompoundTag tag = CustomDataUtil.getOrCreateTag(stack);
			if (tag.contains("UUID")) {
				if (LogisticsItemCard.getCardType(stack) == LogisticsItemCard.FREQ_CARD) {
					tooltip.add(net.minecraft.network.chat.Component.literal("Freq. Card"));
				} else if (LogisticsItemCard.getCardType(stack) == LogisticsItemCard.SEC_CARD) {
					tooltip.add(net.minecraft.network.chat.Component.literal("Sec. Card"));
				}
				if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
					tooltip.add(net.minecraft.network.chat.Component.literal("Id: " + tag.getString("UUID")));
					if (LogisticsItemCard.getCardType(stack) == LogisticsItemCard.SEC_CARD) {
						UUID id = UUID.fromString(tag.getStringOr("UUID", ""));
						tooltip.add(net.minecraft.network.chat.Component.literal("Authorization: " + (SimpleServiceLocator.securityStationManager.isAuthorized(id) ? "Authorized" : "Unauthorized")));
					}
				}
			}
		}
		tooltip.forEach(tooltipConsumer);
	}

	// getShareTag() removed in 1.20 — NBT always shared now
	@Deprecated public boolean getShareTag__REMOVED() {
		return true;
	}


	@Override
	public boolean canExistInNormalInventory(@Nonnull ItemStack stack) {
		return true;
	}

	@Override
	public boolean canExistInWorld(@Nonnull ItemStack stack) {
		return LogisticsItemCard.getCardType(stack) != LogisticsItemCard.SEC_CARD;
	}
}
