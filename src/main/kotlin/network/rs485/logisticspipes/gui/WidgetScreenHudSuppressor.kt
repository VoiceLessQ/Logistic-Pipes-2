/*
 * Copyright (c) 2026  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 */
package network.rs485.logisticspipes.gui

import net.minecraft.client.Minecraft
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent

/**
 * When a widget-based LP GUI (any subclass of [BaseGuiContainer]) is the active screen,
 * suppress the vanilla HUD overlays that would otherwise leak through behind the panel —
 * principally the hotbar, but also crosshair, health/food bars, and chat. Other inventory
 * screens (chest, furnace, etc.) are left alone.
 *
 * 1.20.1 renders `gui.render(...)` unconditionally while a screen is open
 * ([GameRenderer.java:947]); the screen's semi-transparent `renderBackground` gradient only
 * dims the HUD, it doesn't hide it. Cancelling the overlay pre-event is the clean fix.
 */
object WidgetScreenHudSuppressor {

    private val SUPPRESSED = setOf(
        VanillaGuiOverlay.HOTBAR.id(),
        VanillaGuiOverlay.CROSSHAIR.id(),
        VanillaGuiOverlay.PLAYER_HEALTH.id(),
        VanillaGuiOverlay.FOOD_LEVEL.id(),
        VanillaGuiOverlay.ARMOR_LEVEL.id(),
        VanillaGuiOverlay.EXPERIENCE_BAR.id(),
        VanillaGuiOverlay.AIR_LEVEL.id(),
        VanillaGuiOverlay.MOUNT_HEALTH.id(),
        VanillaGuiOverlay.JUMP_BAR.id(),
        VanillaGuiOverlay.CHAT_PANEL.id(),
    )

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderOverlay(event: RenderGuiOverlayEvent.Pre) {
        if (Minecraft.getInstance().screen !is BaseGuiContainer) return
        if (event.overlay.id() in SUPPRESSED) event.isCanceled = true
    }
}
