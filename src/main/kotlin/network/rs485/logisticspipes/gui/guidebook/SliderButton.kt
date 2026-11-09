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

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
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

    override fun extractContents(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (!visible) return
        val grip = sliderButton.translated(body)
        hoveredBar = grip.contains(mouseX, mouseY)
        // Atlas grip is 12px wide at x=96 with four 16px-tall hover states stacked vertically.
        val hoverState = if (dragging) 3 else if (!active) 2 else if (hoveredBar) 1 else 0
        val src = texture.translated(0, hoverState * texture.roundedHeight)
        // 2px caps, ridged middle tiled (LP1 look; stretching it turns the ridges into blocks)
        val cap = 2
        val midSrc = texture.roundedHeight - 2 * cap
        val u = src.left
        val w = grip.roundedWidth
        fun part(y: Int, v: Float, h: Int) = guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED, GuideBookGraphics.GUI_ATLAS, grip.roundedLeft, y, u, v, w, h, w, h,
            GuideBookGraphics.ATLAS_SIZE, GuideBookGraphics.ATLAS_SIZE,
        )
        part(grip.roundedTop, src.top, cap)
        var y = grip.roundedTop + cap
        val midEnd = grip.roundedBottom - cap
        while (y < midEnd) {
            val h = minOf(midSrc, midEnd - y)
            part(y, src.top + cap, h)
            y += h
        }
        part(midEnd, src.top + texture.roundedHeight - cap, cap)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val mouseY = event.y()
        if (dragging) {
            dragging = false
            setProgressI((mouseY.toInt() - body.roundedY) - initialMouseYOffset)
            initialMouseYOffset = 0
            setProgressCallback(progress)
        }
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val mouseY = event.y()
        if (dragging) {
            setProgressI((mouseY.toInt() - body.roundedY) - initialMouseYOffset)
            setProgressCallback(progress)
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        val button = event.button()
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
