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

package network.rs485.logisticspipes.gui.guidebook

import logisticspipes.utils.MinecraftColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import network.rs485.logisticspipes.gui.HorizontalAlignment
import network.rs485.logisticspipes.gui.VerticalAlignment
import network.rs485.logisticspipes.util.Rectangle

interface TabButtonReturn {
    fun onLeftClick(): Boolean
    fun onRightClick(shiftClick: Boolean, ctrlClick: Boolean): Boolean
    fun getColor(): Int
    fun isPageActive(): Boolean
}

private val buttonTextureArea = Rectangle(40, 64, 24, 32)
private val circleAreaTexture = Rectangle(32, 96, 16, 16)

/** Multiply tint with a channel floor, so a black tab keeps its outline instead of becoming a solid black block. */
private fun inactiveTabTint(colorCode: Int): Int {
    fun channel(shift: Int) = maxOf((colorCode shr shift) and 0xFF, 0x40) shl shift
    return 0xFF000000.toInt() or channel(16) or channel(8) or channel(0)
}

class TabButton(
    internal val tabPage: Page,
    x: Int,
    y: Int,
    private val whisky: TabButtonReturn,
) : LPGuiButton(99, x, y - 24, 24, 32) {

    override val bodyTrigger = Rectangle(1, 1, 22, 22)
    private val circleArea = Rectangle(4, 4, 16, 16)
    val isPageActive: Boolean
        get() = whisky.isPageActive()
    val isInactive: Boolean
        get() = !isPageActive

    fun onLeftClick() = whisky.onLeftClick()

    fun onRightClick(shiftClick: Boolean, ctrlClick: Boolean) = whisky.onRightClick(shiftClick, ctrlClick)

    // Body pass, drawn UNDER the frame: only inactive tabs (LP1's drawButton). The active tab's
    // body is drawn over the frame in renderForeground so it "opens into" the page.
    override fun extractContents(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (!visible || !isInactive) return
        // Inactive tabs sit slightly lower (shifted down by 3px) and are tinted with their color.
        val tabColor: Int = inactiveTabTint(MinecraftColor.values()[whisky.getColor()].colorCode)
        val yOffset = if (whisky.isPageActive()) 0 else 3
        GuideBookGraphics.blitAtlas(
            guiGraphics,
            body.translated(0, yOffset),
            buttonTextureArea,
            color = if (whisky.isPageActive()) 0xFFFFFFFF.toInt() else tabColor,
            blend = true,
        )
    }

    // Foreground pass, drawn OVER the frame (LP1's drawButtonForegroundLayer): the active tab's
    // white body plus colored circle marker, and the hover tooltip.
    fun renderForeground(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (!isInactive) {
            val tabColor: Int = (MinecraftColor.values()[whisky.getColor()].colorCode and 0x00FFFFFF) or 0xFF000000.toInt()
            GuideBookGraphics.blitAtlas(guiGraphics, body, buttonTextureArea, color = -1, blend = true)
            GuideBookGraphics.blitAtlas(guiGraphics, circleArea.translated(body), circleAreaTexture, color = tabColor, blend = true)
        }
        if (visible && isHovered(mouseX, mouseY)) {
            drawTooltip(
                x = body.roundedRight,
                y = body.roundedTop,
                horizontalAlign = HorizontalAlignment.RIGHT,
                verticalAlign = VerticalAlignment.BOTTOM,
            )
        }
    }

    override fun getTooltipText(): String {
        return tabPage.title
    }

    override fun setPos(newX: Int, newY: Int) {
        body.setPos(newX, newY - 24)
        this.x = newX
        this.y = newY - 24
    }

    // LP1 dispatched via GuiGuideBook.mouseClicked -> mousePressed -> actionPerformed/rightClick:
    // left-click on an INACTIVE tab switches to its page, right-click on the active tab cycles
    // its color (shift inverts, ctrl+shift removes the bookmark).
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        val button = event.button()
        if (!visible || !active) return false
        val hit = bodyTrigger
            .translated(body)
            .translated(0, if (whisky.isPageActive()) -3 else 0)
            .contains(mouseX.toInt(), mouseY.toInt())
        if (!hit) return false
        val handled = when (button) {
            0 -> onLeftClick()
            1 -> onRightClick(
                shiftClick = net.minecraft.client.Minecraft.getInstance().hasShiftDown(),
                ctrlClick = net.minecraft.client.Minecraft.getInstance().hasControlDown(),
            )
            else -> false
        }
        if (handled) playDownSound(net.minecraft.client.Minecraft.getInstance().soundManager)
        return handled
    }
}
