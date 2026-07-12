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

import logisticspipes.LPItems
import logisticspipes.LogisticsPipes
import logisticspipes.modplugins.jei.JEIPluginLoader
import logisticspipes.utils.MinecraftColor
import logisticspipes.utils.gui.SimpleGraphics
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import network.rs485.logisticspipes.gui.GuiDrawer
import network.rs485.logisticspipes.gui.HorizontalAlignment
import network.rs485.logisticspipes.gui.VerticalAlignment
import network.rs485.logisticspipes.gui.widget.Tooltipped
import network.rs485.logisticspipes.guidebook.BookContents
import network.rs485.logisticspipes.guidebook.BookContents.MAIN_MENU_FILE
import network.rs485.logisticspipes.guidebook.DebugPage
import network.rs485.logisticspipes.guidebook.ItemGuideBook
import network.rs485.logisticspipes.util.cycleMinecraftColorId
import network.rs485.logisticspipes.util.math.MutableRectangle
import network.rs485.markdown.TextFormat
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object GuideBookConstants {
    const val Z_TOOLTIP = 500.0f
    const val DRAW_BODY_WIREFRAME = false
}

class GuiGuideBook(private val state: ItemGuideBook.GuideBookState) : Screen(Component.empty()) {

    private val guiBorderThickness = 16
    private val guiShadowThickness = 6
    private val guiSeparatorThickness = 6
    private val guiSliderWidth = 12
    private val guiTabWidth = 24
    private val maxTabs = 100

    private val innerGui = MutableRectangle()
    private val outerGui = MutableRectangle()
    private val sliderSeparator = MutableRectangle()
    private val visibleArea = MutableRectangle()

    private var guiSliderX = 0
    private var guiSliderY0 = 0
    private var guiSliderY1 = 0

    private lateinit var slider: SliderButton
    private lateinit var home: HomeButton
    private lateinit var addOrRemoveTabButton: BookmarkManagingButton

    private val tabButtons = state.bookmarks.map(::createGuiTabButton).toMutableList()
    private var freeColor: Int = state.bookmarks.maxOfOrNull { it.color ?: 0 } ?: 0

    private val cachedPages = state.bookmarks.plus(state.currentPage).associateBy { it.page }.toMutableMap()

    private val actionListener = ActionListener()
    private var currentProgress: Float = state.currentPage.progress
    private var clickedLinkURI: URI? = null

    inner class ActionListener {
        fun onMenuButtonClick(newPage: String) = changePage(newPage)
        fun onPageLinkClick(newPage: String) = changePage(newPage)
        fun onWebLinkClick(webLink: String) {
            try {
                clickedLinkURI = URI(webLink)
                minecraft?.setScreen(net.minecraft.client.gui.screens.ConfirmLinkScreen({ confirmed ->
                    if (confirmed) net.minecraft.Util.getPlatform().openUri(clickedLinkURI!!)
                    minecraft?.setScreen(this@GuiGuideBook)
                }, webLink, true))
            } catch (error: URISyntaxException) {
                LogisticsPipes.log.warn("Could not parse link $webLink in GuiGuideBook", error)
            }
        }
        fun onItemLinkClick(stack: ItemStack) {
            JEIPluginLoader.showRecipe(stack)
        }
    }

    init {
        if (LogisticsPipes.isDEBUG()) {
            val debugSavedPage = cachedPages.getOrPut(DebugPage.FILE) { Page(PageData(DebugPage.FILE)) }
            debugSavedPage.color = 0
            tabButtons.add(createGuiTabButton(debugSavedPage))
        }
    }

    private fun changePage(path: String) {
        val newPage = cachedPages.getOrPut(path) { Page(PageData(path)) }
        state.currentPage = newPage
        currentProgress = state.currentPage.progress
        newPage.setDrawablesPosition(visibleArea)
        updateButtonVisibility()
    }

    private fun calculateGuiConstraints() {
        val marginRatio = 1.0 / 8.0
        val sizeRatio = 6.0 / 8.0
        outerGui.setPos(floor(marginRatio * width).toInt(), floor(marginRatio * height).toInt())
            .setSize((sizeRatio * width).toInt(), (sizeRatio * height).toInt())
        innerGui.setPos(outerGui.x0 + guiBorderThickness, outerGui.y0 + guiBorderThickness)
            .setSize(outerGui.roundedWidth - 2 * guiBorderThickness, outerGui.roundedHeight - 2 * guiBorderThickness)
        sliderSeparator.setPos(innerGui.x1 - guiSliderWidth - guiSeparatorThickness - guiShadowThickness, innerGui.y0 - 1)
            .setSize(2 * guiShadowThickness + guiSeparatorThickness, innerGui.roundedHeight + 2)
        guiSliderX = innerGui.roundedRight - guiSliderWidth
        guiSliderY0 = innerGui.roundedTop
        guiSliderY1 = innerGui.roundedBottom
        visibleArea.setPos(innerGui.x0 + guiShadowThickness, innerGui.y0)
            .setSize(innerGui.roundedWidth - sliderSeparator.roundedWidth - guiSliderWidth, innerGui.roundedHeight)
        state.currentPage.setDrawablesPosition(visibleArea)
        updateButtonVisibility()
    }

