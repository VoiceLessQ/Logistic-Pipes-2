package logisticspipes.routing.channels;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import logisticspipes.utils.PlayerIdentifier;

@Data
@AllArgsConstructor
public class ChannelInformation {

	public enum AccessRights {
		PRIVATE,
		SECURED,
		PUBLIC
	}

	private String name;
	@NonNull
	private final UUID channelIdentifier;
	@NonNull
	private PlayerIdentifier owner;
	@NonNull
	private AccessRights rights;
	private UUID responsibleSecurityID;

	public ChannelInformation(CompoundTag nbt) {
		name = nbt.getStringOr("name", "");
		channelIdentifier = UUID.fromString(nbt.getStringOr("channelIdentifier", ""));
		owner = PlayerIdentifier.readFromNBT(nbt, "owner");
		rights = AccessRights.values()[nbt.getIntOr("rights", 0)];
		if (nbt.contains("responsibleSecurityID")) {
			responsibleSecurityID = UUID.fromString(nbt.getStringOr("responsibleSecurityID", ""));
		} else {
			responsibleSecurityID = null;
		}
	}

	public CompoundTag writeToNBT(CompoundTag compound) {
		compound.putString("name", name);
		compound.putString("channelIdentifier", channelIdentifier.toString());
		owner.writeToNBT(compound, "owner");
		compound.putInt("rights", rights.ordinal());
		if (responsibleSecurityID != null) {
			compound.putString("responsibleSecurityID", responsibleSecurityID.toString());
		}

		return compound;
	}
}
