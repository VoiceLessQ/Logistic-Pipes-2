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

import network.rs485.logisticspipes.integration.MinecraftTest.TIMEOUT_MODIFIER
import network.rs485.logisticspipes.integration.MinecraftTest.regularTest
import network.rs485.logisticspipes.integration.MinecraftTest.skippedTest
import network.rs485.logisticspipes.util.FuzzyFlag
import network.rs485.logisticspipes.util.FuzzyUtil
import network.rs485.minecraft.BlockPlacer
import network.rs485.minecraft.BlockPosSelector
import network.rs485.minecraft.configurator
import logisticspipes.LPBlocks
import logisticspipes.LPItems
import logisticspipes.blocks.LogisticsSolidBlock
import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity
import logisticspipes.pipes.PipeItemsBasicLogistics
import logisticspipes.pipes.PipeItemsCraftingLogistics
import logisticspipes.pipes.PipeItemsRequestLogistics
import logisticspipes.pipes.upgrades.FuzzyUpgrade
import logisticspipes.pipes.upgrades.UpgradeManager
import logisticspipes.utils.item.ItemIdentifier
import net.minecraft.world.level.block.Block
// BlockPlanks removed — planks are a vanilla Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.core.Direction
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

@Suppress("FunctionName")
object CraftingTest {
    private val fuzzyUpgradeItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(LPItems.upgrades[FuzzyUpgrade.getName()])!!

    suspend fun `test single fuzzy ingredient crafting fails multi-request with mixed OreDict input`(
        loggerIn: (Any) -> Unit,
        selector: BlockPosSelector,
    ) = skippedTest(loggerIn, selector) {
        val setup = `setup fuzzy crafting chest`(
            selector = selector,
            providerStacks = arrayOf(
                ItemStack(Blocks.OAK_PLANKS, 4),
                ItemStack(Blocks.DARK_OAK_PLANKS, 4),
            ),
        )
        selector.finalize()
        delay(5000) // FIXME: delay and pipe update should not be needed
        setup.requesterPipe.router.update(true, setup.requesterPipe)
        val stacks = listOf(ItemStack(Blocks.CHEST))
        setup.requesterPipe.requestItems(stacks, mustSucceed = false)
        val waitForChestResult = waitForOrNull(
            timeout = Duration.ofSeconds(3 * TIMEOUT_MODIFIER),
            check = { setup.requesterChest.containsOnly(stacks) },
        )
        assertTrue(message = "a chest item was found in the requester chest") {
            waitForChestResult === null
        }
    }

    suspend fun `test single fuzzy ingredient crafting fails with mixed OreDict input`(
        loggerIn: (Any) -> Unit,
        selector: BlockPosSelector,
    ) = skippedTest(loggerIn, selector) {
        val setup = `setup fuzzy crafting chest`(
            selector = selector,
            providerStacks = arrayOf(
                ItemStack(Blocks.OAK_PLANKS, 4),
                ItemStack(Blocks.DARK_OAK_PLANKS, 4),
            ),
        )
        selector.finalize()
        delay(5000)
        setup.requesterPipe.router.update(true, setup.requesterPipe)
        setup.requesterPipe.requestItem(ItemStack(Blocks.CHEST), mustSucceed = false)
        val waitForChestResult = waitForOrNull(
            timeout = Duration.ofSeconds(3 * TIMEOUT_MODIFIER),
            check = { setup.requesterChest.containsOnly(listOf(ItemStack(Blocks.CHEST))) },
        )
        assertTrue(message = "a chest item was found in the requester chest") {
            waitForChestResult === null
        }
    }

    suspend fun `test single fuzzy ingredient crafting succeeds with sufficient input of one OreDict type`(
        loggerIn: (Any) -> Unit,
        selector: BlockPosSelector,
    ) = skippedTest(loggerIn, selector) {
        val trapItems = ItemStack(Blocks.OAK_PLANKS, 4)
        val setup = `setup fuzzy crafting chest`(
            selector = selector,
            providerStacks = arrayOf(
                trapItems,
                ItemStack(Blocks.DARK_OAK_PLANKS, 8),
            ),
        )
        selector.finalize()
        delay(5000)
        setup.requesterPipe.router.update(true, setup.requesterPipe)
        setup.requesterPipe.requestItem(ItemStack(Blocks.CHEST), mustSucceed = true)
        val waitForChestResult = waitForOrNull(
            timeout = Duration.ofSeconds(3 * TIMEOUT_MODIFIER),
            check = { setup.requesterChest.containsOnly(listOf(ItemStack(Blocks.CHEST))) },
        )
        assertTrue(message = "a chest item was not found before a timeout") {
            waitForChestResult !== null
        }
        setup.providerChest.containsOnly(listOf(trapItems))
    }

