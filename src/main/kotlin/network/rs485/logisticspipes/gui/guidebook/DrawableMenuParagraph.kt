/*
 * Copyright (c) 2020-2021  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2020-2021  RS485
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

import logisticspipes.LPItems
import logisticspipes.utils.MinecraftColor
import logisticspipes.utils.gui.SimpleGraphics
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import network.rs485.logisticspipes.gui.GuiDrawer
import network.rs485.logisticspipes.gui.widget.Tooltipped
import network.rs485.logisticspipes.util.IRectangle
import network.rs485.logisticspipes.util.math.MutableRectangle
import network.rs485.markdown.TextFormat
import java.util.*

/**
 * Resolves an icon identifier (e.g. "minecraft:stone") to an [ItemStack], falling back to LP's broken
 * item when the identifier is unknown. Mirrors LP1's `Item.REGISTRY.getObject(...) ?: LPItems.brokenItem`
 * using the 1.20.1 [BuiltInRegistries.ITEM] registry.
 */
private fun iconStack(icon: String): ItemStack {
    // BuiltInRegistries.ITEM is a defaulted registry — unknown keys resolve to AIR rather than null,
    // so treat AIR as "not found" and fall back to LP's broken item (matching LP1's broken-icon look).
    // tryParse instead of the constructor: book pages are data-driven and a malformed identifier
    // (e.g. uppercase) must degrade to the broken item, not throw (1.12's constructor never threw).
    val item = Identifier.tryParse(icon)?.let { BuiltInRegistries.ITEM.getValue(it) }
    return if (item == null || item === Items.AIR) ItemStack(LPItems.getBrokenItem()) else ItemStack(item)
}

private const val listEntryHeight = 24
private const val tileSize = 40
private const val tileSpacing = 5

/**
 * Menu token, stores the key and the type of menu in a page.
 */
class DrawableMenuParagraph<T>(private val menuTitle: List<DrawableWord>, private val menuGroups: List<DrawableMenuGroup<T>>) :
    DrawableParagraph() where T : Drawable, T : GuideBookMouseInteractable {
    override val relativeBody = MutableRectangle()
    override var parent: Drawable? = null

    private val horizontalLine = createChild { DrawableHorizontalLine(1) }

    override fun draw(mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        super.draw(mouseX, mouseY, delta, visibleArea)
        drawChildren(mouseX, mouseY, delta, visibleArea)
    }

    override fun inBookMouseClicked(mouseX: Float, mouseY: Float, mouseButton: Int, guideActionListener: GuiGuideBook.ActionListener?): Boolean =
        menuGroups.firstOrNull { it.isMouseHovering(mouseX, mouseY) }?.inBookMouseClicked(mouseX, mouseY, mouseButton, guideActionListener)
            ?: false

    override fun setChildrenPos(): Int {
        var currentY = 1
        currentY += splitAndInitialize(menuTitle, 0, currentY, width, false)
        currentY += horizontalLine.setPos(0, currentY).y
        currentY += 5
        for (group in menuGroups) currentY += group.setPos(0, currentY).y
        return currentY
    }

    override fun drawChildren(mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        (menuTitle + horizontalLine + menuGroups).filter { it.visible(visibleArea) }.forEach { it.draw(mouseX, mouseY, delta, visibleArea) }
    }

    override fun getHovered(mouseX: Float, mouseY: Float): Drawable? =
        (menuTitle + horizontalLine + menuGroups).firstOrNull { it.isMouseHovering(mouseX, mouseY) }?.let {
            if (it is DrawableParagraph) {
                return it.getHovered(mouseX, mouseY)
            } else {
                it
            }
        }
}

class DrawableMenuGroup<T>(private val groupTitle: List<DrawableWord>, private val groupTiles: List<T>) :
    DrawableParagraph() where T : Drawable, T : GuideBookMouseInteractable {
    override val relativeBody = MutableRectangle()
    override var parent: Drawable? = null

    override fun draw(mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        drawChildren(mouseX, mouseY, delta, visibleArea)
    }

    override fun inBookMouseClicked(mouseX: Float, mouseY: Float, mouseButton: Int, guideActionListener: GuiGuideBook.ActionListener?): Boolean =
        groupTiles.firstOrNull { it.isMouseHovering(mouseX, mouseY) }?.inBookMouseClicked(mouseX, mouseY, mouseButton, guideActionListener)
            ?: false

    override fun setChildrenPos(): Int {
        var currentY = 0
        var currentX = tileSpacing
        currentY += splitAndInitialize(groupTitle, currentX, currentY, width, false)
        for (tile in groupTiles) {
            if (currentX + tile.width + tileSpacing > width) {
                currentX = tileSpacing
                currentY += tile.height + tileSpacing
            }
            tile.setPos(currentX, currentY)
            currentX += tile.width + tileSpacing
            if (tile == groupTiles.last()) currentY += tile.height + tileSpacing
        }
        return currentY
    }

    override fun drawChildren(mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        (groupTitle + groupTiles).filter { it.visible(visibleArea) }.forEach { it.draw(mouseX, mouseY, delta, visibleArea) }
    }

    override fun getHovered(mouseX: Float, mouseY: Float): Drawable? =
        (groupTitle + groupTiles).firstOrNull { it.isMouseHovering(mouseX, mouseY) }
}

