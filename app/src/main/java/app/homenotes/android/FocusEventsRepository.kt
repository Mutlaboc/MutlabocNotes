package app.homenotes.android

import app.homenotes.android.local.LocalCharacterSkillEntity
import app.homenotes.android.local.LocalInventoryItemEntity
import app.homenotes.android.local.OfflineDao
import app.homenotes.android.local.OutboxEntity
import app.homenotes.android.network.toDomain
import com.google.gson.Gson
import java.util.UUID
import kotlin.random.Random

/**
 * Репозиторий полосы событий фокус-таймера (offline-first).
 *
 * Клиент сам роллит взвешенно-случайное событие из Room-кэша каталога (или из
 * встроенного фолбэка), сразу применяет награды к локальным проекциям персонажа /
 * инвентаря и кладёт клейм в outbox — [OutboxKind.EVENT_CLAIM] уходит на бекенд
 * идемпотентно по operationId, где начисление повторяется по ключу события.
 */
class FocusEventsRepository(
    private val dao: OfflineDao,
    private val session: AuthSessionStore,
    private val scheduler: SyncScheduler,
    private val gson: Gson = Gson(),
    private val random: Random = Random.Default,
) {

    /**
     * Роллит одно событие и применяет его локально. [sessionSkillKey] — навык текущей
     * сессии таймера (для SKILL_XP-событий), [english] — язык текстов.
     * Возвращает запись для полосы событий или null (нет аккаунта / пустой каталог).
     */
    suspend fun rollEvent(sessionSkillKey: String?, english: Boolean): FocusFeedEntry? {
        val account = normalizeAccountKey(session.getEmail()) ?: return null

        val cached = dao.focusEvents()
        val catalog = if (cached.isEmpty()) builtInFocusEvents else cached.map { it.toDomain(gson) }
        val ownedSkills = dao.characterSkills(account)
        val ownedItemKeys = dao.inventoryItems(account).mapTo(hashSetOf()) { it.itemId }

        // События, которые не могут дать награду, из ролла исключаются:
        // SKILL_XP без навыка сессии, дубликаты предметов и уже открытые навыки.
        val candidates = catalog.filter { event ->
            when (event.type) {
                FocusEventType.TEXT, FocusEventType.CHARACTER_XP -> true
                FocusEventType.SKILL_XP ->
                    sessionSkillKey != null && ownedSkills.any { it.skillKey == sessionSkillKey }
                FocusEventType.ITEM ->
                    event.item != null && event.item.key !in ownedItemKeys
                FocusEventType.NEW_SKILL ->
                    event.newSkill != null && ownedSkills.none { it.skillKey == event.newSkill.key }
            }
        }
        val event = pickWeightedFocusEvent(candidates, random) ?: return null

        var skillName: String? = null
        when (event.type) {
            FocusEventType.TEXT -> Unit
            FocusEventType.CHARACTER_XP -> applyXpToProjection(account, event.characterXp, null, 0)
            FocusEventType.SKILL_XP -> {
                skillName = ownedSkills.firstOrNull { it.skillKey == sessionSkillKey }?.name
                applyXpToProjection(account, 0, sessionSkillKey, event.skillXp)
            }
            FocusEventType.NEW_SKILL -> grantNewSkill(account, event.newSkill!!, english, ownedSkills)
            FocusEventType.ITEM -> grantItem(account, event.item!!, english)
        }

        // Клейм предмета блокирует pull инвентаря (hasPending по inventoryLocalId),
        // остальные — pull листа персонажа, чтобы синк не затёр локальные награды.
        val localId = if (event.type == FocusEventType.ITEM) inventoryLocalId(account) else account
        dao.enqueue(OutboxEntity(
            accountKey = account,
            kind = OutboxKind.EVENT_CLAIM,
            localId = localId,
            operationId = UUID.randomUUID().toString(),
            payload = gson.toJson(EventClaimPayload(
                eventKey = event.key,
                skillKey = sessionSkillKey.takeIf { event.type == FocusEventType.SKILL_XP },
                locale = if (english) "en" else "ru",
            )),
            createdAt = System.currentTimeMillis(),
        ))
        scheduler.request(account)

        return FocusFeedEntry(
            id = System.currentTimeMillis(),
            type = event.type,
            text = event.text(english),
            characterXp = event.characterXp,
            skillXp = event.skillXp,
            skillName = skillName,
            itemName = event.item?.name(english),
            itemIcon = event.item?.icon,
            newSkillName = event.newSkill?.name(english),
        )
    }

    // Применяет опыт к локальной проекции персонажа (без собственной outbox-операции:
    // на бекенде опыт начислит клейм события).
    private suspend fun applyXpToProjection(account: String, characterXp: Int, skillKey: String?, skillXp: Int) {
        val entity = dao.character(account) ?: return
        val sheet = entity.toDomain(dao.characterStats(account), dao.characterSkills(account))
        val updated = applyExperience(sheet, characterXp, skillKey, skillXp)
        dao.putCharacter(entity.copy(level = updated.level, xp = updated.xp, xpToNext = updated.xpToNext))
        dao.putCharacterSkills(updated.skills.mapIndexed { i, skill ->
            LocalCharacterSkillEntity(account, skill.key, i, skill.name, skill.level, skill.progress.toDouble())
        })
    }

    private suspend fun grantNewSkill(
        account: String,
        newSkill: FocusEventNewSkill,
        english: Boolean,
        ownedSkills: List<LocalCharacterSkillEntity>,
    ) {
        if (ownedSkills.any { it.skillKey == newSkill.key }) return
        val nextPosition = (ownedSkills.maxOfOrNull { it.position } ?: -1) + 1
        dao.putCharacterSkills(listOf(
            LocalCharacterSkillEntity(
                accountKey = account,
                skillKey = newSkill.key,
                position = nextPosition,
                name = newSkill.name(english),
                level = 1,
                progress = 0.0,
            )
        ))
    }

    private suspend fun grantItem(account: String, item: FocusEventItem, english: Boolean) {
        val existing = dao.inventoryItems(account)
        if (existing.any { it.itemId == item.key }) return
        val bonus = item.bonusStatKey?.let { statKey ->
            ItemBonus(
                statKey = statKey,
                statName = item.bonusStatName(english) ?: statKey,
                value = item.bonusValue ?: 0,
            )
        }?.takeIf { it.value != 0 }
        dao.putInventoryItems(listOf(
            LocalInventoryItemEntity(
                accountKey = account,
                itemId = item.key,
                position = (existing.maxOfOrNull { it.position } ?: -1) + 1,
                name = item.name(english),
                description = item.description(english),
                icon = item.icon,
                slot = item.slot?.takeIf { EquipSlot.fromKey(it) != null },
                rarity = ItemRarity.fromKey(item.rarity).name,
                bonusesJson = gson.toJson(listOfNotNull(bonus)),
                equippedSlot = null,
            )
        ))
    }
}
