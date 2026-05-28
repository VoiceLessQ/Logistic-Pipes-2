package com.Morph.logisticspipes.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.Morph.ExampleMod;

/**
 * C2S: client requests {@code amount} of {@code itemKey} from the request pipe at {@code pos}.
 *
 * Typed payload, registered on both platforms in their respective mod-init code.
 * Replaces the Architectury {@code NetworkManager.registerReceiver} REQUEST_ITEM packet.
 */
public record RequestItemPayload(BlockPos pos, ResourceLocation itemKey, int amount)
        implements CustomPacketPayload {

    public static final Type<RequestItemPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "request_item"));

    public static final StreamCodec<FriendlyByteBuf, RequestItemPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,        RequestItemPayload::pos,
            ResourceLocation.STREAM_CODEC, RequestItemPayload::itemKey,
            ByteBufCodecs.VAR_INT,         RequestItemPayload::amount,
            RequestItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