class DrawableMenuTile(private val linkedPage: String, private val pageName: String, private val icon: String) : Drawable, GuideBookMouseInteractable, Tooltipped {
    private val iconScale = 1.5f
    private val iconBody = MutableRectangle()

    override val relativeBody = MutableRectangle()
    override var parent: Drawable? = null

    init {
        relativeBody.setSize(tileSize, tileSize)
        iconBody.setSize((16 * iconScale).toInt(), (16 * iconScale).toInt())
        iconBody.setPos((tileSize - iconBody.width) / 2, (tileSize - iconBody.height) / 2)
    }

    override fun getTooltipText(): List<String> = listOf(pageName)

    override fun isMouseHovering(mouseX: Float, mouseY: Float): Boolean = absoluteBody.contains(mouseX, mouseY)

    override fun inBookMouseClicked(mouseX: Float, mouseY: Float, mouseButton: Int, guideActionListener: GuiGuideBook.ActionListener?): Boolean =
        guideActionListener?.onMenuButtonClick(linkedPage) != null

    override fun draw(mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        val hovered = isMouseHovering(mouseX, mouseY)
        GuiDrawer.drawBorderedTile(
            rect = absoluteBody,
            hovered = hovered,
            enabled = true,
            light = true,
            thickerBottomBorder = false
        )
        if (hovered) {
            GuiDrawer.drawInteractionIndicator(mouseX, mouseY)
        }
        val itemRect = iconBody.translated(absoluteBody)
        if (visibleArea.intersects(itemRect)) {
            val guiGraphics = SimpleGraphics.guiGraphics ?: return
            // renderItem draws at native 16x16; scale the pose by iconScale around the icon origin.
            val pose = guiGraphics.pose()
            pose.pushMatrix()
            pose.translate(itemRect.left, itemRect.top)
            pose.scale(iconScale, iconScale)
            guiGraphics.item(iconStack(icon), 0, 0)
            pose.popMatrix()
        }
    }

    override fun setPos(x: Int, y: Int): Pair<Int, Int> {
        relativeBody.setPos(x, y)
        return super.setPos(x, y)
    }
}

class DrawableMenuListEntry(private val linkedPage: String, private val pageName: String, private val icon: String) : Drawable, GuideBookMouseInteractable {
    private val iconScale = 1.0f
    private val iconSize = (16 * iconScale).toInt()
    private val itemRect = MutableRectangle()
    private val itemOffset = (listEntryHeight - iconSize) / 2

    override val relativeBody = MutableRectangle()
    override var parent: Drawable? = null

    init {
        relativeBody.setSize(4 * itemOffset + iconSize + GuiDrawer.lpFontRenderer.width(pageName), listEntryHeight)
        itemRect.setSize(iconSize, iconSize)
    }

    override fun isMouseHovering(mouseX: Float, mouseY: Float): Boolean = absoluteBody.contains(mouseX, mouseY)

    override fun inBookMouseClicked(mouseX: Float, mouseY: Float, mouseButton: Int, guideActionListener: GuiGuideBook.ActionListener?): Boolean =
        guideActionListener?.onMenuButtonClick(linkedPage) != null

    override fun draw(mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        val hovered = visibleArea.contains(mouseX, mouseY) && isMouseHovering(mouseX, mouseY)
        GuiDrawer.drawBorderedTile(
            rect = absoluteBody,
            hovered = hovered,
            enabled = true,
            light = true,
            thickerBottomBorder = false
        )
        itemRect.setPos(left + itemOffset, top + itemOffset)
        if (itemRect.intersects(visibleArea)) {
            val textColor: Int = if (!hovered) MinecraftColor.WHITE.colorCode else 0xffffffa0.toInt()
            val textVerticalOffset = (height - GuiDrawer.lpFontRenderer.getFontHeight(1.0f)) / 2
            GuiDrawer.lpFontRenderer.drawString(
                string = pageName,
                x = itemRect.right + itemOffset,
                y = top + textVerticalOffset,
                color = textColor,
                format = EnumSet.of(TextFormat.Shadow),
                scale = 1.0f
            )
            SimpleGraphics.guiGraphics?.item(iconStack(icon), itemRect.roundedLeft, itemRect.roundedTop)
        }
        if (hovered) {
            GuiDrawer.drawInteractionIndicator(mouseX, mouseY)
        }
    }

    override fun setPos(x: Int, y: Int): Pair<Int, Int> {
        relativeBody.setPos(x, y)
        itemRect.setPos(left + itemOffset, top + itemOffset)
        return super.setPos(x, y)
    }
}
