package logisticspipes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.Unpooled;

/**
 * Single multiplexed payload type for all LogisticsPipes packets.
 * LP uses a custom short-discriminator binary format; rather than registering each of
 * its ~60 packet types separately, one payload wraps the full LP binary stream.
 *
 * Wire format (written by {@link PacketHandler#fillByteBuf}):
 *   short  — packet ID (index into PacketHandler.packetlist)
 *   int    — debug ID
 *   ...    — LPDataOutput-encoded packet body
 */
public final class LPPacketPayload implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("logisticspipes", "packet");
    public static final CustomPacketPayload.Type<LPPacketPayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, LPPacketPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), LPPacketPayload::decode);

    private final FriendlyByteBuf data;

    private LPPacketPayload(FriendlyByteBuf data) {
        this.data = data;
    }

    /**
     * Creates a payload for sending (the buffer is ready to write).
     */
    public static LPPacketPayload of(FriendlyByteBuf data) {
        return new LPPacketPayload(data);
    }

    /**
     * Decoder factory — called by NeoForge upon receipt.
     * Signature: {@code Function<FriendlyByteBuf, LPPacketPayload>}.
     * Copies all remaining bytes so the returned payload owns its buffer.
     */
    public static LPPacketPayload decode(FriendlyByteBuf incoming) {
        FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer(incoming.readableBytes()));
        copy.writeBytes(incoming);
        return new LPPacketPayload(copy);
    }

    /** Returns the raw payload buffer. The handler should call {@link #release()} when done. */
    public FriendlyByteBuf getData() {
        return data;
    }

    /** Releases the underlying buffer — call after the handler finishes processing. */
    public void release() {
        if (data.refCnt() > 0) {
            data.release();
        }
    }

    // ── CustomPacketPayload ──────────────────────────────────────────────────

    public void write(FriendlyByteBuf buf) {
        buf.writeBytes(data, data.readerIndex(), data.readableBytes());
    }

    @Override
    public CustomPacketPayload.Type<LPPacketPayload> type() {
        return TYPE;
    }
}
