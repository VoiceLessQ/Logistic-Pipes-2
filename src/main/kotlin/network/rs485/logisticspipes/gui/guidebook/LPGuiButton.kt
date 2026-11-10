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

package network.rs485.logisticspipes.gui.guidebook

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import network.rs485.logisticspipes.gui.GuiDrawer
import network.rs485.logisticspipes.gui.HorizontalAlignment
import network.rs485.logisticspipes.gui.VerticalAlignment
import network.rs485.logisticspipes.util.Rectangle
import network.rs485.logisticspipes.util.math.MutableRectangle

open class LPGuiButton(id: Int, x: Int, y: Int, width: Int, height: Int) :
    AbstractButton(x, y, width, height, Component.empty()) {

    val body = MutableRectangle(x, y, width, height)

    open val bodyTrigger: Rectangle = Rectangle(width = width, height = height)
    private var onClickAction: ((Int) -> Boolean)? = null

    internal fun isHovered(mouseX: Int, mouseY: Int): Boolean =
        isActive && visible && bodyTrigger.translated(body.x0, body.y0).contains(mouseX, mouseY)

    open fun setPos(newX: Int, newY: Int) {
        body.setPos(newX, newY)
        this.x = newX
        this.y = newY
    }

    // LP1 routed clicks through GuiScreen.mouseClicked -> mousePressed -> actionPerformed ->
    // click(0). In the 1.20.1 Screen model each widget receives mouseClicked itself; hit-test
    // against the body rectangle (not the vanilla widget x/y) like LP1's mousePressed did.
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() != 0 || !isHovered(event.x().toInt(), event.y().toInt())) return false
        return click(0).also { if (it) playDownSound(Minecraft.getInstance().soundManager) }
    }

    open fun setOnClickAction(newOnClickAction: (Int) -> Boolean): LPGuiButton {
        onClickAction = newOnClickAction
        return this
    }

    open fun click(mouseButton: Int): Boolean = onClickAction?.invoke(mouseButton) ?: false

    open fun getTooltipText(): String = ""

    open fun drawTooltip(x: Int, y: Int, horizontalAlign: HorizontalAlignment, verticalAlign: VerticalAlignment) {
        GuiDrawer.drawTextTooltip(listOf(getTooltipText()), x, y, GuideBookConstants.Z_TOOLTIP, horizontalAlign, verticalAlign)
    }

    fun getHoverState(mouseOver: Boolean): Int = if (!isActive) 2 else if (mouseOver) 1 else 0

    // clicks go through mouseClicked/click(); the vanilla press path is unused
    override fun onPress(input: InputWithModifiers) {}

    override fun extractContents(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {}

    override fun updateWidgetNarration(output: NarrationElementOutput) = defaultButtonNarrationText(output)
}
