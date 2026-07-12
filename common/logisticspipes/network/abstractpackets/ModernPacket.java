package logisticspipes.network.abstractpackets;

import java.util.Collections;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.packetcontent.IPacketContent;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class ModernPacket {

	/*
	@Getter
	protected String channel;
	 */
	@Getter
	private final int id;
	protected int leftRetries = 5;
	@Getter
	@Setter
	private boolean isChunkDataPacket;
	@Getter
	@Setter
	private boolean compressable;
	//@Getter
	//private byte[] data = null;
	@Getter
	@Setter
	private int debugId = 0;
	@Getter
	private ResourceLocation dimension = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

	public List<IPacketContent<?>> content = Collections.emptyList();

	public ModernPacket(int id) {
		this.id = id;
	}

	public ModernPacket setDimension(ResourceLocation dimension) {
		this.dimension = dimension;
		return this;
	}

	public ModernPacket setDimension(Level world) {
		this.dimension = world.dimension().location();
		return this;
	}

	public void readData(LPDataInput input) {
		ResourceLocation rl = input.readResourceLocation();
		dimension = rl != null ? rl : ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
		content.forEach(it -> it.readData(input));
	}

	public abstract void processPacket(Player player);

	public void writeData(LPDataOutput output) {
		output.writeResourceLocation(dimension);
		content.forEach(it -> it.writeData(output));
	}

	public abstract ModernPacket template();

	public boolean retry() {
		return leftRetries-- > 0;
	}
}
