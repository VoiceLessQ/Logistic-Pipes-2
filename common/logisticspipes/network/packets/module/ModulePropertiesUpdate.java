package logisticspipes.network.packets.module;

import java.util.Objects;
import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.nbt.CompoundTag;

import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.property.PropertyHolder;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ModulePropertiesUpdate extends ModuleCoordinatesPacket {

	@Nonnull
	public CompoundTag tag = new CompoundTag();

	public ModulePropertiesUpdate(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCompoundTag(tag);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		tag = Objects.requireNonNull(input.readCompoundTag(), "read null NBT in ModulePropertiesUpdate");
	}

	@Override
	public ModernPacket template() {
		return new ModulePropertiesUpdate(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsModule module = this.getLogisticsModule(player, LogisticsModule.class);
		if (module == null) {
			return;
		}

		// sync updated properties
		module.readFromNBT(tag);

		if (!getType().isInWorld() && player.containerMenu instanceof InventoryMenu) {
			// sync slot in player inventory and mark player inventory dirty
			ItemModuleInformationManager.saveInformation(player.getInventory().getNonEquipmentItems().get(getPositionInt()), module);
			player.getInventory().setChanged();
		}

		MainProxy.runOnServer(player.level(), () -> () -> {
			// resync client; always
			MainProxy.sendPacketToPlayer(fromPropertyHolder(module).setModulePos(module), player);
		});
	}

	@Nonnull
	public static ModuleCoordinatesPacket fromPropertyHolder(PropertyHolder holder) {
		final ModulePropertiesUpdate packet = PacketHandler.getPacket(ModulePropertiesUpdate.class);
		PropertyHolder.writeToNBT(packet.tag, holder);
		return packet;
	}

}
