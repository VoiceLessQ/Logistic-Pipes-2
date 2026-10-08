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

package network.rs485.logisticspipes.gui.widget

import logisticspipes.utils.gui.SimpleGraphics
import net.minecraft.client.Minecraft
import network.rs485.logisticspipes.gui.GuiDrawer
import network.rs485.logisticspipes.util.Rectangle
import network.rs485.logisticspipes.util.TextUtil
import network.rs485.logisticspipes.util.opaque
import network.rs485.logisticspipes.util.math.MutableRectangle

open class Label(fullText: String, internal val x: Int, internal val y: Int, internal val maxLength: Int, internal val textColor: Int, internal val backgroundColor: Int) {

    open val overflows: Boolean get() = fullRect.width > maxLength

    internal val fontRenderer = Minecraft.getInstance().font

    internal val fullRect = MutableRectangle().setPos(x, y)
    internal val trimmedRect = MutableRectangle().setPos(x, y)

    internal var fullText: String = ""
    internal var trimmedText: String = ""
    internal var hovered = false

    init {
        setText(fullText)
    }

    open fun draw(mouseX: Int, mouseY: Int) {
        val gg = SimpleGraphics.guiGraphics ?: return
        hovered = hovered(mouseX, mouseY)
        val rect = if (hovered) fullRect else trimmedRect
        val text = if (hovered) fullText else trimmedText
        if (overflows && hovered) {
            drawOverflowBox(rect.roundedLeft, rect.roundedTop, rect.roundedWidth, rect.roundedHeight)
        }
        gg.drawString(fontRenderer, text, rect.roundedLeft, rect.roundedTop, textColor, false)
    }

    /** LP1: background plus a fading outline while the expanded full text shows. */
    protected fun drawOverflowBox(x: Int, y: Int, w: Int, h: Int) {
        val gg = SimpleGraphics.guiGraphics ?: return
        gg.fill(x, y - 1, x + w, y + h + 1, backgroundColor)
        gg.fill(x + w, y - 2, x + w + 1, y + h + 1, textColor.opaque())
        GuiDrawer.drawHorizontalGradientRect(Rectangle(x, y - 2, w, 1), 0x0, textColor.opaque())
        GuiDrawer.drawHorizontalGradientRect(Rectangle(x, y + h, w, 1), 0x0, textColor.opaque())
    }

    open fun setText(newFullText: String) {
        fullText = newFullText
        fullRect.setSize(fontRenderer.width(fullText), fontRenderer.lineHeight)

        trimmedText = TextUtil.getTrimmedString(fullText, maxLength, fontRenderer)
        trimmedRect.setSize(fontRenderer.width(trimmedText), fontRenderer.lineHeight)

        val offset = (maxLength - trimmedRect.roundedWidth) / 2
        fullRect.setPos(x + offset, y)
        trimmedRect.setPos(x + offset, y)
    }

    fun isTextEqual(text: String): Boolean = fullText === text

    internal fun hovered(mouseX: Int, mouseY: Int): Boolean = (if (hovered) fullRect else trimmedRect).contains(mouseX, mouseY)
}
