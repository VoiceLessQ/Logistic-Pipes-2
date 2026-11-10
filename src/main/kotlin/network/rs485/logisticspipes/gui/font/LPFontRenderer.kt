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

import network.rs485.grow.Coroutines
import network.rs485.logisticspipes.util.alpha
import network.rs485.logisticspipes.util.blue
import network.rs485.logisticspipes.util.green
import network.rs485.logisticspipes.util.red
import network.rs485.markdown.*
import logisticspipes.LPConstants
import logisticspipes.LogisticsPipes
import logisticspipes.utils.gui.SimpleGraphics
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.tan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class LPFontRenderer(private val fontName: String) {
    companion object Factory {
        private val fontRenderersOfThisWorld = ConcurrentHashMap<String, LPFontRenderer>()
        private val preloadFonts = listOf("ter-u12n")
        fun get(fontName: String): LPFontRenderer = fontRenderersOfThisWorld.getOrPut(fontName) { LPFontRenderer(fontName) }

        @ExperimentalCoroutinesApi
        fun asyncPreload() {
            Coroutines.ioScope.launch {
                preloadFonts.map {
                    async {
                        get(it).apply { ::fontPlain.get() }
                    }
                }.forEach { deferred ->
                    deferred.invokeOnCompletion {
                        if (it != null) {
                            LogisticsPipes.log.error("Error while preloading fonts:\n${it.stackTraceToString()}")
                        } else {
                            val fontRenderer = deferred.getCompleted()
                            LogisticsPipes.log.info("Preloaded font files: ${fontRenderer.fontName}")
                            Minecraft.getInstance().execute {
                                fontRenderer::wrapperPlain.get()
                                LogisticsPipes.log.info("Created font textures for: ${fontRenderer.fontName}")
                            }
                        }
                    }
                }
            }
        }
    }

    private val fontPlain: IFont by lazy {
        val initialTime = System.currentTimeMillis()
        val fontResourcePlain = Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "fonts/$fontName.bdf")
        FontParser.read(fontResourcePlain).also { LogisticsPipes.log.info("Elapsed time parsing font: ${System.currentTimeMillis() - initialTime}ms") }
            ?: throw IOException("Failed to load ${fontResourcePlain.path}, this is not tolerated.")
    }

    // builds GL textures, so only touch it on the render thread
    private val wrapperPlain: FontWrapper by lazy {
        val initialTime = System.currentTimeMillis()
        FontWrapper(fontPlain, fontName).also { LogisticsPipes.log.info("Elapsed time wrapping font: ${System.currentTimeMillis() - initialTime}ms") }
    }

    private val shadowColor = 0xEE3C3F41.toInt()
    var zLevel: Float = 5f

    fun width(text: String): Int = getStringWidth(text)

    /**
     * Adds the given char at the given spot with the set color to the buffer and returns the xOffset for the next character.
     *
     * @param char    Character to be drawn
     * @param x       X coordinate to start drawing the character (left boundary)
     * @param y       Y coordinate to draw the character. (top of the line)
     * @param color   ARGB color for the character to be drawn.
     * @param wrapper This is passed so you could draw with different fonts in quick succession if needed.
     * @param italics This defines if the characters will have a front lean to them.
     * @param scale   This defines the size of the character to be drawn.
     * @return the width of the drawn character
     */
    private fun putChar(gg: GuiGraphicsExtractor, char: Char, x: Float, y: Float, color: Int, wrapper: FontWrapper, italics: Boolean, scale: Float): Float {
        // Fall back to the default char when this one has no glyph, draw nothing when neither has one
        val c = if (wrapper.getTexture(char) != null) char else wrapper.defaultChar
        val texture = wrapper.getTexture(c) ?: return 0f
        // Width of the drawn character texture
        val width = wrapper.getFontTextureSize()
        // Height of the drawn character texture
        val height = wrapper.getFontTextureSize()
        // X position of the drawn character
        val textureX = wrapper.getGlyphX(c)
        // Y position of the drawn character
        val textureY = wrapper.getGlyphY(c)
        // The actual character glyph containing the actual character size and offsets.
        val glyph = wrapper.getGlyph(c)
        // In case any of the above fails to be set return 0 without adding anything to the buffer.
        if (textureX == -1 || textureY == -1 || glyph == null) return 0f
        // Scaled character dimensions.
        val sWidth = (glyph.width * scale)
        val sHeight = (glyph.height * scale)
        // Character draw coordinate calculation based on scale
        val x0 = x - (glyph.offsetX * scale)
        val y1 = y + ((wrapper.fontLineOffset - glyph.offsetY) * scale)
        val x1 = x0 + sWidth
        val y0 = y1 - sHeight
        // The offset distance requires a tan calculation because merely adding a fixed value independent of the character's height would lead to slightly different angles for each character.
        val italicsOffset = if (italics) glyph.height * tan(0.2181662f) else 0f
        // 26.x GUI rendering is deferred blits on a 2D pose: place, shear (italics) and scale the glyph quad, then blit its atlas cell
        val pose = gg.pose()
        pose.pushMatrix()
        pose.translate(x0, y0)
        if (italics) pose.mul(Matrix3x2f(1f, 0f, -italicsOffset / sHeight, 1f, italicsOffset, 0f))
        pose.scale(sWidth / glyph.width, sHeight / glyph.height)
        gg.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, textureX.toFloat(), textureY.toFloat(), glyph.width, glyph.height, width, height, color)
        pose.popMatrix()
        // Return the final width of the character, including the spacing for the next character, while being scaled or bypassing scaling in case the scale is set to 1.
        return glyph.dWidthX * scale
    }

    /**
     * Extension of the previous draw(char), that just implements functionality for bold characters, depending on the available wrappers
     */
    private fun putChar(gg: GuiGraphicsExtractor, char: Char, x: Float, y: Float, color: Int, italics: Boolean, bold: Boolean, scale: Float): Float {
        return if (bold) {
            putChar(gg, char, x + scale, y, color, wrapperPlain, italics, scale)
            putChar(gg, char, x, y, color, wrapperPlain, italics, scale) + scale
        } else {
            putChar(gg, char, x, y, color, wrapperPlain, italics, scale)
        }
    }

    /**
     * Extension of the previous draw(char), that just implements functionality for shadowed characters.
     */
    private fun putChar(gg: GuiGraphicsExtractor, c: Char, x: Float, y: Float, color: Int, italics: Boolean, bold: Boolean, shadow: Boolean, scale: Float): Float {
        if (shadow) {
            putChar(gg, c, x + scale, y + scale, shadowColor, italics, bold, scale)
        }
        return putChar(gg, c, x, y, color, italics, bold, scale)
    }

    /**
     * Draws the given string with all the possible parameters applied to it.
     */
    fun drawString(string: String, x: Float, y: Float, color: Int, format: Set<TextFormat>, scale: Float): Int =
        drawString(string = string, x = x, y = y, color = color, italic = format.italic(), bold = format.bold(), shadow = format.shadow(), underline = format.underline(), strikethrough = format.strikethrough(), scale = scale)

    /**
     * Draws the given string with all the possible parameters applied to it.
     */
    private fun drawString(string: String, x: Float, y: Float, color: Int, italic: Boolean, bold: Boolean, shadow: Boolean, underline: Boolean, strikethrough: Boolean, scale: Float): Int {
        val gg = SimpleGraphics.guiGraphics ?: return getStringWidth(string, italic, bold, scale)
        var stringSize = string.fold(0f) { currentX, char -> currentX + putChar(gg, char, x + currentX, y, color, italic, bold, shadow, scale) }
        if (underline || strikethrough) {
            putOverlayFormatting(gg = gg, x = x, y = y, width = stringSize, color = color, italic = italic, underline = underline, strikethrough = strikethrough, shadow = shadow, scale = scale)
        }
        if (italic) stringSize += scale
        return ceil(stringSize).toInt()
    }

    /**
     * Adds the formatting lines to the buffer, for underline and strikethrough.
     */
    private fun putOverlayFormatting(gg: GuiGraphicsExtractor, x: Float, y: Float, width: Float, color: Int, italic: Boolean, underline: Boolean, strikethrough: Boolean, shadow: Boolean, scale: Float) {
        if (underline) {
            val underlineY = (wrapperPlain.fontHeight - 1) * scale
            if (shadow) putHorizontalLine(gg = gg, x = x + scale, y = y + underlineY + scale, width = width, thickness = scale, color = shadowColor, italics = italic)
            putHorizontalLine(gg = gg, x = x, y = y + underlineY, width = width, thickness = scale, color = color, italics = italic)
        }
        if (strikethrough) {
            val strikethroughY = ((wrapperPlain.fontHeight) / 2f) * scale
            if (shadow) putHorizontalLine(gg = gg, x = x + scale, y = y + strikethroughY + scale, width = width, thickness = scale, color = shadowColor, italics = italic)
            putHorizontalLine(gg = gg, x = x, y = y + strikethroughY, width = width, thickness = scale, color = color, italics = italic)
        }
    }

    fun drawSpace(x: Float, y: Float, width: Int, color: Int, italic: Boolean, underline: Boolean, strikethrough: Boolean, shadow: Boolean, scale: Float): Int {
        if (width > 0 && (underline || strikethrough)) {
            val gg = SimpleGraphics.guiGraphics ?: return width
            putOverlayFormatting(gg = gg, x = x, y = y, width = width.toFloat(), color = color, italic = italic, underline = underline, strikethrough = strikethrough, shadow = shadow, scale = scale)
        }
        return width
    }

    fun drawCenteredString(string: String, x: Float, y: Float, color: Int, tags: Set<TextFormat>, scale: Float): Int {
        return drawString(string, x - (getStringWidth(string, tags, scale) / 2f), y, color, tags, scale)
    }

    // It's not being rounded because it's supposed to be floored.
    fun getFontHeight(scale: Float = 1f): Int = (fontPlain.height * scale).toInt()

    private fun getCharWidth(char: Char, scale: Float): Float {
        val glyph = fontPlain.glyphs[char] ?: fontPlain.glyphs[fontPlain.defaultChar] ?: return 0f
        return glyph.dWidthX * scale
    }

    private fun getCharWidth(char: Char, bold: Boolean, scale: Float): Float {
        return if (bold) {
            getCharWidth(char, scale) + 1
        } else {
            getCharWidth(char, scale)
        }
    }

    fun getStringWidth(string: String, italics: Boolean, bold: Boolean, scale: Float): Int {
        val italicsOffset = if (italics) scale else 0f
        return (string.fold(0.0) { currentX, char -> currentX + getCharWidth(char, bold, scale) } + italicsOffset).toInt()
    }

    fun getStringWidth(string: String, tags: Set<TextFormat>, scale: Float): Int =
        getStringWidth(string, tags.italic(), tags.bold(), scale)

    fun getStringWidth(string: String): Int = getStringWidth(string = string, italics = false, bold = false, scale = 1f)

    private fun putHorizontalLine(gg: GuiGraphicsExtractor, x: Float, y: Float, width: Float, thickness: Float, color: Int, italics: Boolean) {
        val italicsOffset = if (italics) thickness else 0f
        val pose = gg.pose()
        pose.pushMatrix()
        pose.translate(x, y)
        pose.scale(width + italicsOffset, thickness)
        gg.fill(0, 0, 1, 1, color)
        pose.popMatrix()
    }
}
