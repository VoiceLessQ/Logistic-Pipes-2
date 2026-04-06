package com.Morph.logisticspipes.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.Morph.logisticspipes.LPConstants;
import com.Morph.logisticspipes.pipes.PipeItemsRequestLogistics;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Central registration of all LP network packets.
 * C2S packets registered here; S2C registered from client init via registerClientReceivers().
 */
public final class LPNetworking {

    /** C2S: client requests an item from the request pipe network. */
    public static final ResourceLocation REQUEST_ITEM =
            ResourceLocation.fromNamespaceAndPath(LPConstants.MOD_ID, "request_item");

    private LPNetworking() {}

    public static void init() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_ITEM,
                (buf, ctx) -> {
                    BlockPos pos = buf.readBlockPos();
                    ResourceLocation itemKey = buf.readResourceLocation();
                    int amount = buf.readInt();
                    ctx.queue(() -> {
                        Player player = ctx.getPlayer();
                        Level level = player.level();
                        net.minecraft.world.item.Item item =
                                BuiltInRegistries.ITEM.getValue(itemKey);
                        if (item == null) return;
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof LogisticsPipeBlockEntity lpbe
                                && lpbe.getPipe() instanceof PipeItemsRequestLogistics req) {
                            req.requestItem(ItemIdentifier.get(item), amount);
                        }
                    });
                });
    }

    /** Build a REQUEST_ITEM packet buffer (called from client screen). */
    public static FriendlyByteBuf buildRequestItemPacket(BlockPos pos,
                                                          ItemIdentifier item, int amount) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item.item));
        buf.writeInt(amount);
        return buf;
    }
}
