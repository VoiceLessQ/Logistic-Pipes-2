/*
 * Copyright (c) 2021  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2021  RS485
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED.
 */

package network.rs485.logisticspipes.gui.guidebook

import logisticspipes.utils.MinecraftColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import network.rs485.logisticspipes.gui.GuiDrawer
import network.rs485.logisticspipes.gui.HorizontalAlignment
import network.rs485.logisticspipes.gui.VerticalAlignment
import network.rs485.logisticspipes.util.Rectangle
import network.rs485.logisticspipes.util.TextUtil
import network.rs485.logisticspipes.util.math.MutableRectangle

private val homeButtonTexture = Rectangle(16, 64, 24, 32)
private val homeIconTexture = Rectangle(128, 0, 16, 16)

class HomeButton(x: Int, y: Int, onClickAction: (Int) -> Boolean) :
    LPGuiButton(1, x - 24, y - 24, homeButtonTexture.roundedWidth, homeButtonTexture.roundedHeight) {

    private val homeIconBody: MutableRectangle
    override val bodyTrigger = Rectangle(1, 1, 22, 22)

    init {
        this.setOnClickAction(onClickAction)
        val offset = (body.roundedWidth - homeIconTexture.roundedWidth) / 2
        homeIconBody = MutableRectangle(offset, offset, homeIconTexture.roundedWidth, homeIconTexture.roundedHeight)
    }

    override fun setPos(newX: Int, newY: Int) {
        body.setPos(newX - 24, newY + 8)
        this.x = newX - 24
        this.y = newY + 8
    }

    override fun getTooltipText(): String = TextUtil.translate("misc.guide_book.home_button")

    override fun extractContents(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (!visible) return
        val hovered = isHovered(mouseX, mouseY)
        // Button background.
        GuideBookGraphics.blitAtlas(guiGraphics, body, homeButtonTexture, MinecraftColor.WHITE.colorCode, blend = true)
        // Home icon, shifted down the atlas by the hover state (normal/hovered/disabled).
        val hoverStateOffset = getHoverState(hovered) * homeIconTexture.roundedHeight
        GuideBookGraphics.blitAtlas(
            guiGraphics,
            homeIconBody.translated(body),
            homeIconTexture.translated(0, hoverStateOffset),
            MinecraftColor.WHITE.colorCode,
            blend = true,
        )
        if (hovered) {
            drawTooltip(
                x = body.roundedRight,
                y = body.roundedTop,
                horizontalAlign = HorizontalAlignment.RIGHT,
                verticalAlign = VerticalAlignment.BOTTOM,
            )
        }
    }
}
