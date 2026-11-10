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
 * of the Source Code, and to permit persons to whom the Software is furnished to
 * do so, subject to the following conditions:
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

package network.rs485.logisticspipes.connection

import logisticspipes.pipes.basic.CoreRoutedPipe
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import java.util.*

class DynamicAdjacent(private val parent: CoreRoutedPipe, private val cache: Array<ConnectionType?>) : Adjacent {
    override fun connectedPos(): Map<BlockPos, ConnectionType> = cache
        .mapIndexedNotNull { index, type -> type?.let { parent.getPos().relative(Direction.values()[index]) to type } }
        .let { it.associateTo(LinkedHashMap(it.size)) { pair -> pair } }

    override fun get(direction: Direction): ConnectionType? = cache[direction.get3DDataValue()]

    override fun optionalGet(direction: Direction): Optional<ConnectionType> = Optional.ofNullable(cache[direction.get3DDataValue()])

    override fun neighbors(): Map<NeighborTileEntity<BlockEntity>, ConnectionType> = cache
        .mapIndexedNotNull { index, connectionType ->
            connectionType?.let {
                Direction.values()[index].let { dir ->
                    parent.getWorld()?.getBlockEntity(parent.getPos().relative(dir))?.let { LPNeighborTileEntity(it, dir) to connectionType }
                }
            }
        }
        .let { it.associateTo(LinkedHashMap(it.size)) { pair -> pair } }

    override fun inventories() = cache
        .mapIndexedNotNull { index, type ->
            if (type?.isItem() != true) return@mapIndexedNotNull null
            Direction.values()[index].let { dir ->
                parent.getWorld()?.getBlockEntity(parent.getPos().relative(dir))?.let { it to dir }
            }
        }
        .mapNotNull { (tile, dir) -> LPNeighborTileEntity(tile, dir).takeIf { it.canHandleItems() } }

    override fun fluidTanks(): List<NeighborTileEntity<BlockEntity>> = cache
        .mapIndexedNotNull { index, type ->
            if (type?.isFluid() != true) return@mapIndexedNotNull null
            Direction.values()[index].let { dir ->
                parent.getWorld()?.getBlockEntity(parent.getPos().relative(dir))?.let { it to dir }
            }
        }
        .mapNotNull { (tile, dir) -> LPNeighborTileEntity(tile, dir).takeIf { it.canHandleFluids() } }

    override fun toString(): String = "DynamicAdjacent(${Direction.values().withIndex().joinToString { "{${it.value.getName()}: ${cache[it.index]}}" }})"
}
