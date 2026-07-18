package app.homenotes.android

import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun item(
        id: String,
        slot: EquipSlot? = EquipSlot.WEAPON,
        equippedSlot: EquipSlot? = null,
        bonuses: List<ItemBonus> = emptyList()
    ) = InventoryItem(
        id = id, name = id, description = "", icon = "X",
        slot = slot, bonuses = bonuses, equippedSlot = equippedSlot
    )

    /* ---------------- Чистая логика экипировки ---------------- */

    @Test
    fun applyEquip_putsItemIntoSlot_andUnequipsPrevious() {
        val inventory = Inventory(listOf(
            item("sword", equippedSlot = EquipSlot.WEAPON),
            item("axe")
        ))

        val updated = inventory.applyEquip("axe")

        assertEquals(EquipSlot.WEAPON, updated.items.first { it.id == "axe" }.equippedSlot)
        assertNull(updated.items.first { it.id == "sword" }.equippedSlot)
    }

    @Test
    fun applyEquip_ignoresUnknownAndNonEquippableItems() {
        val inventory = Inventory(listOf(item("rock", slot = null)))

        assertEquals(inventory, inventory.applyEquip("rock"))
        assertEquals(inventory, inventory.applyEquip("missing"))
    }

    @Test
    fun applyUnequip_clearsSlot_andIsNoOpWhenEmpty() {
        val inventory = Inventory(listOf(item("sword", equippedSlot = EquipSlot.WEAPON)))

        val updated = inventory.applyUnequip(EquipSlot.WEAPON)

        assertNull(updated.items.first().equippedSlot)
        assertEquals(updated, updated.applyUnequip(EquipSlot.WEAPON))
    }

    @Test
    fun totalBonuses_sumsBonusesOfEquippedItemsOnly() {
        val inventory = Inventory(listOf(
            item(
                "helmet", slot = EquipSlot.HEAD, equippedSlot = EquipSlot.HEAD,
                bonuses = listOf(ItemBonus("str", "Сила", 1))
            ),
            item(
                "sword", equippedSlot = EquipSlot.WEAPON,
                bonuses = listOf(ItemBonus("str", "Сила", 2), ItemBonus("dex", "Ловкость", 1))
            ),
            item("axe", bonuses = listOf(ItemBonus("str", "Сила", 99)))
        ))

        val totals = inventory.totalBonuses().associate { it.statKey to it.value }

        assertEquals(mapOf("str" to 3, "dex" to 1), totals)
    }

    /* ---------------- ViewModel ---------------- */

    @Test
    fun loadInventory_publishesContent() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = InventoryViewModel(
            Application(), FakeInventoryDataSource(Inventory(listOf(item("sword")))),
            mainDispatcherRule.dispatcher
        )

        viewModel.loadInventory()
        advanceUntilIdle()

        val content = viewModel.uiState as InventoryUiState.Content
        assertEquals(1, content.inventory.items.size)
    }

    @Test
    fun equip_appliesOptimistically_andPersists() = runTest(mainDispatcherRule.dispatcher) {
        val repo = FakeInventoryDataSource(Inventory(listOf(item("sword"))))
        val viewModel = InventoryViewModel(Application(), repo, mainDispatcherRule.dispatcher)
        viewModel.loadInventory()
        advanceUntilIdle()

        viewModel.equip("sword")
        advanceUntilIdle()

        val content = viewModel.uiState as InventoryUiState.Content
        assertEquals(EquipSlot.WEAPON, content.inventory.items.first().equippedSlot)
        assertEquals(1, repo.equipCount)
    }

    @Test
    fun equip_revertsOnFailure() = runTest(mainDispatcherRule.dispatcher) {
        val repo = FakeInventoryDataSource(Inventory(listOf(item("sword"))), failMutations = true)
        val viewModel = InventoryViewModel(Application(), repo, mainDispatcherRule.dispatcher)
        viewModel.loadInventory()
        advanceUntilIdle()

        viewModel.equip("sword")
        advanceUntilIdle()

        val content = viewModel.uiState as InventoryUiState.Content
        assertNull(content.inventory.items.first().equippedSlot)
    }

    @Test
    fun unequip_clearsSlot() = runTest(mainDispatcherRule.dispatcher) {
        val repo = FakeInventoryDataSource(
            Inventory(listOf(item("sword", equippedSlot = EquipSlot.WEAPON)))
        )
        val viewModel = InventoryViewModel(Application(), repo, mainDispatcherRule.dispatcher)
        viewModel.loadInventory()
        advanceUntilIdle()

        viewModel.unequip(EquipSlot.WEAPON)
        advanceUntilIdle()

        val content = viewModel.uiState as InventoryUiState.Content
        assertTrue(content.inventory.items.none { it.isEquipped })
        assertEquals(1, repo.unequipCount)
    }

    private class FakeInventoryDataSource(
        initial: Inventory,
        private val failMutations: Boolean = false
    ) : InventoryDataSource {
        private var current = initial
        var equipCount = 0
            private set
        var unequipCount = 0
            private set

        override suspend fun getInventory(): Result<Inventory> = Result.success(current)

        override suspend fun equip(itemId: String): Result<Inventory> {
            if (failMutations) return Result.failure(RuntimeException("boom"))
            equipCount++
            current = current.applyEquip(itemId)
            return Result.success(current)
        }

        override suspend fun unequip(slot: EquipSlot): Result<Inventory> {
            if (failMutations) return Result.failure(RuntimeException("boom"))
            unequipCount++
            current = current.applyUnequip(slot)
            return Result.success(current)
        }
    }
}
