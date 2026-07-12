/*
 * Copyright (c) 2020  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2020  RS485
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

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import network.rs485.logisticspipes.util.Rectangle
import network.rs485.logisticspipes.util.math.MutableRectangle
import kotlin.math.roundToInt

private const val minimumHeight = 16
private val texture = Rectangle(96, 0, 12, 16)

class SliderButton(
    x: Int,
    y: Int,
    width: Int,
    railHeight: Int,
    private var progress: Float,
    val setProgressCallback: (progress: Float) -> Unit,
) : LPGuiButton(0, x, y, width, railHeight) {

    private val sliderButton: MutableRectangle = MutableRectangle()
    private val movementDistance: Int get() = body.roundedHeight - sliderButton.roundedHeight
    private var dragging: Boolean = false
    private var initialMouseYOffset: Int = 0
    private var hoveredBar: Boolean = false

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (!visible) return
        val grip = sliderButton.translated(body)
        hoveredBar = grip.contains(mouseX, mouseY)
        // Atlas grip is 12px wide at x=96 with four 16px-tall hover states stacked vertically.
        val hoverState = if (dragging) 3 else if (!active) 2 else if (hoveredBar) 1 else 0
        val src = texture.translated(0, hoverState * texture.roundedHeight)
        RenderSystem.enableBlend()
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        // Vertical three-slice: 2px top/bottom caps from the 12x16 grip region, middle
        // stretched to grip height (blitNineSliced was removed in 1.20.2+).
        val gx = grip.roundedLeft
        val gy = grip.roundedTop
        val gw = grip.roundedWidth
        val gh = grip.roundedHeight
        val su = src.roundedLeft.toFloat()
        val sv = src.roundedTop.toFloat()
        val sw = texture.roundedWidth
        val sh = texture.roundedHeight
        val atlas = GuideBookGraphics.ATLAS_SIZE
        guiGraphics.blit(GuideBookGraphics.GUI_ATLAS, gx, gy, gw, 2, su, sv, sw, 2, atlas, atlas)
        guiGraphics.blit(GuideBookGraphics.GUI_ATLAS, gx, gy + 2, gw, gh - 4, su, sv + 2, sw, sh - 4, atlas, atlas)
        guiGraphics.blit(GuideBookGraphics.GUI_ATLAS, gx, gy + gh - 2, gw, 2, su, sv + sh - 2, sw, 2, atlas, atlas)
        RenderSystem.disableBlend()
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (dragging) {
            dragging = false
            setProgressI((mouseY.toInt() - body.roundedY) - initialMouseYOffset)
            initialMouseYOffset = 0
            setProgressCallback(progress)
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (dragging) {
            setProgressI((mouseY.toInt() - body.roundedY) - initialMouseYOffset)
            setProgressCallback(progress)
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mouseXi = mouseX.toInt(); val mouseYi = mouseY.toInt()
        hoveredBar = sliderButton.translated(body).contains(mouseXi, mouseYi)
        if (visible && isActive) {
            if (hoveredBar) {
                dragging = true
                initialMouseYOffset = mouseYi - sliderButton.translated(body).y0.toInt()
                return true
            } else if (body.contains(mouseXi, mouseYi)) {
                setProgressI(mouseYi - body.roundedTop - (sliderButton.roundedHeight / 2))
                setProgressCallback(progress)
            }
        }
        return false
    }

    private fun updateButtonY() {
        val y: Int = (movementDistance * progress).toInt()
        sliderButton.setPos(sliderButton.roundedX, y)
    }

    fun updateSlider(extraHeight: Int, newProgress: Float): SliderButton {
        if (extraHeight > 0) {
            active = true
            sliderButton.setPos(0, calculateProgressI())
            val possibleHeight = body.roundedHeight - extraHeight
            sliderButton.setSize(
                newWidth = body.roundedWidth,
                newHeight = (if ((possibleHeight % 2) == 0) possibleHeight - 1 else possibleHeight).coerceIn(minimumHeight..body.roundedHeight),
            )
            progress = newProgress
            updateButtonY()
        } else {
            active = false
            sliderButton.setPos(0, 0)
            sliderButton.setSize(body.roundedWidth, newHeight = minimumHeight)
            progress = 0.0f
            updateButtonY()
        }
        return this
    }

    private fun setProgressI(progressI: Int) {
        sliderButton.setPos(sliderButton.roundedX, progressI.coerceIn(0, movementDistance))
        progress = calculateProgressF()
    }

    fun changeProgress(amount: Int) {
        sliderButton.setPos(sliderButton.x0, (sliderButton.y0 + amount).coerceIn(0.0f, movementDistance.toFloat()))
        progress = calculateProgressF()
        setProgressCallback(progress)
    }

    private fun calculateProgressI(): Int = (movementDistance * progress).roundToInt()
    private fun calculateProgressF(): Float = if (movementDistance > 0) sliderButton.y0 / movementDistance else 0f
}
