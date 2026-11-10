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

package network.rs485.logisticspipes.integration

import network.rs485.minecraft.BlockPosSelector
import network.rs485.minecraft.WorldBuilder
import network.rs485.minecraft.minus
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.core.Vec3i
import net.minecraft.server.level.ServerLevel
import kotlin.math.min

const val LEVEL = 100
val ONE_VECTOR = Vec3i(1, 1, 1)

class TestWorldBuilder(override val world: ServerLevel, val firstBlockPos: BlockPos) : WorldBuilder {

    private val selectors = ArrayList<Pair<BlockPosSelector, BlockPos>>()

    private var nextPos = firstBlockPos
    private var lowest = LEVEL

    private val chunksToLoad: MutableSet<ChunkPos> = HashSet()

    fun newSelector() = BlockPosSelector(worldBuilder = this)

    override fun finalPosition(selector: BlockPosSelector): BlockPos = (nextPos - selector.localStart).also {
        selectors.add(selector to it)
        val start = it.offset(selector.localStart)
        val borderStart = start.subtract(ONE_VECTOR)
        val end = it.offset(selector.localEnd)
        val borderEnd = end.offset(ONE_VECTOR)
        lowest = min(lowest, borderStart.y)
        nextPos = BlockPos(borderEnd.x + 2, LEVEL, 0)
        world.setBlocksInRange(borderStart, borderEnd, Blocks.AIR.defaultBlockState())
        // Border platform
        val slabState = Blocks.SMOOTH_STONE_SLAB.defaultBlockState()
        world.setBlocksInRange(BlockPos(borderStart.x, lowest, borderStart.z), BlockPos(borderStart.x, lowest, borderEnd.z), slabState)
        world.setBlocksInRange(BlockPos(borderStart.x, lowest, borderEnd.z), BlockPos(borderEnd.x, lowest, borderEnd.z), slabState)
        world.setBlocksInRange(BlockPos(borderEnd.x, lowest, borderStart.z), BlockPos(borderEnd.x, lowest, borderEnd.z), slabState)
        world.setBlocksInRange(BlockPos(borderStart.x, lowest, borderStart.z), BlockPos(borderEnd.x, lowest, borderStart.z), slabState)
        world.setBlocksInRange(BlockPos(start.x, lowest, start.z), BlockPos(end.x, lowest, end.z), Blocks.STONE.defaultBlockState())
    }

    override fun loadChunk(pos: ChunkPos) {
        if (chunksToLoad.add(pos)) {
            world.setChunkForced(pos.x, pos.z, true)
        }
    }

    fun buildSpawnPlatform(): BlockPos {
        val start = firstBlockPos.subtract(ONE_VECTOR)
        world.setBlocksInRange(
            BlockPos(start.x - 5, start.y, start.z),
            BlockPos(start.x - 1, start.y, start.z + 4),
            Blocks.OBSIDIAN.defaultBlockState(),
        )
        return BlockPos(start.x - 3, firstBlockPos.y, start.z + 2)
    }

}

private fun ServerLevel.setBlocksInRange(start: BlockPos, end: BlockPos, state: BlockState) =
    blocksIn(start, end).forEach { setBlock(it, state, 3) }

private fun blocksIn(start: BlockPos, end: BlockPos): List<BlockPos> {
    assert(start.x <= end.x)
    assert(start.y <= end.y)
    assert(start.z <= end.z)
    return (start.x..end.x).flatMap { x ->
        (start.y..end.y).flatMap { y ->
            (start.z..end.z).map { z -> BlockPos(x, y, z) }
        }
    }
}
