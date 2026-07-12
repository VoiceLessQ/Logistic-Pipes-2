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

package network.rs485.logisticspipes.guidebook

import logisticspipes.items.LogisticsItem
import logisticspipes.network.PacketHandler
import logisticspipes.network.guis.OpenGuideBook
import logisticspipes.proxy.MainProxy
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import network.rs485.logisticspipes.gui.guidebook.GuiGuideBook
import network.rs485.logisticspipes.gui.guidebook.IPageData
import network.rs485.logisticspipes.gui.guidebook.PageData
import network.rs485.logisticspipes.gui.guidebook.Page
import network.rs485.logisticspipes.network.packets.SetCurrentPagePacket

class ItemGuideBook : LogisticsItem() {

    companion object {
        /**
         * Loads the current page and the tabs from the stack's NBT. Returns Pair(currentPage, Tabs)
         */
        private fun loadDataFromNBT(stack: ItemStack): Pair<PageData, List<PageData>> {
            var currentPage: PageData? = null
            var tabPages: List<PageData>? = null
            // 1.20.5+: stack NBT lives in the CUSTOM_DATA component.
            val nbt = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()
            if (nbt != null) {
                if (nbt.contains("version")) {
                    when (nbt.getByte("version")) {
                        1.toByte() -> {
                            currentPage = PageData(nbt.getCompound("page"))
                            // type 10 = CompoundTag, see net.minecraft.nbt.Tag
                            val tagList = nbt.getList("bookmarks", 10)
                            tabPages = tagList.mapNotNull { tag -> PageData(tag as CompoundTag) }
                        }
                    }
                }
            }
            currentPage = currentPage ?: PageData(BookContents.MAIN_MENU_FILE)
            tabPages = tabPages ?: emptyList()
            return currentPage to tabPages
        }

        @JvmStatic
        fun openGuideBook(hand: InteractionHand, stack: ItemStack) {
            val mc = Minecraft.getInstance()
            val equipmentSlot = if (hand == InteractionHand.MAIN_HAND) EquipmentSlot.MAINHAND else EquipmentSlot.OFFHAND
            // schedule on main thread
            mc.execute {
                val state = loadDataFromNBT(stack).let {
                    GuideBookState(equipmentSlot, Page(it.first), it.second.map(::Page))
                }
                mc.setScreen(GuiGuideBook(state))
            }
        }
    }

    class GuideBookState(val equipmentSlot: EquipmentSlot, var currentPage: Page, bookmarks: List<Page>) {
        val bookmarks = bookmarks.toMutableList()
    }

    fun updateNBT(tag: CompoundTag, page: IPageData, tabs: List<IPageData>) = tag.apply {
        putByte("version", 1)
        put("page", page.toTag())
        put("bookmarks", ListTag().apply {
            tabs.map(IPageData::toTag).forEach { add(it) }
        })
    }

    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (stack.item is ItemGuideBook && MainProxy.isServer(world)) {
            MainProxy.sendPacketToPlayer(
                PacketHandler.getPacket(OpenGuideBook::class.java).setHand(hand).setStack(stack),
                player,
            )
            return InteractionResultHolder.success(stack)
        }
        return InteractionResultHolder.pass(stack)
    }

    fun saveState(state: GuideBookState) {
        val stack = Minecraft.getInstance().player!!.getItemBySlot(state.equipmentSlot)
        val compound = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()
        // update NBT for the client
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(updateNBT(compound, state.currentPage, state.bookmarks)))

        // … and for the server
        MainProxy.sendPacketToServer(
            PacketHandler.getPacket(SetCurrentPagePacket::class.java)
                .setEquipmentSlot(state.equipmentSlot)
                .setCurrentPage(state.currentPage)
                .setBookmarks(state.bookmarks)
        )
    }

}
