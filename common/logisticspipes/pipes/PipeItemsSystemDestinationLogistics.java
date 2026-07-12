package logisticspipes.pipes;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import logisticspipes.LogisticsPipes;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;

public class PipeItemsSystemDestinationLogistics extends CoreRoutedPipe {

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "Freq Slot", 1);

	public PipeItemsSystemDestinationLogistics(Item item) {
		super(item);
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_DESTINATION_TEXTURE;
	}

	@Override
	public @Nullable LogisticsModule getLogisticsModule() {
		return null;
	}

	public Object getTargetUUID() {
		final ItemIdentifierStack itemIdent = inv.getIDStackInSlot(0);
		if (itemIdent == null) {
			return null;
		}
		final ItemStack stack = itemIdent.makeNormalStack();
		if (!logisticspipes.utils.item.StackTag.hasTag(stack)) {
			return null;
		}
		if (!Objects.requireNonNull(logisticspipes.utils.item.StackTag.getTag(stack)).contains("UUID")) {
			return null;
		}
		spawnParticle(Particles.WhiteParticle, 2);
		return UUID.fromString(logisticspipes.utils.item.StackTag.getTag(stack).getString("UUID"));
	}

	@Override
	public void onAllowedRemoval() {
		dropFreqCard();
	}

	@Override
	public void writeToNBT(CompoundTag nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		inv.writeToNBT(nbttagcompound);
	}

	@Override
	public void readFromNBT(CompoundTag nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		inv.readFromNBT(nbttagcompound);
	}

	private void dropFreqCard() {
		final ItemIdentifierStack itemident = inv.getIDStackInSlot(0);
		if (itemident == null) {
			return;
		}
		ItemEntity item = new ItemEntity(getWorld(), getX(), getY(), getZ(), itemident.makeNormalStack());
		getWorld().addFreshEntity(item);
		inv.clearInventorySlotContents(0);
	}

	@Override
	public void onWrenchClicked(Player entityplayer) {
		logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.pipe.FreqCardGui.class)
				.setPosX(getX()).setPosY(getY()).setPosZ(getZ())
				.open(entityplayer);
	}
}
