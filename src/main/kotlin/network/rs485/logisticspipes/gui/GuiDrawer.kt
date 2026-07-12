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
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.rs485.logisticspipes.gui

import network.rs485.logisticspipes.gui.font.LPFontRenderer
import network.rs485.logisticspipes.gui.guidebook.Screen
import network.rs485.logisticspipes.gui.guidebook.x
import network.rs485.logisticspipes.gui.guidebook.y
import network.rs485.logisticspipes.gui.widget.FuzzyItemSlot
import network.rs485.logisticspipes.util.FuzzyFlag
import network.rs485.logisticspipes.util.FuzzyUtil
import network.rs485.logisticspipes.util.IRectangle
import network.rs485.logisticspipes.util.Rectangle
import network.rs485.logisticspipes.util.math.BorderedRectangle
import network.rs485.logisticspipes.util.math.MutableRectangle
import network.rs485.markdown.defaultDrawableState
import logisticspipes.LPConstants
import logisticspipes.utils.Color
import logisticspipes.utils.MinecraftColor
import logisticspipes.utils.gui.LPGuiGraphics
import logisticspipes.utils.gui.SimpleGraphics
import net.minecraft.world.Container
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.resources.ResourceLocation
import java.lang.Float.min

// Rendering ported to 1.20.1 GuiGraphics via SimpleGraphics.guiGraphics (set by BaseGuiContainer.render()).
// BDF custom-font paths remain deferred — those live on LPFontRenderer.

/**
 * Drawing methods to help with GUIs (rendering implementation deferred for 1.20.1).
 */
object GuiDrawer {

    private const val BORDER: Int = 4
    private const val NORMAL_SLOT_SIZE = 18

    val lpFontRenderer: LPFontRenderer by lazy {
        LPFontRenderer.get("ter-u12n")
    }
    val mcFontRenderer: Font by lazy {
        Minecraft.getInstance().font
    }

    fun getFuzzyColor(fuzzyFlag: FuzzyFlag) = when (fuzzyFlag) {
        FuzzyFlag.IGNORE_DAMAGE -> Color.FUZZY_IGNORE_DAMAGE_COLOR.value
        FuzzyFlag.IGNORE_NBT -> Color.FUZZY_IGNORE_NBT_COLOR.value
        FuzzyFlag.USE_ORE_DICT -> Color.FUZZY_ORE_DICT_COLOR.value
        FuzzyFlag.USE_ORE_CATEGORY -> Color.FUZZY_ORE_CATEGORY_COLOR.value
    }

    fun drawGuiContainerBackground(guiArea: IRectangle, topLeft: Pair<Int, Int>, container: AbstractContainerMenu) {
        drawGuiBackground(guiArea)
        if (SimpleGraphics.guiGraphics == null) return
        val mc = Minecraft.getInstance()
        val (ox, oy) = topLeft
        for (slot in container.slots) {
            if (slot is Slot) {
                LPGuiGraphics.drawSlotBackground(mc, ox + slot.x - 1, oy + slot.y - 1)
            }
        }
    }

    fun drawGuiBackground(guiArea: IRectangle) {
        if (SimpleGraphics.guiGraphics == null) return
        val left = guiArea.roundedLeft
        val top = guiArea.roundedTop
        val right = guiArea.roundedRight
        val bottom = guiArea.roundedBottom
        LPGuiGraphics.drawGuiBackGround(Minecraft.getInstance(), left, top, right, bottom, 0f, true)
    }

    fun drawGuiTexturedRect(rect: IRectangle, text: IRectangle, blend: Boolean, color: Int) {
        // TODO: texture-atlas sprite blit — no widget GUI calls this yet; port alongside guide book work.
    }

    private val VANILLA_WIDGETS = ResourceLocation.withDefaultNamespace("textures/gui/widgets.png")

    fun drawBorderedTile(
        rect: IRectangle,
        hovered: Boolean,
        enabled: Boolean,
        light: Boolean,
        thickerBottomBorder: Boolean,
    ) {
        val gg = SimpleGraphics.guiGraphics ?: return
        val textureY = 46 + when {
            !enabled -> 0
            hovered -> 2
            else -> 1
        } * 20
        // Nine-slice of the 200x20 vanilla button texture: 20px horizontal and 4px vertical
        // borders, middle stretched (blitNineSliced was removed in 1.20.2+).
        val x = rect.roundedLeft
        val y = rect.roundedTop
        val w = rect.roundedWidth
        val h = rect.roundedHeight
        val bw = minOf(20, w / 2)
        val bh = minOf(4, h / 2)
        val v = textureY.toFloat()
        // corners
        gg.blit(VANILLA_WIDGETS, x, y, bw, bh, 0f, v, bw, bh, 256, 256)
        gg.blit(VANILLA_WIDGETS, x + w - bw, y, bw, bh, (200 - bw).toFloat(), v, bw, bh, 256, 256)
        gg.blit(VANILLA_WIDGETS, x, y + h - bh, bw, bh, 0f, v + 20 - bh, bw, bh, 256, 256)
        gg.blit(VANILLA_WIDGETS, x + w - bw, y + h - bh, bw, bh, (200 - bw).toFloat(), v + 20 - bh, bw, bh, 256, 256)
        // edges
        if (w > 2 * bw) {
            gg.blit(VANILLA_WIDGETS, x + bw, y, w - 2 * bw, bh, bw.toFloat(), v, 200 - 2 * bw, bh, 256, 256)
            gg.blit(VANILLA_WIDGETS, x + bw, y + h - bh, w - 2 * bw, bh, bw.toFloat(), v + 20 - bh, 200 - 2 * bw, bh, 256, 256)
        }
        if (h > 2 * bh) {
            gg.blit(VANILLA_WIDGETS, x, y + bh, bw, h - 2 * bh, 0f, v + bh, bw, 20 - 2 * bh, 256, 256)
            gg.blit(VANILLA_WIDGETS, x + w - bw, y + bh, bw, h - 2 * bh, (200 - bw).toFloat(), v + bh, bw, 20 - 2 * bh, 256, 256)
        }
        // center
        if (w > 2 * bw && h > 2 * bh) {
            gg.blit(VANILLA_WIDGETS, x + bw, y + bh, w - 2 * bw, h - 2 * bh, bw.toFloat(), v + bh, 200 - 2 * bw, 20 - 2 * bh, 256, 256)
        }
    }

