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

import net.minecraft.client.renderer.RenderPipelines
import network.rs485.logisticspipes.gui.GuiDrawer
import network.rs485.logisticspipes.util.IRectangle
import network.rs485.logisticspipes.util.math.MutableRectangle
import logisticspipes.LogisticsPipes
import logisticspipes.utils.MinecraftColor
import logisticspipes.utils.gui.SimpleGraphics
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import java.io.IOException

class DrawableImageParagraph(private val alternativeText: List<DrawableWord>, val image: DrawableImage) : DrawableParagraph() {
    override val relativeBody = MutableRectangle()
    override var parent: Drawable? = null

    override fun setPos(x: Int, y: Int): Pair<Int, Int> {
        relativeBody.setPos(x, y)
        // This has to be done in two steps because setChildrenPos() depends on the width already being set.
        relativeBody.setSize(parent!!.width, 0)
        relativeBody.setSize(relativeBody.roundedWidth, setChildrenPos())
        return relativeBody.roundedWidth to relativeBody.roundedHeight
    }

    override fun setChildrenPos(): Int {
        var currentY = 0
        currentY += if (image.broken) {
            splitAndInitialize(alternativeText, 5, currentY, width - 10, false)
        } else {
            image.setPos(0, currentY).y
        }
        return currentY
    }

    override fun getHovered(mouseX: Float, mouseY: Float): Drawable? = if (image.broken) {
        alternativeText.firstOrNull { it.isMouseHovering(mouseX, mouseY) }
    } else {
        null
    }

    override fun draw(mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        if (image.broken) {
            super.draw(mouseX, mouseY, delta, visibleArea)
            for (drawableWord in alternativeText.filter { it.visible(visibleArea) }) {
                drawableWord.draw(mouseX, mouseY, delta, visibleArea)
                GuiDrawer.drawOutlineRect(absoluteBody, MinecraftColor.WHITE.colorCode)
            }
        } else {
            image.draw(mouseX, mouseY, delta, visibleArea)
        }
    }
}

class DrawableImage(private var imageResource: Identifier) : Drawable {

    override var relativeBody: MutableRectangle = MutableRectangle()
    override var parent: Drawable? = null

    // 1.20.1 has no PngSizeInfo; read the PNG IHDR header directly from the resource stream to get
    // the intrinsic dimensions (used both for layout sizing and as the blit source size).
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    val broken: Boolean

    init {
        var success = false
        try {
            val resource = Minecraft.getInstance().resourceManager.getResource(imageResource)
            if (resource.isPresent) {
                resource.get().open().use { stream ->
                    val (w, h) = readPngSize(stream.readNBytes(24))
                    if (w > 0 && h > 0) {
                        imageWidth = w
                        imageHeight = h
                        success = true
                    }
                }
            }
        } catch (e: IOException) {
            LogisticsPipes.log.error("File not found: ${imageResource.path}")
        }
        broken = !success
    }

    /**
     * Reads width/height from a PNG IHDR chunk (big-endian 32-bit ints at byte offsets 16 and 20).
     * Returns 0 to 0 if the header does not look like a PNG.
     */
    private fun readPngSize(header: ByteArray): Pair<Int, Int> {
        if (header.size < 24) return 0 to 0
        // PNG signature: 89 50 4E 47 0D 0A 1A 0A
        if (header[1].toInt() != 'P'.code || header[2].toInt() != 'N'.code || header[3].toInt() != 'G'.code) return 0 to 0
        fun int32(offset: Int): Int =
            (header[offset].toInt() and 0xFF shl 24) or
                (header[offset + 1].toInt() and 0xFF shl 16) or
                (header[offset + 2].toInt() and 0xFF shl 8) or
                (header[offset + 3].toInt() and 0xFF)
        return int32(16) to int32(20)
    }

    override fun draw(mouseX: Float, mouseY: Float, delta: Float, visibleArea: IRectangle) {
        if (!broken) {
            val guiGraphics = SimpleGraphics.guiGraphics ?: return
            // Clip to the visible page area so partially-scrolled images do not draw over the frame.
            guiGraphics.enableScissor(
                visibleArea.roundedLeft,
                visibleArea.roundedTop,
                visibleArea.roundedRight,
                visibleArea.roundedBottom,
            )
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                imageResource,
                absoluteBody.roundedLeft,
                absoluteBody.roundedTop,
                0.0f, 0.0f,
                absoluteBody.roundedWidth,
                absoluteBody.roundedHeight,
                imageWidth, imageHeight,
                imageWidth, imageHeight,
            )
            guiGraphics.disableScissor()
        } else {
            GuiDrawer.drawOutlineRect(absoluteBody, MinecraftColor.WHITE.colorCode)
        }
    }

    override fun setPos(x: Int, y: Int): Pair<Int, Int> {
        if (!broken) {
            relativeBody.setSize(imageWidth, imageHeight)
            if (imageWidth > (parent?.width ?: 0) && (parent?.width ?: 0) > 0) {
                val downScaleFactor = parent!!.width.toFloat() / width
                relativeBody.scale(downScaleFactor)
            }
        } else {
            relativeBody.setSize(20, 20)
        }
        return super.setPos(x, y)
    }
}
