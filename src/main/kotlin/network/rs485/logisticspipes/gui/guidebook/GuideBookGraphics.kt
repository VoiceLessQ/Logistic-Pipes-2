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

import logisticspipes.LPConstants
import network.rs485.logisticspipes.util.IRectangle
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import network.rs485.logisticspipes.gui.blitNineSliced
import net.minecraft.resources.Identifier

/**
 * Guide book texture-atlas drawing, ported from LP1's [GuiDrawer.drawGuiTexturedRect]/`putTexturedQuad`
 * (BufferBuilder + GlStateManager) to the 1.20.1 [GuiGraphicsExtractor] blit idiom.
 *
 * LP1 packs every guide book widget texture into a single 256x256 atlas (`textures/gui/gui.png`).
 * The widget classes pass a destination [IRectangle] (screen-space) and a source [IRectangle] (atlas
 * UV-space, in pixels); this helper maps those onto [GuiGraphicsExtractor.blit] using the
 * `(texture, dstX, dstY, dstW, dstH, srcU, srcV, srcW, srcH, atlasW, atlasH)` overload so that
 * arbitrary scaling between the source and destination is preserved (matching LP1's quad UV mapping).
 */
internal object GuideBookGraphics {

    const val ATLAS_SIZE = 256
    val GUI_ATLAS = Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "textures/gui/gui.png")
    val GUI_DARK_PATTERN = Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "textures/gui/dark.png")

    /**
     * Blits a region of the guide book atlas. The destination rectangle is screen-space; the texture
     * rectangle is the source region within the atlas (in atlas pixels). Mirrors LP1's
     * `drawGuiTexturedRect(rect, text, blend, color)` including the optional color tint, which 1.20.1
     * expresses as the blit color.
     */
    fun blitAtlas(
        guiGraphics: GuiGraphicsExtractor,
        dst: IRectangle,
        src: IRectangle,
        color: Int = -1,
        blend: Boolean = true,
    ) {
        // GUI_TEXTURED always blends; the shader color is now the per-blit ARGB color
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            GUI_ATLAS,
            dst.roundedLeft,
            dst.roundedTop,
            src.left,
            src.top,
            dst.roundedWidth,
            dst.roundedHeight,
            src.roundedWidth,
            src.roundedHeight,
            ATLAS_SIZE,
            ATLAS_SIZE,
            color,
        )
    }

    /**
     * Draws the guide book frame (border + corners) plus the slider rail. Ported from LP1's
     * [GuiDrawer.drawGuideBookFrame], which nine-sliced the 64x64 `guiGuidebookFrame` atlas region
     * (24px borders) over the outer rectangle and drew the slider rail from the `guiGuidebookSlider`
     * region. In 1.20.1 the nine-slice is expressed directly via [GuiGraphicsExtractor.blitNineSliced].
     */
    fun blitGuideBookFrame(guiGraphics: GuiGraphicsExtractor, frame: IRectangle, slider: IRectangle) {
        // Frame: source region (0,0)-(64,64) with a 24px border on each edge.
        guiGraphics.blitNineSliced(
            GUI_ATLAS,
            frame.roundedLeft,
            frame.roundedTop,
            frame.roundedWidth,
            frame.roundedHeight,
            24, 24,
            64, 64,
            0, 0,
            ATLAS_SIZE, ATLAS_SIZE,
        )
        // Slider rail/separator: source region (96,64)-(112,80) with 1px top/bottom caps and the
        // middle STRETCHED, like LP1's BorderedRectangle(slider, 1, 0, 1, 0). blitNineSliced is
        // wrong here: it TILES the edges/center (blitRepeating), and the 18px-wide destination from
        // the 16px source adds a partial slice sampled from the texture center — an opaque 2px
        // stripe instead of the transparent shadow gradient.
        val railLeft = slider.roundedLeft
        val railTop = slider.roundedTop
        val railWidth = slider.roundedWidth
        val railHeight = slider.roundedHeight
        // Top cap (1px), stretched horizontally.
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_ATLAS, railLeft, railTop, 96f, 64f, railWidth, 1, 16, 1, ATLAS_SIZE, ATLAS_SIZE)
        // Middle, stretched both ways.
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_ATLAS, railLeft, railTop + 1, 96f, 65f, railWidth, railHeight - 2, 16, 14, ATLAS_SIZE, ATLAS_SIZE)
        // Bottom cap (1px), stretched horizontally.
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_ATLAS, railLeft, railTop + railHeight - 1, 96f, 79f, railWidth, 1, 16, 1, ATLAS_SIZE, ATLAS_SIZE)
    }

    /**
     * Tiles the [GUI_DARK_PATTERN] over [dst]. Ported from LP1's [GuiDrawer.drawGuideBookBackground],
     * which painted the inner panel with a repeating dark texture. The pattern texture is 64x64 and is
     * tiled by repeated 64x64 blits clipped to [dst].
     */
    fun blitDarkPatternTiled(guiGraphics: GuiGraphicsExtractor, dst: IRectangle) {
        val tile = 64
        val left = dst.roundedLeft
        val top = dst.roundedTop
        val right = dst.roundedRight
        val bottom = dst.roundedBottom
        var x = left
        while (x < right) {
            var y = top
            val w = minOf(tile, right - x)
            while (y < bottom) {
                val h = minOf(tile, bottom - y)
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_DARK_PATTERN, x, y, 0.0f, 0.0f, w, h, tile, tile)
                y += tile
            }
            x += tile
        }
    }
}
