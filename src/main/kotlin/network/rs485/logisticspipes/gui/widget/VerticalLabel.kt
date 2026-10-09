/*
 * Copyright (c) 2022  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2022  RS485
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

import com.mojang.math.Axis
import logisticspipes.utils.gui.SimpleGraphics
import network.rs485.logisticspipes.util.TextUtil

class VerticalLabel(fullText: String, x: Int, y: Int, maxLength: Int, textColor: Int, backgroundColor: Int) : Label(fullText, x, y, maxLength, textColor, backgroundColor) {

    override val overflows: Boolean get() = fullRect.height > maxLength

    override fun draw(mouseX: Int, mouseY: Int) {
        val gg = SimpleGraphics.guiGraphics ?: return
        hovered = hovered(mouseX, mouseY)
        val rect = if (hovered) fullRect else trimmedRect
        val text = if (hovered) fullText else trimmedText
        val pose = gg.pose()
        pose.pushPose()
        pose.translate(rect.x0, rect.y0 + rect.height, 0f)
        pose.mulPose(Axis.ZP.rotationDegrees(-90f))
        if (overflows && hovered) {
            drawOverflowBox(0, 0, fontRenderer.width(text), fontRenderer.lineHeight)
        }
        gg.drawString(fontRenderer, text, 0, 0, textColor, false)
        pose.popPose()
    }

    override fun setText(newFullText: String) {
        fullText = newFullText
        fullRect.setSize(fontRenderer.lineHeight, fontRenderer.width(fullText))

        trimmedText = TextUtil.getTrimmedString(fullText, maxLength, fontRenderer)
        trimmedRect.setSize(fontRenderer.lineHeight, fontRenderer.width(trimmedText))

        val offset = (maxLength - trimmedRect.roundedHeight) / 2
        fullRect.setPos(x, y + offset)
        trimmedRect.setPos(x, y + offset)
    }
}
