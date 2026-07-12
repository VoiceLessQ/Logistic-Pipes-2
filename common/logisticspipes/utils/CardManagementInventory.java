package logisticspipes.utils;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import logisticspipes.items.ItemModule;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.utils.item.ItemIdentifierInventory;

public class CardManagementInventory implements Container {

	ItemIdentifierInventory inv = new ItemIdentifierInventory(4, "", 1);

	@Override
	public int getContainerSize() {
		return 10;
	}

	@Override
	public boolean isEmpty() {
		return inv.isEmpty();
	}

	@Nonnull
	@Override
	public ItemStack getItem(int i) {
		if (i > -1 && i < 4) {
			return inv.getItem(i);
		}
		ItemStack card = inv.getItem(3);
		if (!card.isEmpty()) {
			CompoundTag nbt = logisticspipes.utils.item.StackTag.getTag(card);
			if (nbt == null) {
				nbt = new CompoundTag();
			}
			CompoundTag colors = nbt.getCompound("colors");
			int slot = i - 4;

			int colorCode;
			if (colors.contains("color:" + slot)) {
				colorCode = colors.getInt("color:" + slot);
			} else {
				colors.putInt("color:" + slot, 16);
				colorCode = 16;
			}

			MinecraftColor color = MinecraftColor.values()[colorCode];

			nbt.put("colors", colors);
			logisticspipes.utils.item.StackTag.setTag(card, nbt);
			inv.setItem(3, card);

			return color.getItemStack();
		}

		return ItemStack.EMPTY;
	}

	@Nonnull
	@Override
	public ItemStack removeItem(int i, int j) {
		if (i > -1 && i < 4) {
			return inv.removeItem(i, j);
		}
		return ItemStack.EMPTY;
	}

	@Nonnull
	@Override
	public ItemStack removeItemNoUpdate(int i) {
		if (i > -1 && i < 4) {
			return inv.removeItemNoUpdate(i);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int i, @Nonnull ItemStack itemstack) {
		if (i > -1 && i < 4) {
			if (i == 0 && !itemstack.isEmpty() && !inv.getItem(1).isEmpty() && inv.getItem(2).isEmpty() && inv.getItem(1).getDamageValue() == itemstack.getDamageValue()) {
				logisticspipes.utils.item.StackTag.setTag(itemstack, logisticspipes.utils.item.StackTag.getTag(inv.getItem(1)));
				inv.setItem(2, itemstack);
				return;
			}
			if (i == 1 && !itemstack.isEmpty() && !inv.getItem(0).isEmpty() && inv.getItem(2).isEmpty() && inv.getItem(0).getDamageValue() == itemstack.getDamageValue()) {
				logisticspipes.utils.item.StackTag.setTag(itemstack, logisticspipes.utils.item.StackTag.getTag(inv.getItem(0)));
				inv.setItem(2, itemstack);
				return;
			}
			inv.setItem(i, itemstack);
			return;
		}
		ItemStack card = inv.getItem(3);
		if (!card.isEmpty()) {
			CompoundTag nbt = logisticspipes.utils.item.StackTag.getTag(card);
			if (nbt == null) {
				nbt = new CompoundTag();
			}
			CompoundTag colors = nbt.getCompound("colors");
			int slot = i - 4;
			colors.putInt("color:" + slot, MinecraftColor.getColor(itemstack).ordinal());
			nbt.put("colors", colors);
			logisticspipes.utils.item.StackTag.setTag(card, nbt);
			inv.setItem(3, card);
		}
	}

	@Nonnull
	public String getName() {
		return "Card Managment Inventory";
	}

	@Nonnull
	public Component getDisplayName() {
		return Component.literal("");
	}

	public boolean hasCustomName() {
		return false;
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public boolean stillValid(@Nonnull Player entityplayer) {
		return true;
	}

	@Override
	public void setChanged() {}

	@Override
	public void startOpen(@Nonnull Player player) {}

	@Override
	public void stopOpen(@Nonnull Player player) {}

	@Override
	public boolean canPlaceItem(int i, @Nonnull ItemStack itemstack) {
		if (itemstack.isEmpty()) {
			return false;
		}
		if (i == 0 || i == 1) {
			return itemstack.getItem() instanceof ItemModule;
		} else if (i == 3) {
			return itemstack.getItem() instanceof LogisticsItemCard;
		}
		return false;
	}

	public void close(Player player, int x, int y, int z) {
		inv.dropContents(player.level(), x, y, z);
	}

	public int getField(int id) {
		return 0;
	}

	public void setField(int id, int value) {

	}

	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clearContent() {

	}
}
