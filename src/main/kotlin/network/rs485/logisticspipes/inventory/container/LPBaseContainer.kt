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

package network.rs485.logisticspipes.inventory.container

import network.rs485.logisticspipes.gui.widget.*
import network.rs485.logisticspipes.property.IBitSet
import network.rs485.logisticspipes.property.InventoryProperty
import network.rs485.logisticspipes.util.FuzzyFlag
import logisticspipes.LogisticsPipes
import logisticspipes.modules.LogisticsModule
import logisticspipes.utils.gui.ModuleSlot
import net.minecraft.world.Container
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.math.min

abstract class LPBaseContainer<out M : LogisticsModule>(val module: M) : AbstractContainerMenu(null, 0) {

    val slotSize = 18

    private val playerHotbarSlots = mutableListOf<Slot>()
    private val playerBackpackSlots = mutableListOf<Slot>()
    private val directSlotPropertyMap = mutableMapOf<Slot, InventoryProperty<*>>()

    /**
     * Add container-specific's DummySlots to the Container's slot list.
     * This should be implemented in a per-container basis.
     * @param overlayInventory  dummy inventory from which the slots will be grabbed.
     * @param startX            starting leftmost position.
     * @param startY            starting bottommost position.
     * @return the list of added lots.
     */
    abstract fun addDummySlotsToContainer(
        overlayInventory: Container,
        baseProperty: InventoryProperty<*>? = null,
        startX: Int,
        startY: Int
    ): List<GhostSlot>

    /**
     * Add DummySlot to the Container's slot list.
     * @param dummyInventoryIn slot's inventory.
     * @param slotId id to be given to the slot
     */
    fun addGhostItemSlotToContainer(
        dummyInventoryIn: Container,
        baseProperty: InventoryProperty<*>?,
        slotId: Int,
        posX: Int,
        posY: Int
    ): GhostItemSlot {
        val slot = GhostItemSlot(
            inventoryIn = dummyInventoryIn,
            index = slotId,
            xPosition = posX,
            yPosition = posY,
        )
        addSlot(slot)
        baseProperty?.let { directSlotPropertyMap[slot] = it }
        return slot
    }


    fun addFuzzyItemSlotToContainer(
        dummyInventoryIn: Container,
        baseProperty: InventoryProperty<*>?,
        slotId: Int,
        posX: Int,
        posY: Int,
        usedFlags: EnumSet<FuzzyFlag>,
        flagGetter: () -> IBitSet,
    ): FuzzyItemSlot {
        val slot = FuzzyItemSlot(
            inventoryIn = dummyInventoryIn,
            index = slotId,
            xPosition = posX,
            yPosition = posY,
            usedFlags = usedFlags,
            flagGetter = flagGetter,
        )
        addSlot(slot)
        baseProperty?.let { directSlotPropertyMap[slot] = it }
        return slot
    }


    override fun clicked(slotId: Int, dragType: Int, clickTypeIn: ClickType, player: Player) {

        // slotId -999 is a special case for when the ItemStack is being drag-split between slots.
        if (slotId < 0) {
            super.clicked(slotId, dragType, clickTypeIn, player)
            return
        }

        val slot = slots.getOrNull(slotId) ?: return

        if (slot is LockedSlot) return

        if (slot !is GhostSlot) {
            // In case slot is not a subtype of GhostSlot vanilla behaviour will be applied.
            super.clicked(slotId, dragType, clickTypeIn, player)
            return
        }

        // The slot will always be a subtype of GhostSlot from this point onwards.
        val grabbedItemStack = getCarried()
        handleGhostSlotClick(slot, grabbedItemStack, dragType, clickTypeIn, player)

    }