    private fun updateButtonVisibility() {
        if (this::home.isInitialized) home.visible = state.currentPage.page != MAIN_MENU_FILE
        if (this::slider.isInitialized) {
            slider.updateSlider(state.currentPage.getExtraHeight(visibleArea), state.currentPage.progress)
        }
        var xOffset = 0
        for (button in tabButtons) {
            button.setPos(outerGui.roundedRight - 2 - 2 * guiTabWidth - xOffset, outerGui.roundedTop)
            xOffset += guiTabWidth
        }
        if (this::addOrRemoveTabButton.isInitialized) {
            addOrRemoveTabButton.updateState()
            addOrRemoveTabButton.setX(outerGui.roundedRight - 20 - guiTabWidth - xOffset)
        }
    }

    private fun isTabAbsent(page: Page): Boolean = state.bookmarks.none { it.pageEquals(page) }

    override fun init() {
        calculateGuiConstraints()
        slider = addRenderableWidget(
            SliderButton(
                x = innerGui.roundedRight - guiSliderWidth,
                y = innerGui.roundedTop,
                railHeight = innerGui.roundedHeight,
                width = guiSliderWidth,
                progress = state.currentPage.progress,
                setProgressCallback = { progress -> state.currentPage.progress = progress }
            )
        )
        home = addRenderableWidget(
            HomeButton(
                x = outerGui.roundedRight,
                y = outerGui.roundedTop
            ) { mouseButton ->
                if (mouseButton == 0) changePage(MAIN_MENU_FILE)
                true
            }
        )
        addOrRemoveTabButton = addRenderableWidget(
            BookmarkManagingButton(
                x = outerGui.roundedRight - 18 - guiTabWidth + 4,
                y = outerGui.roundedTop - 2,
                onClickAction = { buttonState ->
                    when (buttonState) {
                        BookmarkManagingButton.ButtonState.ADD -> addBookmark().let { true }
                        BookmarkManagingButton.ButtonState.REMOVE -> removeBookmark(state.currentPage)
                        BookmarkManagingButton.ButtonState.DISABLED -> false
                    }
                },
                additionStateUpdater = {
                    when {
                        state.currentPage.isBookmarkable() && isTabAbsent(state.currentPage) -> BookmarkManagingButton.ButtonState.ADD
                        !isTabAbsent(state.currentPage) -> BookmarkManagingButton.ButtonState.REMOVE
                        else -> BookmarkManagingButton.ButtonState.DISABLED
                    }
                }
            )
        )
        // Tab buttons receive click/scroll events but are drawn manually in render() so they sit
        // UNDER the frame (matching LP1's draw order); hence addWidget (events-only) not
        // addRenderableWidget.
        tabButtons.forEach { addWidget(it) }
        updateButtonVisibility()
    }

    override fun onClose() {
        LPItems.getItemGuideBook().saveState(state)
        if (LogisticsPipes.isDEBUG()) BookContents.clear()
        super.onClose()
    }

    override fun resize(minecraft: Minecraft, width: Int, height: Int) {
        state.currentPage.progress = 0.0f
        super.resize(minecraft, width, height)
    }

