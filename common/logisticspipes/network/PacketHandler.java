package logisticspipes.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static io.netty.buffer.Unpooled.buffer;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.exception.DelayPacketException;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.StaticResolverUtil;
import network.rs485.logisticspipes.util.LPDataIOWrapper;
import network.rs485.logisticspipes.util.LPDataInput;

/**
 * Central packet registry and dispatcher for LogisticsPipes.
 *
 * All LP packets share a single {@link LPPacketPayload} channel multiplexed by a short ID.
 * Registration happens in {@link logisticspipes.LogisticsPipes} via {@code RegisterPayloadsEvent}.
 */
public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final ResourceLocation CHANNEL_ID = LPPacketPayload.ID;

    /** Mod-bus listener; replaces the SimpleChannel registration. Handled on the main thread. */
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playBidirectional(LPPacketPayload.TYPE, LPPacketPayload.STREAM_CODEC, (payload, context) -> {
            try {
                Player player = context.player();
                onPacketData(payload.getData(), player);
            } finally {
                payload.release();
            }
        });
    }

    public static final Map<Integer, StackTraceElement[]> debugMap = new HashMap<>();
    public static List<ModernPacket> packetlist;
    public static Map<Class<? extends ModernPacket>, ModernPacket> packetmap;
    private static int packetDebugID = 1;

    // ── Registration ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static <T extends ModernPacket> T getPacket(Class<T> clazz) {
        T packet = (T) PacketHandler.packetmap.get(clazz).template();
        if (LogisticsPipes.isDEBUG() && MainProxy.proxy.getSide().equals("Client")) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            synchronized (PacketHandler.debugMap) {
                int id = PacketHandler.packetDebugID++;
                PacketHandler.debugMap.put(id, trace);
                packet.setDebugId(id);
            }
        }
        return packet;
    }

    /** Enumerates all ModernPacket subclasses, assigns IDs, and populates packetlist/packetmap. */
    public static void initialize() {
        Set<Class<? extends ModernPacket>> classes = StaticResolverUtil.findClassesByType(ModernPacket.class);
        loadPackets(classes);
        if (PacketHandler.packetmap.isEmpty()) {
            throw new RuntimeException("Cannot load Packet Classes");
        }
    }

    private static void loadPackets(Set<Class<? extends ModernPacket>> classesIn) {
        List<Class<? extends ModernPacket>> classes = classesIn.stream()
                .sorted(Comparator.comparing(Class::getCanonicalName))
                .collect(Collectors.toList());

        // Packet IDs are the index in the sorted class list, so they are IDENTICAL on the client
        // and the dedicated server even if a packet fails to construct on one side (e.g. a class
        // the RuntimeDistCleaner refuses to link). A success-counter would shift every later ID
        // and desync the protocol. Failed slots stay null; the dispatch path guards against that.
        PacketHandler.packetlist = new ArrayList<>(java.util.Collections.nCopies(classes.size(), (ModernPacket) null));
        PacketHandler.packetmap = new HashMap<>(classes.size());

        for (int id = 0; id < classes.size(); id++) {
            Class<? extends ModernPacket> cls = classes.get(id);
            try {
                final ModernPacket instance = cls.getConstructor(int.class).newInstance(id);
                PacketHandler.packetlist.set(id, instance);
                PacketHandler.packetmap.put(cls, instance);
            } catch (Throwable t) {
                LogisticsPipes.log.error("Failed to load packet (id " + id + ") " + cls.getName(), t);
            }
        }
    }

    // ── Binary serialization ─────────────────────────────────────────────────

    /**
     * Writes a ModernPacket into a raw ByteBuf (short id + int debugId + LP body).
     * Used both for network sending and for NBT embedding.
     */
    public static void fillByteBuf(@Nonnull ModernPacket msg, @Nonnull ByteBuf buffer) {
        buffer.writeShort(msg.getId());
        buffer.writeInt(msg.getDebugId());
        LPDataIOWrapper.writeData(buffer, msg::writeData);
    }

    public static void addPacketToNBT(ModernPacket packet, CompoundTag nbt) {
        ByteBuf dataBuffer = buffer();
        PacketHandler.fillByteBuf(packet, dataBuffer);

        byte[] data = new byte[dataBuffer.readableBytes()];
        dataBuffer.getBytes(0, data);
        dataBuffer.release();

        nbt.putByteArray("LogisticsPipes:PacketData", data);
    }

    @OnlyIn(Dist.CLIENT)
    public static void queueAndRemovePacketFromNBT(CompoundTag nbt) {
        byte[] data = nbt.getByteArray("LogisticsPipes:PacketData");
        if (data.length > 0) {
            LPDataIOWrapper.provideData(data, dataInput -> {
                final int packetID = dataInput.readShort();
                final ModernPacket packet = PacketHandler.templateForId(packetID);
                packet.setDebugId(dataInput.readInt());
                packet.readData(dataInput);
                SimpleServiceLocator.clientBufferHandler.queuePacket(packet, MainProxy.proxy.getClientPlayer());
            });
        }
        nbt.remove("LogisticsPipes:PacketData");
    }

    // ── Sending ──────────────────────────────────────────────────────────────

    private static LPPacketPayload buildPayload(@Nonnull ModernPacket msg) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        fillByteBuf(msg, buf);
        return LPPacketPayload.of(buf);
    }

    public static LPPacketPayload buildPayloadPublic(@Nonnull ModernPacket msg) {
        return buildPayload(msg);
    }

    /** Sends a packet from the client to the server. Must only be called client-side. */
    @OnlyIn(Dist.CLIENT)
    public static void sendToServer(@Nonnull ModernPacket msg) {
        PacketDistributor.sendToServer(buildPayload(msg));
    }

    /** Sends a packet from the server to a specific player. Must only be called server-side. */
    public static void sendToPlayer(@Nonnull ModernPacket msg, @Nonnull Player player) {
        if (!(player instanceof ServerPlayer sp)) {
            LogisticsPipes.log.warn("sendToPlayer: player is not a ServerPlayer, skipping");
            return;
        }
        PacketDistributor.sendToPlayer(sp, buildPayload(msg));
    }

    /** Sends a packet to every connected player. Must only be called server-side. */
    public static void sendToAll(@Nonnull ModernPacket msg) {
        PacketDistributor.sendToAllPlayers(buildPayload(msg));
    }

    /** Resolves a fresh packet template for a received id, guarding the null gaps that
     *  {@link #loadPackets} leaves for packets unavailable on this side. A non-null result is
     *  expected in normal play (IDs are index-based and symmetric); a gap means a client/server
     *  packet-table mismatch. */
    private static ModernPacket templateForId(int packetID) {
        ModernPacket tmpl = (packetID >= 0 && packetID < packetlist.size()) ? packetlist.get(packetID) : null;
        if (tmpl == null) {
            throw new IllegalStateException("Received LP packet id " + packetID
                    + " not registered on this side (client/server packet-table mismatch)");
        }
        return tmpl.template();
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    /** Decodes and dispatches a raw LP packet from a FriendlyByteBuf. */
    public static void onPacketData(@Nonnull final FriendlyByteBuf data, @Nonnull final Player player) {
        LPDataIOWrapper.provideData(data, input -> {
            final int packetID = input.readShort();
            final ModernPacket packet = PacketHandler.templateForId(packetID);
            packet.setDebugId(input.readInt());
            packet.readData(input);
            onPacketData(packet, player);
        });
    }

    /** Decodes a raw LP packet from an LPDataInput (used by NBT-embedded packets). */
    public static void onPacketData(@Nonnull final LPDataInput data, @Nonnull final Player player) {
        final int packetID = data.readShort();
        final ModernPacket packet = templateForId(packetID);
        packet.setDebugId(data.readInt());
        packet.readData(data);
        onPacketData(packet, player);
    }

    /** Processes a fully-decoded ModernPacket on the correct thread. */
    public static void onPacketData(@Nonnull ModernPacket packet, @Nonnull final Player player) {
        try {
            packet.processPacket(player);
            if (LogisticsPipes.isDEBUG()) {
                PacketHandler.debugMap.remove(packet.getDebugId());
            }
        } catch (DelayPacketException e) {
            if (packet.retry() && MainProxy.isClient(player.level())) {
                SimpleServiceLocator.clientBufferHandler.queuePacket(packet, player);
            } else if (LogisticsPipes.isDEBUG()) {
                LogisticsPipes.log.error(packet.getClass().getName());
                LogisticsPipes.log.error(packet.toString());
                LogisticsPipes.log.error("Packet handling error", e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