    suspend fun `test single fuzzy ingredient crafting succeeds multi-request with sufficient input of one OreDict type`(
        loggerIn: (Any) -> Unit,
        selector: BlockPosSelector,
    ) = skippedTest(loggerIn, selector) {
        val trapItems = ItemStack(Blocks.OAK_PLANKS, 4)
        val setup = `setup fuzzy crafting chest`(
            selector = selector,
            providerStacks = arrayOf(
                trapItems,
                ItemStack(Blocks.DARK_OAK_PLANKS, 8),
            ),
        )
        selector.finalize()
        delay(5000)
        setup.requesterPipe.router.update(true, setup.requesterPipe)
        val stacks = listOf(ItemStack(Blocks.CHEST))
        setup.requesterPipe.requestItems(stacks, mustSucceed = true)
        val waitForChestResult = waitForOrNull(
            timeout = Duration.ofSeconds(3 * TIMEOUT_MODIFIER),
            check = { setup.requesterChest.containsOnly(stacks) },
        )
        assertTrue(message = "a chest item was not found before a timeout") {
            waitForChestResult !== null
        }
        setup.providerChest.containsOnly(listOf(trapItems))
    }

    suspend fun `test single fuzzy ingredient crafting fails with mixed OreDict input on two provider pipes on one double chest`(
        loggerIn: (Any) -> Unit,
        selector: BlockPosSelector,
    ) = skippedTest(loggerIn, selector) {
        val setup = `setup fuzzy crafting chest`(
            selector = selector,
            providerStacks = arrayOf(
                ItemStack(Blocks.OAK_PLANKS, 4),
            ),
        )
        selector.direction(Direction.NORTH)
            .apply {
                place(PipePlacer(PipeItemsBasicLogistics(LPItems.pipeBasic.get())))
                setupProvidingChest(Direction.WEST,
                    ItemStack(Blocks.DARK_OAK_PLANKS, 4))
            }
            .finalize()
        delay(5000)
        setup.requesterPipe.router.update(true, setup.requesterPipe)
        setup.requesterPipe.requestItem(ItemStack(Blocks.CHEST), mustSucceed = false)
        val waitForChestResult = waitForOrNull(
            timeout = Duration.ofSeconds(3 * TIMEOUT_MODIFIER),
            check = { setup.requesterChest.containsOnly(listOf(ItemStack(Blocks.CHEST))) },
        )
        assertTrue(message = "a chest item was found in the requester chest") {
            waitForChestResult === null
        }
    }

    suspend fun `test single fuzzy ingredient crafting fails with mixed OreDict input on different providers`(
        loggerIn: (Any) -> Unit,
        selector: BlockPosSelector,
    ) = skippedTest(loggerIn, selector) {
        val setup = `setup fuzzy crafting chest`(
            selector = selector,
            providerStacks = arrayOf(
                ItemStack(Blocks.OAK_PLANKS, 4),
            ),
        )
        selector
            .direction(Direction.NORTH)
            .place(UnroutedPipePlacer)
            .direction(Direction.NORTH)
            .place(PipePlacer(PipeItemsBasicLogistics(LPItems.pipeBasic.get())))
            .apply {
                setupProvidingChest(Direction.WEST,
                    ItemStack(Blocks.DARK_OAK_PLANKS, 4))
            }
            .finalize()
        delay(5000)
        setup.requesterPipe.router.update(true, setup.requesterPipe)
        setup.requesterPipe.requestItem(ItemStack(Blocks.CHEST), mustSucceed = false)
        val waitForChestResult = waitForOrNull(
            timeout = Duration.ofSeconds(3 * TIMEOUT_MODIFIER),
            check = { setup.requesterChest.containsOnly(listOf(ItemStack(Blocks.CHEST))) },
        )
        assertTrue(message = "a chest item was found in the requester chest") {
            waitForChestResult === null
        }
    }

    suspend fun `test split fuzzy ingredients crafting succeeds with mixed OreDict input`(
        loggerIn: (Any) -> Unit,
        selector: BlockPosSelector,
    ) = regularTest(loggerIn, selector) {
        val setup = `setup fuzzy crafting chest`(
            selector = selector,
            providerStacks = arrayOf(
                ItemStack(Blocks.OAK_PLANKS, 4),
                ItemStack(Blocks.DARK_OAK_PLANKS, 4),
            ),
        )
        selector.finalize()
        (0 until 8).forEach {
            setup.craftingPipe.dummyInventory.setItem(it,
                ItemStack(Blocks.OAK_PLANKS, 1))
            FuzzyUtil.set(setup.craftingPipe.logisticsModule.inputFuzzy(it), FuzzyFlag.USE_ORE_DICT, true)
        }
        delay(5000)
        setup.requesterPipe.router.update(true, setup.requesterPipe)
        setup.requesterPipe.requestItem(ItemStack(Blocks.CHEST), mustSucceed = true)
        val waitForChestResult = waitForOrNull(
            timeout = Duration.ofSeconds(3 * TIMEOUT_MODIFIER),
            check = { setup.requesterChest.containsOnly(listOf(ItemStack(Blocks.CHEST))) },
        )
        assertTrue(message = "a chest item was not found before a timeout") {
            waitForChestResult !== null
        }
    }

