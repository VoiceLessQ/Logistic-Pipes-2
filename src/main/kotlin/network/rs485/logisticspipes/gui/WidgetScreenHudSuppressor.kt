/*
 * Copyright (c) 2026  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 */
package network.rs485.logisticspipes.gui

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft

object WidgetScreenHudSuppressor {

    private val SUPPRESSED = listOf(
        VanillaHudElements.HOTBAR,
        VanillaHudElements.CROSSHAIR,
        VanillaHudElements.HEALTH_BAR,
        VanillaHudElements.FOOD_BAR,
        VanillaHudElements.ARMOR_BAR,
        VanillaHudElements.EXPERIENCE_LEVEL,
        VanillaHudElements.AIR_BAR,
        VanillaHudElements.MOUNT_HEALTH,
        VanillaHudElements.INFO_BAR,
        VanillaHudElements.CHAT,
    )

    fun register() {
        SUPPRESSED.forEach { id ->
            HudElementRegistry.replaceElement(id) { original ->
                HudElement { graphics, deltaTracker ->
                    if (Minecraft.getInstance().gui.screen() !is BaseGuiContainer) original.extractRenderState(graphics, deltaTracker)
                }
            }
        }
    }
}
