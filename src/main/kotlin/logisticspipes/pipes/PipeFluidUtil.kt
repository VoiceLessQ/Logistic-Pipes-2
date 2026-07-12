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

package logisticspipes.pipes

import logisticspipes.interfaces.ISpecialTankAccessHandler
import logisticspipes.interfaces.ITankUtil
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe
import logisticspipes.proxy.SimpleServiceLocator
import logisticspipes.utils.FluidIdentifier
import logisticspipes.utils.FluidIdentifierStack
import logisticspipes.utils.SpecialTankUtil
import logisticspipes.utils.TankUtil
import logisticspipes.utils.item.ItemIdentifierStack
import logisticspipes.utils.tuples.Pair
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.capabilities.Capabilities
import network.rs485.logisticspipes.connection.getTankUtil

object PipeFluidUtil {

    fun getTankUtilForTE(tile: BlockEntity?, dirOnEntity: Direction?): ITankUtil? {
        if (SimpleServiceLocator.specialTankHandler.hasHandlerFor(tile)) {
            val handler = SimpleServiceLocator.specialTankHandler.getTankHandlerFor(tile)
            if (handler is ISpecialTankAccessHandler && tile != null) {
                val fluidHandler = tile.level?.getCapability(Capabilities.FluidHandler.BLOCK, tile.blockPos, dirOnEntity)
                if (fluidHandler != null) {
                    return SpecialTankUtil(fluidHandler, tile, handler)
                }
            }
        }
        if (tile != null) {
            val fluidHandler = tile.level?.getCapability(Capabilities.FluidHandler.BLOCK, tile.blockPos, dirOnEntity)
            if (fluidHandler != null) {
                return TankUtil(fluidHandler)
            }
        }
        return null
    }

    fun FluidRoutedPipe.getAdjacentTanks(listNearbyPipes: Boolean) =
        availableAdjacent.fluidTanks()
            .filter { isConnectableTank(it.tileEntity, it.direction, listNearbyPipes) }
            .flatMap { adjacent ->
                adjacent.getTankUtil()?.let { listOf(Pair(adjacent, it)) } ?: emptyList()
            }


    fun FluidRoutedPipe.getAllTankTiles(): List<BlockEntity> = getAdjacentTanks(false)
        .flatMap { pair -> SimpleServiceLocator.specialTankHandler.getBaseTileFor(pair.component1().tileEntity) }

    fun PipeFluidSatellite.fluidsToItemList(): List<ItemIdentifierStack> {
        val fluidIdentStacks = getAdjacentTanks(false)
            .flatMap { (_, util) ->
                util.tanks().toList()
                    .mapNotNull { stack -> FluidIdentifierStack.getFromStack(stack) }
            }
        val distinctionSet = HashSet<FluidIdentifier>()
        val outputList = ArrayList<ItemIdentifierStack>()
        for (identStack in fluidIdentStacks) {
            if (distinctionSet.add(identStack.fluid)) {
                outputList.add(identStack.fluid.itemIdentifier.makeStack(identStack.amount))
            } else {
                outputList.find { it.item == identStack.fluid.itemIdentifier }!!.stackSize += identStack.amount
            }
        }
        return outputList
    }

}