    /**
     * Transfers items back and forth from the player's backpack and hotbar.
     * Override and return this to add extra functionality.
     * @param player player accessing the container
     * @param index of the slot in the inventorySlots list.
     * @return empty ItemStack to stop the weird loop that vanilla runs using this.
     */
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem() || slot is GhostSlot || slot is ModuleSlot) return ItemStack.EMPTY
        when {
            playerHotbarSlots.contains(slot) -> handleShiftClickFromSlotToList(slot, playerBackpackSlots, player)
            playerBackpackSlots.contains(slot) -> handleShiftClickFromSlotToList(slot, playerHotbarSlots, player)
            else -> LogisticsPipes.log.warn("Something is wrong, this slot is not apart of the player's inventory and wasn't dealt with properly before.")
        }
        return ItemStack.EMPTY
    }

    /**
     * Runs the code to check and add any item that would normally be added by running [transferStackInSlot].
     * This is useful to bring client-side shift-clicking functionality to property-based GUIs.
     * @return true, if a ghost slot did take the item and false if the transfer should be handled elsewhere.
     */
    open fun tryTransferSlotToGhostSlot(slotIdx: Int): Boolean = false

    /**
     * Attempts to transfer the stack to one or more slots in the list.
     * @param from slot sending stack
     * @param toList slots possibly receiving stack
     * @param player player interacting with the container
     * @return true when the shifted stack was fully utilized, false otherwise
     */
    open fun handleShiftClickFromSlotToList(from: Slot, toList: List<Slot>, player: Player): Boolean {
        if (!from.hasItem()) return true
        val slots = toList.partition { it.hasItem() }
        // Iterate through all non-empty slots and then through all empty slots until the initial slot is depleted or it fails.
        slots.first.takeIf { it.isNotEmpty() }
            ?.forEach { to -> if (handleShiftClickFromSlotToSlot(from, to, player)) return true }
        slots.second.takeIf { it.isNotEmpty() }
            ?.forEach { to -> if (handleShiftClickFromSlotToSlot(from, to, player)) return true }
        return false
    }

    /**
     * Attempts to shift a stack from one stack from the other.
     * @param from slot sending stack
     * @param to slot receiving stack
     * @param player player interacting with the container
     * @return true when from is empty false when no action taken
     */
    open fun handleShiftClickFromSlotToSlot(from: Slot, to: Slot, player: Player): Boolean {
        if (!from.hasItem()) return true
        if (to is GhostSlot || to is ModuleSlot) return false
        if (to.hasItem() && ItemStack.isSameItem(from.item, to.item)) {
            // Calculate how many items can be added to stack until it is full, can be limited by the ItemStack(Item) or the Slot.
            val freeAmount = min(to.maxStackSize, to.item.maxStackSize) - to.item.count
            if (freeAmount > 0) {
                // Reduce original from stack and do the same on the slot to sync.
                var shiftedStack = from.remove(freeAmount)
                from.onTake(player, shiftedStack)
                if (!shiftedStack.isEmpty && !to.item.isEmpty) {
                    // Increase count on the receiving stack and also slot to sync.
                    to.item.grow(shiftedStack.count)
                    to.set(to.item)
                    return !from.hasItem()
                }
            }
        } else if (!to.hasItem()) {
            // Calculate how much can be added to empty slot
            val maxAmount = min(from.item.count, to.maxStackSize)
            if (maxAmount > 0) {
                val shiftedStack = from.remove(maxAmount)
                to.set(shiftedStack)
                if (from.item.isEmpty) from.set(ItemStack.EMPTY)
            }
            return !from.hasItem()
        }
        return false
    }

    /**
     * Add player 9 + 27 slots to the Container's slot list. This method currently places
     * the slots in a grid so they work on old guis.
     * @param playerInventoryIn player's inventory from which the slots will be grabbed.
     * @param startX starting leftmost position.
     * @param startY starting topmost position.
     * @return  return all the slots in the player's inventory.
     */
    open fun addPlayerSlotsToContainer(
        playerInventoryIn: Container,
        startX: Int,
        startY: Int,
        lockedStack: ItemStack,
    ): List<Slot> {

        // Minecraft expects the 27 backpack slots to be index in 0-26 on the container list, and the hotbar
        // corresponds to 27-35.

        // Add the top 27 inventory slots
        for (row in 0..2) {
            for (column in 0..8) {
                playerBackpackSlots.add(
                    addSlot(
                        Slot(
                            playerInventoryIn,
                            column + row * 9 + 9,
                            startX + column * slotSize,
                            startY + row * slotSize,
                        ),
                    ),
                )
            }
        }

        // Add the hotbar inventory slots
        for (index in 0..8) {
            if (!lockedStack.isEmpty && playerInventoryIn.getItem(index) == lockedStack) {
                playerHotbarSlots.add(
                    addSlot(
                        LockedSlot(
                            playerInventoryIn,
                            index,
                            startX + index * slotSize,
                            startY + 3 * slotSize + 4,
                        ),
                    ),
                )
            } else {
                playerHotbarSlots.add(
                    addSlot(
                        Slot(
                            playerInventoryIn,
                            index,
                            startX + index * slotSize,
                            startY + 3 * slotSize + 4,
                        ),
                    ),
                )
            }
        }

        return playerBackpackSlots + playerHotbarSlots
    }

    // Handle click on GhostSlot
    private fun handleGhostSlotClick(
        slot: GhostSlot,
        grabbedItemStack: ItemStack,
        dragType: Int,
        clickTypeIn: ClickType,
        player: Player,
    ) = when (slot) {
        is Item -> handleGhostItemSlotClick(slot, grabbedItemStack, dragType, clickTypeIn, player)
        else -> Unit // fluid ghost slots not supported; ignore click
    }

    /**
     * Decide what to do based on the input received
     * @param slot Clicked slot
     * @param grabbedItemStack ItemStack grabbed by the mouse, EMPTY if none.
     * @param dragType stage of multi slot dragging
     * @param clickTypeIn type of action being performed @see ClickType
     * @param player interacting with the container
     */
    open fun handleGhostItemSlotClick(
        slot: GhostSlot,
        grabbedItemStack: ItemStack,
        dragType: Int,
        clickTypeIn: ClickType,
        player: Player,
    ) {
        LogisticsPipes.log.info("DragType $dragType, ClickType: $clickTypeIn")
        // Copy the grabbedStack and insert it into the GhostItemSlot
        if (slot !is Unmodifiable) {
            applyItemStackToGhostItemSlot(grabbedItemStack, slot)
        }
    }

    /**
     * Copies given ItemStack and gives it to the given GhostItemSlot
     * @param itemStack ItemStack to be copied
     * @param slot target GhostItemSlot
     */
    open fun applyItemStackToGhostItemSlot(itemStack: ItemStack, slot: GhostSlot) {
        val copiedItemStack = itemStack.copy()
        slot.set(copiedItemStack)
    }

    fun putStackInSlot(slotID: Int, stack: ItemStack) {
        // external change, access underlying property directly
        val slot = slots.getOrNull(slotID)
        slot?.let { directSlotPropertyMap[slot] }?.also { property ->
            property.setItem(slot.slotIndex, stack)
        } ?: slot?.set(stack)
    }

    @OnlyIn(Dist.CLIENT)
    fun setAll(slotStacks: MutableList<ItemStack>) {
        for (slotIdx in slotStacks.indices) {
            putStackInSlot(slotIdx, slotStacks[slotIdx])
        }
    }
}
