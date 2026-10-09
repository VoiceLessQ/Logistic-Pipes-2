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

package network.rs485.logisticspipes.proxy

import network.rs485.logisticspipes.inventory.ProviderMode
import network.rs485.logisticspipes.util.equalsWithNBT
import logisticspipes.LPConstants
import logisticspipes.proxy.specialinventoryhandler.SpecialInventoryHandler
import logisticspipes.utils.item.ItemIdentifier
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken
import net.minecraftforge.fml.ModList
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import kotlin.math.min

class StorageDrawersProxyImpl : SpecialInventoryHandler.Factory {

    companion object {
        // same instance Storage Drawers registers as PlatformCapabilities.NATIVE_DRAWER_GROUP
        private val drawerGroupCapability: Capability<IDrawerGroup> = CapabilityManager.get(object : CapabilityToken<IDrawerGroup>() {})

        private fun drawerGroup(tile: BlockEntity): IDrawerGroup? =
            tile.getCapability(drawerGroupCapability, null).resolve().orElse(null)
    }

    override fun isType(tile: BlockEntity, dir: Direction?): Boolean = drawerGroup(tile) != null

    override fun getUtilForTile(
        tile: BlockEntity,
        direction: Direction?,
        mode: ProviderMode
    ): SpecialInventoryHandler? = drawerGroup(tile)?.let { StorageDrawersInventoryHandler(it, mode) }

    override fun init(): Boolean = ModList.get().isLoaded(LPConstants.storagedrawersModID)

}

// vending drawers are handled by Storage Drawers inside adjustStoredItemCount
class StorageDrawersInventoryHandler(
    private val drawerGroup: IDrawerGroup,
    private val mode: ProviderMode,
) : SpecialInventoryHandler() {

    private fun checkSlot(slot: Int): Boolean = slot in mode.cropStart until (drawerGroup.drawerCount - mode.cropEnd)

    private fun <T> fullDrawerApply(slot: Int, apply: (IDrawer) -> T) =
        drawerGroup.getDrawer(slot).takeIf { it.isEnabled && !it.isEmpty }?.let(apply)

    private fun accessibleDrawerSlots() = drawerGroup.accessibleDrawerSlots.filter(::checkSlot)

    private fun slotMachine() = accessibleDrawerSlots()
        .flatMap { slot ->
            fullDrawerApply(slot) {
                listOf(Triple(slot, it.storedItemPrototype, it.storedItemCount))
            } ?: emptyList()
        }

    private fun Int.hideSinglePerTypeOrStack(): Int =
        (if (mode.hideOnePerStack || mode.hideOnePerType) minus(1) else this).coerceAtLeast(0)

    private fun Int.hideSinglePerStack(): Int = if (mode.hideOnePerStack) minus(1) else this

    private fun enabledDrawerSequence(): Sequence<IDrawer> =
        accessibleDrawerSlots().asSequence()
            .map { slot -> drawerGroup.getDrawer(slot) }
            .filter { drawer -> drawer.isEnabled }

    override fun getItems(): MutableSet<ItemIdentifier> = accessibleDrawerSlots().flatMapTo(HashSet()) { slot ->
        fullDrawerApply(slot) { listOf(ItemIdentifier.get(it.storedItemPrototype)) } ?: emptyList()
    }

    override fun getItem(slot: Int): ItemStack =
        if (checkSlot(slot) && slot in drawerGroup.accessibleDrawerSlots) {
            fullDrawerApply(slot) {
                val prototype = it.storedItemPrototype
                prototype.copyWithCount(min(it.storedItemCount.hideSinglePerTypeOrStack(), prototype.maxStackSize))
            } ?: ItemStack.EMPTY
        } else ItemStack.EMPTY

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        if (amount <= 0 || !checkSlot(slot) || slot !in drawerGroup.accessibleDrawerSlots) return ItemStack.EMPTY
        return fullDrawerApply(slot) { drawer ->
            val prototype = drawer.storedItemPrototype.copy()
            val toRemove = min(amount, drawer.storedItemCount.hideSinglePerTypeOrStack())
            if (toRemove <= 0) ItemStack.EMPTY
            else prototype.copyWithCount(toRemove - drawer.adjustStoredItemCount(-toRemove))
        } ?: ItemStack.EMPTY
    }

    override fun getItemsAndCount(): MutableMap<ItemIdentifier, Int> = HashMap<ItemIdentifier, Int>().also { map ->
        slotMachine().forEach { (_, stack, count) ->
            map.compute(ItemIdentifier.get(stack)) { _, existing ->
                count.hideSinglePerStack() + (existing ?: if (mode.hideOnePerType && !mode.hideOnePerStack) -1 else 0)
            }
        }
        map.forEach { (k, v) -> map[k] = v.coerceAtLeast(0) }
    }

    override fun getContainerSize(): Int = drawerGroup.drawerCount

    override fun getMultipleItems(itemid: ItemIdentifier, count: Int): ItemStack {
        var left = count
        enabledDrawerSequence()
            .takeWhile { left > 0 }
            .filter { drawer -> !drawer.isEmpty && itemid.equalsWithNBT(drawer.storedItemPrototype) }
            .forEach { drawer -> left = drawer.adjustStoredItemCount(-left) }
        return itemid.makeNormalStack(count - left)
    }

    override fun getSingleItem(item: ItemIdentifier): ItemStack = getMultipleItems(item, 1)

    override fun containsUndamagedItem(itemid: ItemIdentifier): Boolean =
        accessibleDrawerSlots().any { slot ->
            fullDrawerApply(slot) { drawer -> itemid.equalsWithNBT(drawer.storedItemPrototype) } ?: false
        }

    // like Storage Drawers itself: fill drawers already holding the item before empty ones
    override fun add(stack: ItemStack, orientation: Direction?, doAdd: Boolean): ItemStack {
        var left = stack.count
        enabledDrawerSequence()
            .filter { drawer -> drawer.canItemBeStored(stack) }
            .sortedBy { drawer -> drawer.isEmpty }
            .takeWhile { left > 0 }
            .forEach { drawer ->
                left = if (doAdd) {
                    (if (drawer.isEmpty) drawer.setStoredItem(stack) else drawer).adjustStoredItemCount(left)
                } else {
                    (left - drawer.capacityFor(stack)).coerceAtLeast(0)
                }
            }
        return stack.copy().also { it.shrink(left) }
    }

    // an empty drawer reports no capacity until it is assigned an item
    private fun IDrawer.capacityFor(stack: ItemStack): Int =
        if (isEmpty) getAcceptingMaxCapacity(stack) else acceptingRemainingCapacity

    override fun roomForItem(stack: ItemStack): Int = accessibleDrawerSlots().sumOf { slot ->
        drawerGroup.getDrawer(slot)
            .let { if (it.isEnabled && it.canItemBeStored(stack)) it.capacityFor(stack) else 0 }
    }

}
