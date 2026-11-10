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
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.math.roundToInt

abstract class BaseGuiContainer(
    private val baseContainer: LPBaseContainer<LogisticsModule>,
    val xOffset: Int = 0,
    val yOffset: Int = 0,
    private val widgetScreen: WidgetScreen,
) : AbstractContainerScreen<LPBaseContainer<LogisticsModule>>(
    baseContainer,
    Minecraft.getInstance().player!!.inventory,
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
        leftPos = rect.x0.toInt()
        topPos = rect.y0.toInt()
    }

    open fun drawBackgroundLayer(mouseX: Int, mouseY: Int, partialTicks: Float) {}

    open fun drawFocalgroundLayer(mouseX: Float, mouseY: Float, partialTicks: Float) {}

    open fun drawForegroundLayer(mouseX: Float, mouseY: Float, partialTicks: Float) {}

    private var lastPartialTick: Float = 0f

    // imageWidth/imageHeight are final in 26.x; the widget layout's rect is the real panel bounds
    override fun hasClickedOutside(mx: Double, my: Double, xo: Int, yo: Int): Boolean {
        val rect = widgetScreen.relativeBody
        return mx < xo || my < yo || mx >= xo + rect.width || my >= yo + rect.height
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        SimpleGraphics.guiGraphics = guiGraphics
        lastPartialTick = partialTick
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick)
        val floatMouseX = mouseX.toFloat()
        val floatMouseY = mouseY.toFloat()
        widgetScreen.updateHoveredState(floatMouseX, floatMouseY)
        (widgetScreen.hoveredWidget as? Tooltipped)?.getTooltipText()?.takeIf { it.isNotEmpty() }?.also {
            GuiDrawer.drawTextTooltip(it, mouseX, mouseY, 0f, HorizontalAlignment.LEFT, VerticalAlignment.TOP)
        } ?: extractTooltip(guiGraphics, mouseX, mouseY)
        fuzzySelector?.let { fuzzySelector ->
            val slot = hoveredSlot
            if (slot == null && fuzzySelector.active && !fuzzySelector.isMouseHovering(floatMouseX, floatMouseY)) {
                fuzzySelector.active = false
                fuzzySelector.currentSlot = null
            } else if (slot is FuzzyItemSlot && slot != fuzzySelector.currentSlot) {
                fuzzySelector.active = true
                fuzzySelector.currentSlot = slot
                fuzzySelector.setPos(leftPos + slot.x, topPos + slot.y + 17)
            }
            // above item tooltips (z 400)
            guiGraphics.pose().pushMatrix()
            guiGraphics.pose().translate(0f, 0f)
            fuzzySelector.draw(floatMouseX, floatMouseY, partialTick, Screen.screen)
            guiGraphics.pose().popMatrix()
        }
    }

    override fun extractLabels(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        // super.renderLabels draws the menu title + inventory label at local (0,0); we skip those
        // for widget GUIs — widget layout handles its own labels.
        // Pose is translated by (leftPos, topPos); counter-translate so widget absolute coords work.
        val pose = guiGraphics.pose()
        pose.pushMatrix()
        pose.translate(-leftPos.toFloat(), -topPos.toFloat())
        val rect = widgetScreen.relativeBody
        drawBackgroundLayer(mouseX, mouseY, lastPartialTick)
        widgetScreen.widgetContainer.draw(mouseX.toFloat(), mouseY.toFloat(), lastPartialTick, rect)
        drawForegroundLayer(mouseX.toFloat(), mouseY.toFloat(), lastPartialTick)
        pose.popMatrix()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        val button = event.button()
        if (fuzzySelector?.takeIf { it.active }?.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), button) == true) {
            return true
        }
        val hovered = widgetScreen.widgetContainer.getHovered(mouseX.toFloat(), mouseY.toFloat())
        if (hovered is MouseInteractable) {
            if (hovered.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), button)) {
                hovered.playPressedSound(Minecraft.getInstance().soundManager)
                return true
            }
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun slotClicked(slot: Slot?, slotId: Int, mouseButton: Int, type: ContainerInput) {
        // client-side filter fill; the property layer syncs it on close
        if (type == ContainerInput.QUICK_MOVE && slot != null && baseContainer.tryTransferSlotToGhostSlot(slot.index)) return
        super.slotClicked(slot, slotId, mouseButton, type)
    }

    override fun extractBackground(
        guiGraphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick)
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