    suspend fun `test split fuzzy ingredients crafting succeeds multi-request with mixed OreDict input`(
        loggerIn: (Any) -> Unit,
        selector: BlockPosSelector,
    ) = regularTest(loggerIn, selector) {
        val setup = `setup fuzzy crafting chest`(
            selector = selector,
            providerStacks = arrayOf(
                ItemStack(Blocks.OAK_PLANKS, 4),
                ItemStack(Blocks.DARK_OAK_PLANKS, 4),
            ),
        )
        selector.finalize()
        (0 until 8).forEach {
            setup.craftingPipe.dummyInventory.setItem(it,
                ItemStack(Blocks.OAK_PLANKS, 1))
            FuzzyUtil.set(setup.craftingPipe.logisticsModule.inputFuzzy(it), FuzzyFlag.USE_ORE_DICT, true)
        }
        delay(5000)
        setup.requesterPipe.router.update(true, setup.requesterPipe)
        val stacks = listOf(ItemStack(Blocks.CHEST))
        setup.requesterPipe.requestItems(stacks, mustSucceed = true)
        val waitForChestResult = waitForOrNull(
            timeout = Duration.ofSeconds(3 * TIMEOUT_MODIFIER),
            check = { setup.requesterChest.containsOnly(stacks) },
        )
        assertTrue(message = "a chest item was not found before a timeout") {
            waitForChestResult !== null
        }
        assertTrue(message = "the provider chest must be empty after providing all planks") {
            setup.providerChest.isEmpty
        }
    }

    suspend fun `test split fuzzy ingredients crafting succeeds with leftover mixed OreDict input`(
        loggerIn: (Any) -> Unit,
        selector: BlockPosSelector,
    ) = regularTest(loggerIn, selector) {
        val oakPlanksStack = ItemStack(Blocks.OAK_PLANKS, 4)
        val oakPlankItemIdent = ItemIdentifier.get(oakPlanksStack)
        val darkOakPlanksStack = ItemStack(Blocks.DARK_OAK_PLANKS, 8)
        val darkOakPlankItemIdent = ItemIdentifier.get(darkOakPlanksStack)
        val setup = `setup fuzzy crafting chest`(
            selector = selector,
            providerStacks = arrayOf(oakPlanksStack, darkOakPlanksStack),
        )
        selector.finalize()
        (0 until 8).forEach {
            setup.craftingPipe.dummyInventory.setItem(it,
                ItemStack(Blocks.OAK_PLANKS, 1))
            FuzzyUtil.set(setup.craftingPipe.logisticsModule.inputFuzzy(it), FuzzyFlag.USE_ORE_DICT, true)
        }
        delay(5000)
        setup.requesterPipe.router.update(true, setup.requesterPipe)
        setup.requesterPipe.requestItem(ItemStack(Blocks.CHEST), mustSucceed = true)
        val waitForChestResult = waitForOrNull(
            timeout = Duration.ofSeconds(3 * TIMEOUT_MODIFIER),
            check = { setup.requesterChest.containsOnly(listOf(ItemStack(Blocks.CHEST))) },
        )
        assertTrue(message = "a chest item was not found before a timeout") {
            waitForChestResult !== null
        }
        val providerPlanksLeft = setup.providerChest.amountOf(listOf(oakPlankItemIdent, darkOakPlankItemIdent))
        assertEquals(4, providerPlanksLeft, message = "the provider chest must still contain 4 planks")
    }

    suspend fun `test split fuzzy ingredients crafting succeeds multi-request with leftover mixed OreDict input`(
        loggerIn: (Any) -> Unit,
        selector: BlockPosSelector,
    ) = regularTest(loggerIn, selector) {
        val oakPlanksStack = ItemStack(Blocks.OAK_PLANKS, 4)
        val oakPlankItemIdent = ItemIdentifier.get(oakPlanksStack)
        val darkOakPlanksStack = ItemStack(Blocks.DARK_OAK_PLANKS, 8)
        val darkOakPlankItemIdent = ItemIdentifier.get(darkOakPlanksStack)
        val setup = `setup fuzzy crafting chest`(
            selector = selector,
            providerStacks = arrayOf(oakPlanksStack, darkOakPlanksStack),
        )
        selector.finalize()
        (0 until 8).forEach {
            setup.craftingPipe.dummyInventory.setItem(it,
                ItemStack(Blocks.OAK_PLANKS, 1))
            FuzzyUtil.set(setup.craftingPipe.logisticsModule.inputFuzzy(it), FuzzyFlag.USE_ORE_DICT, true)
        }
        delay(5000)
        setup.requesterPipe.router.update(true, setup.requesterPipe)
        val stacks = listOf(ItemStack(Blocks.CHEST))
        setup.requesterPipe.requestItems(stacks, mustSucceed = true)
        val waitForChestResult = waitForOrNull(
            timeout = Duration.ofSeconds(3 * TIMEOUT_MODIFIER),
            check = { setup.requesterChest.containsOnly(stacks) },
        )
        assertTrue(message = "a chest item was not found before a timeout") {
            waitForChestResult !== null
        }
        val providerPlanksLeft = setup.providerChest.amountOf(listOf(oakPlankItemIdent, darkOakPlankItemIdent))
        assertEquals(4, providerPlanksLeft, message = "the provider chest must still contain 4 planks")
    }

