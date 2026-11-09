/*
 * Copyright (c) 2023  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2023  RS485
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

package network.rs485.logisticspipes.property.layer

import network.rs485.logisticspipes.property.InventoryProperty
import net.minecraft.world.Container
import net.minecraft.world.entity.ContainerUser
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class PropertyOverlayInventoryAdapter<T : Container, out P : InventoryProperty<T>>(
    private val propertyOverlay: PropertyOverlay<T, P>,
) : Container {
    override fun getContainerSize(): Int = propertyOverlay.read { it.containerSize }

    override fun isEmpty(): Boolean = propertyOverlay.read { it.isEmpty }

    @Deprecated("do not modify returned ItemStack, handle as immutable")
    override fun getItem(index: Int): ItemStack = propertyOverlay.read { it.getItem(index) }

    override fun removeItem(index: Int, count: Int): ItemStack =
        propertyOverlay.write { it.removeItem(index, count) }

    override fun removeItemNoUpdate(index: Int): ItemStack = propertyOverlay.write { it.removeItemNoUpdate(index) }

    override fun setItem(index: Int, stack: ItemStack) =
        propertyOverlay.write { it.setItem(index, stack) }

    override fun getMaxStackSize(): Int = propertyOverlay.read { it.getMaxStackSize() }

    override fun setChanged() = propertyOverlay.write { it.setChanged() }

    override fun stillValid(player: Player): Boolean = propertyOverlay.read { it.stillValid(player) }

    @Deprecated("no-op on adapter")
    override fun startOpen(user: ContainerUser) {
    }

    @Deprecated("no-op on adapter")
    override fun stopOpen(user: ContainerUser) {
    }

    override fun canPlaceItem(index: Int, stack: ItemStack): Boolean =
        propertyOverlay.read { it.canPlaceItem(index, stack) }

    override fun clearContent() = propertyOverlay.write { it.clearContent() }
}