    // Animates the visible scroll position toward the page's target progress; port of LP1's
    // updateScreen. Without this, slider/mouse-wheel changes update page.progress but the
    // rendered content (which scrolls by currentProgress) never moves.
    override fun tick() {
        super.tick()
        if (currentProgress == state.currentPage.progress) return
        val progressDiff = currentProgress - state.currentPage.progress
        val speedModifier = 0.5f
        currentProgress = when {
            progressDiff < 0.0025f && progressDiff > -0.0025f -> state.currentPage.progress
            progressDiff < 0.0025f -> min(currentProgress - (progressDiff * speedModifier), state.currentPage.progress)
            progressDiff > -0.0025f -> max(currentProgress - (progressDiff * speedModifier), state.currentPage.progress)
            else -> currentProgress
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Publish the current GuiGraphics so the guide book drawables (DrawablePage/DrawableWord/
        // LPFontRenderer) and GuiDrawer helpers can draw against it; mirrors how BaseGuiContainer
        // exposes it for the rest of LP2's GUI code.
        val previousGraphics = SimpleGraphics.guiGraphics
        SimpleGraphics.guiGraphics = guiGraphics
        try {
            // Darken the world behind the book (equivalent of LP1's drawDefaultBackground).
            renderBackground(guiGraphics, mouseX, mouseY, partialTick)

            // Background panel (equivalent of LP1's GuiDrawer.drawGuideBookBackground): a tiled dark
            // pattern inset 16px from the outer rectangle, so it sits behind the 24px frame border.
            GuideBookGraphics.blitDarkPatternTiled(
                guiGraphics,
                MutableRectangle.fromRectangle(outerGui).translate(16, 16).grow(-32, -32),
            )

            // Page contents, scrolled to the current progress.
            state.currentPage.run {
                updateScrollPosition(visibleArea, currentProgress)
                draw(visibleArea, mouseX.toFloat(), mouseY.toFloat(), partialTick)
            }

            // Inactive bookmark tab bodies render under the frame (LP1 drew them before the frame).
            tabButtons.forEach { it.render(guiGraphics, mouseX, mouseY, partialTick) }

            // Frame and slider rail.
            GuideBookGraphics.blitGuideBookFrame(guiGraphics, outerGui, sliderSeparator)

            // Remaining registered widgets (slider, home, add/remove bookmark) render over the frame.
            renderables.forEach { it.render(guiGraphics, mouseX, mouseY, partialTick) }

            // Foreground pass over the frame (LP1's drawButtonForegroundLayer, in reversed order):
            // the active tab's body + colored circle and the tab tooltips, which must not be
            // overdrawn by the opaque frame border.
            tabButtons.reversed().forEach { it.renderForeground(guiGraphics, mouseX, mouseY) }

            // Hovered tooltip for the page content.
            if (visibleArea.contains(mouseX, mouseY)) {
                val hovered = state.currentPage.getHovered(mouseX.toFloat(), mouseY.toFloat())
                if (hovered is Tooltipped) {
                    GuiDrawer.drawTextTooltip(
                        text = hovered.getTooltipText(),
                        x = mouseX,
                        y = min(mouseY - 5f, visibleArea.bottom).roundToInt(),
                        z = GuideBookConstants.Z_TOOLTIP,
                        horizontalAlign = HorizontalAlignment.CENTER,
                        verticalAlign = VerticalAlignment.BOTTOM,
                    )
                }
            }

            drawTitle()
        } finally {
            SimpleGraphics.guiGraphics = previousGraphics
        }
    }

    private fun drawTitle() {
        GuiDrawer.lpFontRenderer.drawCenteredString(
            state.currentPage.title,
            floor(width / 2.0f),
            outerGui.y0 + (innerGui.y0 - outerGui.y0 - GuiDrawer.lpFontRenderer.getFontHeight()) / 2.0f,
            MinecraftColor.WHITE.colorCode,
            EnumSet.of(TextFormat.Shadow),
            1.0f,
        )
    }

    override fun isPauseScreen(): Boolean = false

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // LP1 dispatched button clicks first (mousePressed -> actionPerformed/rightClick) and only
        // then forwarded the click to the page content. The widgets (slider, home, bookmark
        // manager, tabs) handle their own actions in their mouseClicked overrides.
        if (super.mouseClicked(mouseX, mouseY, button)) {
            updateButtonVisibility()
            return true
        }
        if (visibleArea.contains(mouseX.toInt(), mouseY.toInt())) {
            state.currentPage.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), button, visibleArea, actionListener)
            return true
        }
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (state.currentPage.getExtraHeight(visibleArea) > 0 && this::slider.isInitialized) {
            slider.changeProgress((scrollY * -GuiDrawer.lpFontRenderer.getFontHeight(1.0f)).toInt())
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    private fun addBookmark() = state.currentPage.takeIf { isTabAbsent(it) && tabButtons.size < maxTabs }
        ?.also {
            state.bookmarks.add(it)
            // Register as events-only widget (rendered manually in render()), matching init().
            tabButtons.add(addWidget(createGuiTabButton(it)))
        }

    private fun createGuiTabButton(tabPage: Page): TabButton =
        TabButton(tabPage, outerGui.roundedRight - 2 - 2 * guiTabWidth, outerGui.roundedTop, object : TabButtonReturn {
            override fun onLeftClick(): Boolean {
                if (!isPageActive()) { changePage(tabPage.page); return true }
                return false
            }
            override fun onRightClick(shiftClick: Boolean, ctrlClick: Boolean): Boolean {
                if (!isPageActive()) return false
                if (ctrlClick && shiftClick) removeBookmark(tabPage) else tabPage.cycleColor(inverted = shiftClick)
                return true
            }
            override fun getColor(): Int =
                tabPage.color ?: cycleMinecraftColorId(freeColor).also { freeColor = it; tabPage.color = it }
            override fun isPageActive(): Boolean = tabPage.pageEquals(state.currentPage)
        })

    private fun removeBookmark(page: Page): Boolean {
        val removedFromState = state.bookmarks.removeIf { it.pageEquals(page) }
        // De-register the matching tab widgets from event dispatch before dropping them from the list.
        tabButtons.filter { it.tabPage.pageEquals(page) }.forEach(::removeWidget)
        val removedFromButtons = tabButtons.removeIf { it.tabPage.pageEquals(page) }
        return removedFromState || removedFromButtons
    }
}