    private suspend fun `setup fuzzy crafting chest`(
        selector: BlockPosSelector,
        providerStacks: Array<ItemStack>,
        extraCraftingTableConfigurator: LogisticsCraftingTableTileEntity.() -> Unit = {},
    ): FuzzyCraftingSetup {
        val fuzzyCraftingTableHasRecipe = CompletableDeferred<Unit>()
        val fuzzyCraftingTablePlacer = BlockPlacer(block = LPBlocks.crafterFuzzy.get()) { placer ->
            placer.getTileEntity<LogisticsCraftingTableTileEntity>().apply {
                (0 until 9).filter { it != 4 }.forEach {
                    matrix.setItem(it, ItemStack(Blocks.OAK_PLANKS))
                    FuzzyUtil.set(inputFuzzy(it), FuzzyFlag.USE_ORE_DICT, true)
                }
                extraCraftingTableConfigurator()
                cacheRecipe()
            }
            fuzzyCraftingTableHasRecipe.complete(Unit)
        }

        val craftingPipeInitialized = CompletableDeferred<PipeItemsCraftingLogistics>()
        val craftingPipePlacer = PipePlacer(PipeItemsCraftingLogistics(LPItems.pipeCrafting.get())) {
            (it.pipe.upgradeManager as UpgradeManager).inv.apply {
                setItem(0, ItemStack(fuzzyUpgradeItem))
                setChanged()
            }
            it.waitForPipeInitialization()
            assertTrue(message = "Expected crafting pipe to have fuzzy upgrade") {
                it.pipe.logisticsModule.hasFuzzyUpgrade()
            }
            craftingPipeInitialized.complete(it.pipe)
            it.updateConnectionsAndWait()
        }

        return selector.place(fuzzyCraftingTablePlacer)
            .direction(Direction.NORTH)
            .place(craftingPipePlacer)
            .configure(configurator(name = "crafting recipe importer") {
                fuzzyCraftingTableHasRecipe.await()
                val craftingPipe = craftingPipeInitialized.await()
                craftingPipe.logisticsModule.importFromCraftingTable(null)
            })
            .run {
                setupLogisticsPower(Direction.EAST, 100000F)
                val (_, providerChestPlacer) = setupProvidingChest(Direction.WEST, *providerStacks)
                val (requesterPipePlacer, requesterChestPlacer) = setupRequestingChest(Direction.UP)
                object : FuzzyCraftingSetup {
                    override val requesterPipePlacer = requesterPipePlacer
                    override val requesterChestPlacer = requesterChestPlacer
                    override val providerChestPlacer = providerChestPlacer
                    override val craftingPipePlacer = craftingPipePlacer
                    override val fuzzyCraftingTablePlacer = fuzzyCraftingTablePlacer
                }
            }
    }

    interface FuzzyCraftingSetup {
        val requesterPipePlacer: PipePlacer<PipeItemsRequestLogistics>
        val requesterPipe: PipeItemsRequestLogistics
            get() = requesterPipePlacer.pipe
        val requesterChestPlacer: BlockPlacer<Block>
        val requesterChest: ChestBlockEntity
            get() = requesterChestPlacer.getTileEntity<ChestBlockEntity>()
        val providerChestPlacer: BlockPlacer<Block>
        val providerChest: ChestBlockEntity
            get() = providerChestPlacer.getTileEntity<ChestBlockEntity>()
        val craftingPipePlacer: PipePlacer<PipeItemsCraftingLogistics>
        val craftingPipe: PipeItemsCraftingLogistics
            get() = craftingPipePlacer.pipe
        val fuzzyCraftingTablePlacer: BlockPlacer<LogisticsSolidBlock>
        @Suppress("unused")
        val fuzzyCraftingTable: LogisticsCraftingTableTileEntity
            get() = fuzzyCraftingTablePlacer.getTileEntity<LogisticsCraftingTableTileEntity>()
    }

}
