package logisticspipes.routing.channels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.ChannelInformationPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.PlayerIdentifier;

public class ChannelManager implements IChannelManager {

	private static final String DATA_NAME = LPConstants.LP_MOD_ID + "_ChannelManager_SavedData";
	private ChannelSavedData savedData;

	public ChannelManager(@Nonnull Level world) {
		if (world instanceof ServerLevel) {
			savedData = ((ServerLevel) world).getDataStorage().computeIfAbsent(
					new net.minecraft.world.level.saveddata.SavedData.Factory<>(ChannelSavedData::new, ChannelSavedData::load), DATA_NAME
			);
		} else {
			savedData = new ChannelSavedData();
		}
	}

	@Override
	public List<ChannelInformation> getChannels() {
		return Collections.unmodifiableList(savedData.channels);
	}

	private boolean isChannelAllowedFor(ChannelInformation channel, Player player) {
		switch (channel.getRights()) {
			case PUBLIC:
				return true;
			case SECURED:
				final UUID secUUID = channel.getResponsibleSecurityID();
				final LogisticsSecurityTileEntity station = SimpleServiceLocator.securityStationManager.getStation(secUUID);
				if (station != null) {
					final SecuritySettings settings = station.getSecuritySettingsForPlayer(player, false);
					if (settings != null) {
						return settings.accessRoutingChannels;
					}
				}
			case PRIVATE:
				return channel.getOwner().equals(PlayerIdentifier.get(player));
		}
		return false;
	}

	@Override
	public List<ChannelInformation> getAllowedChannels(Player player) {
		return Collections.unmodifiableList(savedData.channels.stream().filter(channel -> isChannelAllowedFor(channel, player)).collect(Collectors.toList()));
	}

	@Override
	public ChannelInformation createNewChannel(String name, PlayerIdentifier owner, ChannelInformation.AccessRights rights, UUID responsibleSecurityID) {
		ChannelInformation channel = new ChannelInformation(name, UUID.randomUUID(), owner, rights, responsibleSecurityID);
		savedData.channels.add(channel);
		savedData.setDirty();
		sendUpdatePacketToClients(channel);
		return channel;
	}

	@Override
	public void updateChannelName(UUID channelIdentifier, String newName) {
		savedData.channels.stream().filter(channel -> channel.getChannelIdentifier().equals(channelIdentifier)).forEach(channel -> {
			channel.setName(newName);
			sendUpdatePacketToClients(channel);
		});
		savedData.setDirty();
	}

	@Override
	public void updateChannelRights(UUID channelIdentifier, ChannelInformation.AccessRights rights, UUID responsibleSecurityID) {
		savedData.channels.stream().filter(channel -> channel.getChannelIdentifier().equals(channelIdentifier)).forEach(channel -> {
			channel.setRights(rights);
			channel.setResponsibleSecurityID(responsibleSecurityID);
			sendUpdatePacketToClients(channel);
		});
		savedData.setDirty();
	}

	@Override
	public void removeChannel(UUID channelIdentifier) {
		Optional<ChannelInformation> optChannel = savedData.channels.stream().filter(channel -> channel.getChannelIdentifier().equals(channelIdentifier)).findFirst();
		savedData.channels.removeIf(channel -> channel.getChannelIdentifier().equals(channelIdentifier));
		optChannel.ifPresent(channelInformation -> sendUpdatePacketToClients(new ChannelInformation(null, channelIdentifier, channelInformation.getOwner(), channelInformation.getRights(), null)));
		savedData.setDirty();
	}

	public void setChanged() {
		savedData.setDirty();
	}

	private void sendUpdatePacketToClients(ChannelInformation channel) {
		MainProxy.sendToAllPlayers(PacketHandler.getPacket(ChannelInformationPacket.class).setInformation(channel).setTargeted(false).setCompressable(true));
	}

	public static class ChannelSavedData extends SavedData {

		List<ChannelInformation> channels = new ArrayList<>();

		public ChannelSavedData() {}

		public static ChannelSavedData load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
			ChannelSavedData data = new ChannelSavedData();
			data.channels = new ArrayList<>();
			for (int i = 0; i < nbt.getInt("dataSize"); i++) {
				data.channels.add(i, new ChannelInformation(nbt.getCompound("data" + i)));
			}
			return data;
		}

		@Nonnull
		@Override
		public CompoundTag save(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries) {
			compound.putInt("dataSize", channels.size());
			for (int i = 0; i < channels.size(); i++) {
				ChannelInformation channel = channels.get(i);
				CompoundTag nbt = new CompoundTag();
				channel.writeToNBT(nbt);
				compound.put("data" + i, nbt);
			}
			return compound;
		}

		public List<ChannelInformation> getChannels() {
			return this.channels;
		}

		public ChannelSavedData setChannels(List<ChannelInformation> channels) {
			this.channels = channels;
			return this;
		}
	}
}
