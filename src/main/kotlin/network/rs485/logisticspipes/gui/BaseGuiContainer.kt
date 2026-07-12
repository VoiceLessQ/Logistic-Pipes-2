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

import network.rs485.logisticspipes.gui.guidebook.Drawable
import network.rs485.logisticspipes.gui.guidebook.MouseInteractable
import network.rs485.logisticspipes.gui.guidebook.Screen
import network.rs485.logisticspipes.gui.widget.FuzzyItemSlot
import network.rs485.logisticspipes.gui.widget.FuzzySelectionWidget
import network.rs485.logisticspipes.gui.widget.GhostSlot
import network.rs485.logisticspipes.gui.widget.Tooltipped
import network.rs485.logisticspipes.inventory.container.LPBaseContainer
import network.rs485.logisticspipes.util.IRectangle
import logisticspipes.modules.LogisticsModule
import logisticspipes.utils.gui.DummySlot
import logisticspipes.utils.gui.LPGuiGraphics
import logisticspipes.utils.gui.SimpleGraphics
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.math.roundToInt

// TODO: Rendering deferred — full 1.20.1 rendering migration (PoseStack, AbstractContainerScreen API) pending.

abstract class BaseGuiContainer(
    private val baseContainer: LPBaseContainer<LogisticsModule>,
    val xOffset: Int = 0,
    val yOffset: Int = 0,
    private val widgetScreen: WidgetScreen,
) : AbstractContainerScreen<LPBaseContainer<LogisticsModule>>(
    baseContainer,
    Inventory(Minecraft.getInstance().player!!),
    Component.empty(),
), Drawable by widgetScreen {

    open val fuzzySelector: FuzzySelectionWidget? = null

    /** Exposes the protected hoveredSlot field from AbstractContainerScreen. */
    val currentHoveredSlot: Slot? get() = hoveredSlot

    override fun init() {
        super.init()
        // WidgetScreen resets its origin to (0, 0) before placeChildren, so SlotGroup.setPos
        // bakes LOCAL panel-relative coords into Slot.x/y — exactly what AbstractContainerScreen
        // expects at render time (it translates the pose by leftPos/topPos before renderSlot).
        // We just copy the centered rect into leftPos/topPos/imageWidth/imageHeight.
        widgetScreen.initGuiWidget(this@BaseGuiContainer, width, height)
        val rect = widgetScreen.relativeBody
        imageWidth = rect.width.toInt()
        imageHeight = rect.height.toInt()
        leftPos = rect.x0.toInt()
        topPos = rect.y0.toInt()
    }

    open fun drawBackgroundLayer(mouseX: Int, mouseY: Int, partialTicks: Float) {}

    open fun drawFocalgroundLayer(mouseX: Float, mouseY: Float, partialTicks: Float) {}

    open fun drawForegroundLayer(mouseX: Float, mouseY: Float, partialTicks: Float) {}

    private var lastPartialTick: Float = 0f

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        SimpleGraphics.guiGraphics = guiGraphics
        lastPartialTick = partialTick
        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // super.renderLabels draws the menu title + inventory label at local (0,0); we skip those
        // for widget GUIs — widget layout handles its own labels.
        // Pose is translated by (leftPos, topPos); counter-translate so widget absolute coords work.
        val pose = guiGraphics.pose()
        pose.pushPose()
        pose.translate(-leftPos.toFloat(), -topPos.toFloat(), 0f)
        val rect = widgetScreen.relativeBody
        drawBackgroundLayer(mouseX, mouseY, lastPartialTick)
        widgetScreen.widgetContainer.draw(mouseX.toFloat(), mouseY.toFloat(), lastPartialTick, rect)
        drawForegroundLayer(mouseX.toFloat(), mouseY.toFloat(), lastPartialTick)
        pose.popPose()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val hovered = widgetScreen.widgetContainer.getHovered(mouseX.toFloat(), mouseY.toFloat())
        if (hovered is MouseInteractable) {
            if (hovered.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), button)) return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun renderBg(
        guiGraphics: GuiGraphics,
        partialTick: Float,
        mouseX: Int,
        mouseY: Int,
    ) {
        SimpleGraphics.guiGraphics = guiGraphics
        val rect = widgetScreen.relativeBody
        val left = rect.x0.toInt()
        val top = rect.y0.toInt()
        val right = left + rect.width.toInt()
        val bottom = top + rect.height.toInt()
        LPGuiGraphics.drawGuiBackGround(Minecraft.getInstance(), left, top, right, bottom, 0f, true)
    }

    fun List<Drawable>.draw(mouseX: Float, mouseY: Float, partialTicks: Float, visibleArea: IRectangle) =
        forEach {
            it.draw(mouseX, mouseY, partialTicks, visibleArea)
        }

    /**
     * Returns a list of rectangles that overflow from the main gui area, so that JEI can avoid it.
     */
    abstract fun getExtraGuiAreas(): List<IRectangle>
}
