package logisticspipes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Single multiplexed payload type for all LogisticsPipes packets.
 * LP uses a custom short-discriminator binary format; rather than registering each of
 * its packet types separately, one payload wraps the full LP binary stream.
 *
 * Wire format (written by {@link PacketHandler#fillByteBuf}):
 *   short  — packet ID (index into PacketHandler.packetlist)
 *   int    — debug ID
 *   ...    — LPDataOutput-encoded packet body
 */
public record LPPacketPayload(byte[] data) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<LPPacketPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("logisticspipes", "packet"));

	public static final StreamCodec<FriendlyByteBuf, LPPacketPayload> CODEC = StreamCodec.of(
			(buf, payload) -> buf.writeBytes(payload.data),
			buf -> {
				byte[] bytes = new byte[buf.readableBytes()];
				buf.readBytes(bytes);
				return new LPPacketPayload(bytes);
			});

	@Override
	public CustomPacketPayload.Type<LPPacketPayload> type() {
		return TYPE;
	}
}
