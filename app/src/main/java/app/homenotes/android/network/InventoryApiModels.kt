package app.homenotes.android.network

import app.homenotes.android.EquipSlot
import app.homenotes.android.Inventory
import app.homenotes.android.InventoryItem
import app.homenotes.android.ItemBonus
import app.homenotes.android.ItemRarity

// DTO бонуса предмета к характеристике.
data class ItemBonusDto(
    val statKey: String,
    val statName: String,
    val value: Int,
)

// DTO одного предмета инвентаря.
data class InventoryItemDto(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    // Слот в верхнем регистре (HEAD/BODY/LEGS/WEAPON/OFFHAND/ACCESSORY) или null.
    val slot: String?,
    val rarity: String = "COMMON",
    val bonuses: List<ItemBonusDto> = emptyList(),
    val equippedSlot: String? = null,
)

// DTO полного инвентаря (ответ GET и всех мутаций).
data class InventoryDto(
    val items: List<InventoryItemDto> = emptyList(),
)

// Тело запроса «надеть предмет». Слот определяет сам предмет.
data class InventoryEquipRequestDto(val operationId: String, val itemId: String)

// Тело запроса «снять предмет из слота».
data class InventoryUnequipRequestDto(val operationId: String, val slot: String)

fun InventoryDto.toDomain(): Inventory = Inventory(
    items = items.map { it.toDomain() },
)

fun InventoryItemDto.toDomain(): InventoryItem = InventoryItem(
    id = id,
    name = name,
    description = description,
    icon = icon,
    slot = EquipSlot.fromKey(slot),
    rarity = ItemRarity.fromKey(rarity),
    bonuses = bonuses.map { ItemBonus(it.statKey, it.statName, it.value) },
    equippedSlot = EquipSlot.fromKey(equippedSlot),
)
