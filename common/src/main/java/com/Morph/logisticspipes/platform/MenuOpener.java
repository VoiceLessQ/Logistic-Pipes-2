package com.Morph.logisticspipes.platform;

import java.util.function.Consumer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuConstructor;

/**
 * Platform-agnostic helper for opening a server-side menu that ships a
 * {@link FriendlyByteBuf} of extra data to the client menu ctor.
 *
 * Replaces {@code dev.architectury.registry.menu.MenuRegistry.openExtendedMenu}.
 *
 * Implementations:
 *   - Fabric:   {@code com.Morph.fabric.platform.FabricMenuOpener}
 *     (wraps ExtendedScreenHandlerFactory)
 *   - NeoForge: {@code com.Morph.neoforge.platform.NeoForgeMenuOpener}
 *     (Player.openMenu(MenuProvider, Consumer&lt;FriendlyByteBuf&gt;))
 *
 * The byte shape is preserved so the matching {@code registerMenuExtended}
 * passthrough codec in {@link Registrar} can decode it on the client.
 */
public interface MenuOpener {

    void openExtendedMenu(ServerPlayer player,
                           Component title,
                           MenuConstructor menuFactory,
                           Consumer<FriendlyByteBuf> dataWriter);

    // -----------------------------------------------------------------
    // Static accessor — platforms call set() once at init.
    // -----------------------------------------------------------------

    final class Holder {
        static MenuOpener instance;
        private Holder() {}
    }

    static void set(MenuOpener opener) { Holder.instance = opener; }
    static MenuOpener get()           { return Holder.instance; }
}
