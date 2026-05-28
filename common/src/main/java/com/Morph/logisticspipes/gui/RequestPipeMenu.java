package com.Morph.logisticspipes.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.Morph.logisticspipes.LPMenuTypes;
import com.Morph.logisticspipes.pipes.PipeItemsRequestLogistics;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;
import com.Morph.logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Menu for the request pipe. Displays available network items; no inventory slots.
 */
public class RequestPipeMenu extends AbstractContainerMenu {

    private final BlockPos pipePos;
    private final List<ItemIdentifierStack> networkItems = new ArrayList<>();

    /** Server-side constructor — reads items directly from pipe. */
    public RequestPipeMenu(int containerId, Inventory playerInv, BlockPos pos) {
        super(LPMenuTypes.REQUEST_PIPE.get(), containerId);
        this.pipePos = pos;
    }

    /** Client-side constructor — reads items from the extra buf written by openExtendedMenu. */
    public RequestPipeMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        super(LPMenuTypes.REQUEST_PIPE.get(), containerId);
        this.pipePos = buf.readBlockPos();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            net.minecraft.world.item.Item item =
                    BuiltInRegistries.ITEM.getOptional(buf.readResourceLocation()).orElse(null);
            int amount = buf.readInt();
            if (item != null) {
                networkItems.add(new ItemIdentifierStack(ItemIdentifier.get(item), amount));
            }
        }
    }

    public List<ItemIdentifierStack> getNetworkItems() { return networkItems; }

    public BlockPos getPipePos() { return pipePos; }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    // -------------------------------------------------------------------------
    // Static helper: write network items to buf for the client constructor
    // -------------------------------------------------------------------------

    public static void writeItemsToBuf(FriendlyByteBuf buf, Map<ItemIdentifier, Integer> items) {
        buf.writeInt(items.size());
        for (Map.Entry<ItemIdentifier, Integer> entry : items.entrySet()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(entry.getKey().item);
            buf.writeResourceLocation(key != null ? key : ResourceLocation.withDefaultNamespace("air"));
            buf.writeInt(entry.getValue());
        }
    }
}
