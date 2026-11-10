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

package network.rs485.logisticspipes.gui.font

import com.mojang.blaze3d.platform.NativeImage
import logisticspipes.LPConstants
import logisticspipes.LogisticsPipes
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import kotlin.math.max

class FontWrapper(private val font: IFont, private val fontName: String) {
    val textures: List<Identifier> get() = pages.toList()

    private var glyphPosX: Map<Char, Int> = emptyMap()
    private var glyphPosY: Map<Char, Int> = emptyMap()
    private var glyphPage: Map<Char, Int> = emptyMap()
    private val pages = mutableListOf<Identifier>()

    val fontWidth: Int get() = font.width
    val fontHeight: Int get() = font.height
    val fontXOffset: Int get() = font.offsetX
    val fontYOffset: Int get() = font.offsetY
    val fontLineOffset: Int get() = fontHeight + fontYOffset

    val defaultChar = font.defaultChar

    private val maxTexSize = 512

    init {
        allocateTextures()
    }

    // Creates the necessary textures with all the glyphs in them
    private fun allocateTextures(): Pair<Int, Int> {
        var image: NativeImage? = null
        var currentWidth = 0
        var currentHeight = 0
        var currentRowMaxHeight = 0
        var currentMaxHeight = 0
        var currentMinOffset = 0

        fun uploadPage() {
            val img = image ?: return
            // DynamicTexture uploads on creation and samples with nearest filtering
            val location = Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "font/${fontName}_${pages.size}")
            Minecraft.getInstance().textureManager.register(location, DynamicTexture({ location.toString() }, img))
            pages.add(location)
            image = null
        }

        fun startNewRow() {
            // Use gathered row data to keep track of highest character.
            currentMaxHeight = max(currentMaxHeight, currentRowMaxHeight)
            // Use gathered row data to safely start the next row with interfering with the previous ones.
            currentHeight += currentRowMaxHeight.powerOf2()
            // Reset row variables.
            currentWidth = 0
            currentRowMaxHeight = 0
        }

        fun startNewTexture() {
            uploadPage()
            currentWidth = 0
            currentHeight = 0
            currentRowMaxHeight = 0
            image = NativeImage(maxTexSize, maxTexSize, true)
        }

        fun allocateGlyph(character: Char, glyph: IGlyph) {
            if (currentWidth + glyph.width > maxTexSize)
                error("Cannot write a texture of width=${glyph.width} at x=$currentWidth")
            if (currentHeight + glyph.height > maxTexSize)
                error("Cannot write a texture of height=${glyph.height} at y=$currentHeight")
            // Store the location of the glyph
            glyphPosX = glyphPosX + (character to currentWidth)
            glyphPosY = glyphPosY + (character to currentHeight)
            glyphPage = glyphPage + (character to pages.size)
            // Draw the glyph into the texture, set bits white and opaque
            val img = image!!
            for (i in 0 until glyph.width * glyph.height) {
                img.setPixel(currentWidth + i % glyph.width, currentHeight + i / glyph.width, if (glyph.bitmap[i]) -1 else 0)
            }
            // After allocation
            currentWidth += glyph.width.powerOf2()
            currentRowMaxHeight = maxOf(currentRowMaxHeight, glyph.height)
            currentMinOffset = minOf(currentMinOffset, glyph.offsetY)
        }

        startNewTexture()
        for ((character, glyph) in font.glyphs) {
            if (currentWidth + glyph.width > maxTexSize) startNewRow()
            if (currentHeight + glyph.height > maxTexSize) startNewTexture()
            allocateGlyph(character, glyph)
        }
        uploadPage()
        LogisticsPipes.log.info("Font $fontName uses ${pages.size} texture page(s) for ${font.glyphs.size} glyphs")

        return currentMaxHeight to currentMinOffset
    }

    // Getter for the glyph's atlas page, returns null if the char has no glyph.
    fun getTexture(c: Char): Identifier? = glyphPage[c]?.let { pages[it] }

    // Getter for width, return -1 if char not found.
    fun getFontTextureSize(): Int = maxTexSize

    // Getter for the glyph's X coordinate
    fun getGlyphX(c: Char): Int = glyphPosX[c] ?: -1

    // Getter for the glyph's Y coordinate
    fun getGlyphY(c: Char): Int = glyphPosY[c] ?: -1

    // Getter for the Glyph object
    fun getGlyph(c: Char): IGlyph? = font.glyphs[c] ?: font.glyphs[font.defaultChar]

    // Rounds up to the next power of two.
    private fun Int.powerOf2(): Int {
        var n = this - 1
        n = n or n.ushr(1)
        n = n or n.ushr(2)
        n = n or n.ushr(4)
        n = n or n.ushr(8)
        n = n or n.ushr(16)
        return if (n < 0) 1 else if (n >= Integer.MAX_VALUE) Integer.MAX_VALUE else n + 1
    }
}
