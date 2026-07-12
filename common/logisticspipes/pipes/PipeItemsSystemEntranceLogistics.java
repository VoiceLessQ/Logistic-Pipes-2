package logisticspipes.pipes;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;

import logisticspipes.LogisticsPipes;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.EntrencsTransport;
import logisticspipes.utils.item.ItemIdentifierInventory;

public class PipeItemsSystemEntranceLogistics extends CoreRoutedPipe {

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "Freq Slot", 1);

	public PipeItemsSystemEntranceLogistics(Item item) {
		super(new EntrencsTransport(), item);
		((EntrencsTransport) transport).pipe = this;
	}

	public UUID getLocalFreqUUID() {
		if (inv.getItem(0) == null) {
			return null;
		}
		if (!logisticspipes.utils.item.StackTag.hasTag(inv.getItem(0))) {
			return null;
		}
		if (!logisticspipes.utils.item.StackTag.getTag(inv.getItem(0)).contains("UUID")) {
			return null;
		}
		spawnParticle(Particles.WhiteParticle, 2);
		return UUID.fromString(logisticspipes.utils.item.StackTag.getTag(inv.getItem(0)).getString("UUID"));
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_ENTRANCE_TEXTURE;
	}

	@Override
	public @Nullable LogisticsModule getLogisticsModule() {
		return null;
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

	@Override
	public void onAllowedRemoval() {
		dropFreqCard();
	}

	private void dropFreqCard() {
		if (inv.getItem(0) == null) {
			return;
		}
		ItemEntity item = new ItemEntity(getWorld(), getX(), getY(), getZ(), inv.getItem(0));
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
