package logisticspipes.pipes;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;


import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.network.GuiIDs;
import logisticspipes.proxy.MainProxy;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import network.rs485.logisticspipes.util.items.ItemStackLoader;

public class PipeItemsRequestLogisticsMk2 extends PipeItemsRequestLogistics {

	@Nonnull
	private ItemStack disk = ItemStack.EMPTY;

	public PipeItemsRequestLogisticsMk2(Item item) {
		super(item);
	}

	@Override
	public boolean handleClick(Player entityplayer, SecuritySettings settings) {
		//allow using upgrade manager
		if (MainProxy.isPipeControllerEquipped(entityplayer) && !(entityplayer.isCrouching())) {
			return false;
		}
		if (MainProxy.isServer(getWorld())) {
			if (settings == null || settings.openGui) {
				openGui(entityplayer);
			} else {
				entityplayer.sendSystemMessage(Component.translatable("lp.chat.permissiondenied"));
			}
		}
		return true;
	}

	@Override
	public void openGui(Player entityplayer) {
		boolean flag = true;
		if (disk.isEmpty()) {
			if (!entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).getItem().equals(LPItems.disk.get())) {
				disk = entityplayer.getItemBySlot(EquipmentSlot.MAINHAND);
				entityplayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				flag = false;
			}
		}
		if (flag) {
			logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.pipe.NormalMk2OrdererGui.class)
					.setPosX(getX()).setPosY(getY()).setPosZ(getZ())
					.open(entityplayer);
		}
	}

	@Override
	public void writeToNBT(CompoundTag nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		if (!disk.isEmpty()) {
			CompoundTag itemNBT = new CompoundTag();
			disk.save(logisticspipes.utils.RegistryAccessUtil.registries(), itemNBT);
			nbttagcompound.put("Disk", itemNBT);
		}
	}

	@Override
	public void readFromNBT(CompoundTag nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		if (nbttagcompound.contains("Disk")) {
			CompoundTag item = nbttagcompound.getCompound("Disk");
			disk = ItemStackLoader.loadAndFixItemStackFromNBT(item);
		}
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_REQUESTERMK2_TEXTURE;
	}

	@Nonnull
	public ItemStack getDisk() {
		return disk;
	}

	@Override
	public void onAllowedRemoval() {
		if (MainProxy.isServer(getWorld())) {
			dropDisk();
		}
	}

	public void dropDisk() {
		if (!disk.isEmpty()) {
			ItemEntity item = new ItemEntity(getWorld(), getX(), getY(), getZ(), disk);
			getWorld().addFreshEntity(item);
			disk = ItemStack.EMPTY;
		}
	}

	public void setDisk(@Nonnull ItemStack itemstack) {
		disk = itemstack;
	}
}
