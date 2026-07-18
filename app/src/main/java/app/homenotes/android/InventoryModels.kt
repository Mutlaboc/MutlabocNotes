package app.homenotes.android

/* ------------------------------------------------------------------ *
 * Доменная модель инвентаря и экипировки. Слоты — классика D&D.
 * Иконки предметов приходят с бекенда как текстовые глифы (emoji),
 * пока для них не нарисованы пиксель-арт ассеты.
 * ------------------------------------------------------------------ */

/** Слот экипировки. Порядок объявления задаёт порядок отображения. */
enum class EquipSlot {
    HEAD, BODY, LEGS, WEAPON, OFFHAND, ACCESSORY;

    companion object {
        /** Разбор строки с бекенда; неизвестное значение -> null (слот не показываем). */
        fun fromKey(key: String?): EquipSlot? =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
    }
}

/** Редкость предмета. Порядок — от простого к легендарному. */
enum class ItemRarity {
    COMMON, UNCOMMON, RARE, EPIC, LEGENDARY;

    companion object {
        fun fromKey(key: String?): ItemRarity =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: COMMON
    }
}

/** Бонус предмета к характеристике: ключ и имя совпадают со статами листа персонажа. */
data class ItemBonus(
    val statKey: String,
    val statName: String,
    val value: Int,
)

/**
 * Предмет инвентаря. [slot] == null — предмет нельзя надеть (материал, сувенир).
 * [equippedSlot] != null — предмет сейчас надет в этот слот.
 */
data class InventoryItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val slot: EquipSlot?,
    val rarity: ItemRarity = ItemRarity.COMMON,
    val bonuses: List<ItemBonus> = emptyList(),
    val equippedSlot: EquipSlot? = null,
) {
    val isEquipped: Boolean get() = equippedSlot != null
    val isEquippable: Boolean get() = slot != null
}

/** Инвентарь персонажа: все предметы, включая надетые. */
data class Inventory(
    val items: List<InventoryItem> = emptyList(),
) {
    /** Предмет, надетый в [slot], или null. */
    fun equippedIn(slot: EquipSlot): InventoryItem? =
        items.firstOrNull { it.equippedSlot == slot }

    /** Суммарные бонусы всей надетой экипировки: statKey -> (statName, сумма). */
    fun totalBonuses(): List<ItemBonus> = items
        .filter { it.isEquipped }
        .flatMap { it.bonuses }
        .groupBy { it.statKey }
        .map { (key, group) ->
            ItemBonus(statKey = key, statName = group.first().statName, value = group.sumOf { it.value })
        }
        .filter { it.value != 0 }
}

/**
 * Надевает предмет [itemId] в его слот, снимая предмет, который был там раньше.
 * Чистая функция — используется и репозиторием (optimistic), и тестами.
 * Возвращает исходный инвентарь, если предмет не найден или его нельзя надеть.
 */
fun Inventory.applyEquip(itemId: String): Inventory {
    val target = items.firstOrNull { it.id == itemId } ?: return this
    val slot = target.slot ?: return this
    if (target.equippedSlot == slot) return this
    return copy(items = items.map { item ->
        when {
            item.id == itemId -> item.copy(equippedSlot = slot)
            item.equippedSlot == slot -> item.copy(equippedSlot = null)
            else -> item
        }
    })
}

/** Снимает предмет из слота [slot]; без изменений, если слот пуст. */
fun Inventory.applyUnequip(slot: EquipSlot): Inventory =
    copy(items = items.map { item ->
        if (item.equippedSlot == slot) item.copy(equippedSlot = null) else item
    })

/** Контракт данных инвентаря (см. CharacterDataSource — тот же паттерн). */
interface InventoryDataSource {
    suspend fun getInventory(): Result<Inventory>
    suspend fun equip(itemId: String): Result<Inventory>
    suspend fun unequip(slot: EquipSlot): Result<Inventory>
}
