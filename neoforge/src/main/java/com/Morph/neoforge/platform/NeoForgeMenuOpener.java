package com.Morph.neoforge.platform;

import java.util.function.Consumer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;

import com.Morph.logisticspipes.platform.MenuOpener;

/**
 * NeoForge implementation of {@link MenuOpener}.
 *
 * Uses the {@code Player.openMenu(MenuProvider, Consumer<FriendlyByteBuf>)}
 * overload added by NeoForge. The Consumer writes extra data that the
 * matching {@link com.Morph.neoforge.platform.NeoForgeRegistrar IMenuTypeExtension}
 * delivers to the client menu ctor.
 */
public final class NeoForgeMenuOpener implements MenuOpener {

    public static final NeoForgeMenuOpener INSTANCE = new NeoForgeMenuOpener();

    private NeoForgeMenuOpener() {}

    @Override
    public void openExtendedMenu(ServerPlayer player,
                                  Component title,
                                  MenuConstructor menuFactory,
                                  Consumer<FriendlyByteBuf> dataWriter) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return menuFactory.createMenu(id, inv, p);
            }
        }, dataWriter::accept);
    }
}