    fun drawGuideBookFrame(rect: IRectangle, slider: IRectangle) {
        // TODO: guide book frame — deferred with guide book rendering port.
    }

    fun drawTextTooltip(
        text: List<String>,
        x: Int,
        y: Int,
        z: Float,
        horizontalAlign: HorizontalAlignment,
        verticalAlign: VerticalAlignment,
    ) {
        if (text.isEmpty()) return
        val gg = SimpleGraphics.guiGraphics ?: return
        val components = text.map { Component.literal(it) }
        gg.renderComponentTooltip(mcFontRenderer, components, x, y)
    }

    fun drawGuideBookBackground(rect: IRectangle) {
        // TODO: guide book background — deferred with guide book rendering port.
    }

    fun drawSliderButton(body: IRectangle, texture: IRectangle) {
        // TODO: guide book slider — deferred with guide book rendering port.
    }

    fun drawCenteredString(text: String, x: Int, y: Int, color: Int, shadow: Boolean) {
        val gg = SimpleGraphics.guiGraphics ?: return
        val textWidth = mcFontRenderer.width(text)
        gg.drawString(mcFontRenderer, text, x - textWidth / 2, y, color, shadow)
    }

    fun drawInteractionIndicator(mouseX: Float, mouseY: Float) {
        // TODO: guide book hover indicator — deferred.
    }

    fun drawRect(area: IRectangle, color: Int) {
        val gg = SimpleGraphics.guiGraphics ?: return
        gg.fill(area.roundedLeft, area.roundedTop, area.roundedRight, area.roundedBottom, color)
    }

    fun drawHorizontalGradientRect(area: IRectangle, colorLeft: Int, colorRight: Int) {
        val gg = SimpleGraphics.guiGraphics ?: return
        // GuiGraphics.fillGradient is vertical-only; approximate horizontal via per-column fills.
        val left = area.roundedLeft
        val right = area.roundedRight
        val top = area.roundedTop
        val bottom = area.roundedBottom
        val span = (right - left).coerceAtLeast(1)
        val al = (colorLeft ushr 24) and 0xFF
        val rl = (colorLeft ushr 16) and 0xFF
        val gl = (colorLeft ushr 8) and 0xFF
        val bl = colorLeft and 0xFF
        val ar = (colorRight ushr 24) and 0xFF
        val rr = (colorRight ushr 16) and 0xFF
        val gr = (colorRight ushr 8) and 0xFF
        val br = colorRight and 0xFF
        for (i in 0 until span) {
            val t = i.toFloat() / span.toFloat()
            val a = (al + (ar - al) * t).toInt() and 0xFF
            val r = (rl + (rr - rl) * t).toInt() and 0xFF
            val g = (gl + (gr - gl) * t).toInt() and 0xFF
            val b = (bl + (br - bl) * t).toInt() and 0xFF
            val c = (a shl 24) or (r shl 16) or (g shl 8) or b
            gg.fill(left + i, top, left + i + 1, bottom, c)
        }
    }

    fun drawVerticalGradientRect(area: IRectangle, colorTop: Int, colorBottom: Int) {
        val gg = SimpleGraphics.guiGraphics ?: return
        gg.fillGradient(area.roundedLeft, area.roundedTop, area.roundedRight, area.roundedBottom, colorTop, colorBottom)
    }

    fun drawLine(start: Pair<Float, Float>, finish: Pair<Float, Float>, color: Int, thickness: Float) {
        val gg = SimpleGraphics.guiGraphics ?: return
        val (x1, y1) = start
        val (x2, y2) = finish
        val t = thickness.coerceAtLeast(1f).toInt()
        if (y1 == y2) {
            val xMin = min(x1, x2).toInt()
            val xMax = kotlin.math.max(x1, x2).toInt()
            gg.fill(xMin, y1.toInt(), xMax, y1.toInt() + t, color)
        } else if (x1 == x2) {
            val yMin = min(y1, y2).toInt()
            val yMax = kotlin.math.max(y1, y2).toInt()
            gg.fill(x1.toInt(), yMin, x1.toInt() + t, yMax, color)
        } else {
            // diagonal not supported in widget paths — use axis-aligned only.
        }
    }

    fun drawOutlineRect(rect: IRectangle, color: Int) {
        val gg = SimpleGraphics.guiGraphics ?: return
        val left = rect.roundedLeft
        val top = rect.roundedTop
        val right = rect.roundedRight
        val bottom = rect.roundedBottom
        gg.fill(left, top, right, top + 1, color)
        gg.fill(left, bottom - 1, right, bottom, color)
        gg.fill(left, top, left + 1, bottom, color)
        gg.fill(right - 1, top, right, bottom, color)
    }
}

private class Texture(val resource: ResourceLocation, size: Int) {
    val factor: Float = 1.0f / size
}
