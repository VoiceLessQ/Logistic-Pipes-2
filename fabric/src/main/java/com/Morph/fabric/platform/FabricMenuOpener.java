package com.Morph.fabric.platform;

import java.util.function.Consumer;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;

import com.Morph.logisticspipes.platform.MenuOpener;

/**
 * Fabric implementation of {@link MenuOpener}.
 *
 * Wraps the caller's MenuConstructor + dataWriter in an
 * {@link ExtendedScreenHandlerFactory ExtendedScreenHandlerFactory&lt;FriendlyByteBuf&gt;}.
 * The bytes returned by {@code getScreenOpeningData} flow through Fabric's
 * registered extended-menu codec (the matching passthrough codec in
 * {@link com.Morph.fabric.platform.FabricRegistrar}) and arrive at the
 * client menu ctor as a FriendlyByteBuf with the same bytes.
 */
public final class FabricMenuOpener implements MenuOpener {

    public static final FabricMenuOpener INSTANCE = new FabricMenuOpener();

    private FabricMenuOpener() {}

    @Override
    public void openExtendedMenu(ServerPlayer player,
                                  Component title,
                                  MenuConstructor menuFactory,
                                  Consumer<FriendlyByteBuf> dataWriter) {
        player.openMenu(new ExtendedScreenHandlerFactory<FriendlyByteBuf>() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return menuFactory.createMenu(id, inv, p);
            }

            @Override
            public FriendlyByteBuf getScreenOpeningData(ServerPlayer sp) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                dataWriter.accept(buf);
                return buf;
            }
        });
    }
}
