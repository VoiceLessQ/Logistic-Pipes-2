package logisticspipes.items;

import net.minecraft.client.gui.screens.Screen;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;



import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.proxy.SimpleServiceLocator;
import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsItemCard extends LogisticsItem implements IItemAdvancedExistance {

	public static final int FREQ_CARD = 0;
	public static final int SEC_CARD = 1;

	// 1.12 used the damage value; the item has no durability now, so the type lives in NBT.
	public static int getCardType(@Nonnull ItemStack stack) {
		CompoundTag tag = logisticspipes.utils.item.StackTag.getTag(stack);
		return tag != null && tag.contains("cardType") ? tag.getInt("cardType") : FREQ_CARD;
	}

	public static ItemStack makeSecurityCard(UUID id, int count) {
		ItemStack stack = new ItemStack(logisticspipes.LPItems.itemCard.get(), count);
		CompoundTag tag = new CompoundTag();
		tag.putInt("cardType", SEC_CARD);
		tag.putString("UUID", id.toString());
		logisticspipes.utils.item.StackTag.setTag(stack, tag);
		return stack;
	}

	public LogisticsItemCard() {
		// hasSubtypes removed in 1.20.1 — item variants handled via DamageValue or separate items
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, net.minecraft.world.item.Item.TooltipContext worldIn, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
		if (!logisticspipes.utils.item.StackTag.hasTag(stack)) {
			tooltip.add(net.minecraft.network.chat.Component.literal(TextUtil.translate("tooltip.logisticsItemCard")));
		} else {
			final CompoundTag tag = Objects.requireNonNull(logisticspipes.utils.item.StackTag.getTag(stack));
			if (tag.contains("UUID")) {
				if (LogisticsItemCard.getCardType(stack) == LogisticsItemCard.FREQ_CARD) {
					tooltip.add(net.minecraft.network.chat.Component.literal("Freq. Card"));
				} else if (LogisticsItemCard.getCardType(stack) == LogisticsItemCard.SEC_CARD) {
					tooltip.add(net.minecraft.network.chat.Component.literal("Sec. Card"));
				}
				if (Screen.hasShiftDown()) {
					tooltip.add(net.minecraft.network.chat.Component.literal("Id: " + tag.getString("UUID")));
					if (LogisticsItemCard.getCardType(stack) == LogisticsItemCard.SEC_CARD) {
						UUID id = UUID.fromString(tag.getString("UUID"));
						tooltip.add(net.minecraft.network.chat.Component.literal("Authorization: " + (SimpleServiceLocator.securityStationManager.isAuthorized(id) ? "Authorized" : "Unauthorized")));
					}
				}
			}
		}
	}

	// getShareTag() removed in 1.20 — NBT always shared now
	@Deprecated public boolean getShareTag__REMOVED() {
		return true;
	}

	@Override
	public int getMaxStackSize(@Nonnull ItemStack stack) {
		return 64;
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
