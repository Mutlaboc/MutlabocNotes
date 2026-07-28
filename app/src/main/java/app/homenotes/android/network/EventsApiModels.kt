package app.homenotes.android.network

import app.homenotes.android.FocusEvent
import app.homenotes.android.FocusEventItem
import app.homenotes.android.FocusEventNewSkill
import app.homenotes.android.FocusEventType
import app.homenotes.android.local.LocalFocusEventEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

// DTO предмета-награды события (обе локали приходят разом — кэшируем целиком).
@Serializable
data class FocusEventItemDto(
    val key: String,
    val nameRu: String,
    val nameEn: String,
    val descriptionRu: String = "",
    val descriptionEn: String = "",
    val icon: String = "",
    val slot: String? = null,
    val rarity: String = "COMMON",
    val bonusStatKey: String? = null,
    val bonusStatNameRu: String? = null,
    val bonusStatNameEn: String? = null,
    val bonusValue: Int? = null,
)

// DTO нового навыка-награды события.
@Serializable
data class FocusEventNewSkillDto(
    val key: String,
    val nameRu: String,
    val nameEn: String,
)

// DTO одного события каталога.
@Serializable
data class FocusEventDto(
    val key: String,
    val type: String,
    val weight: Int,
    val textRu: String,
    val textEn: String,
    val characterXp: Int = 0,
    val skillXp: Int = 0,
    val item: FocusEventItemDto? = null,
    val newSkill: FocusEventNewSkillDto? = null,
)

// Ответ GET /events.
@Serializable
data class FocusEventsCatalogDto(
    val events: List<FocusEventDto> = emptyList(),
)

// Тело POST /events/claims. [locale] определяет язык имени предмета/навыка на сервере.
@Serializable
data class FocusEventClaimRequestDto(
    val operationId: String,
    val eventKey: String,
    val skillKey: String? = null,
    val locale: String? = null,
)

// Ответ клейма: свежий лист персонажа и выданный предмет (если был).
@Serializable
data class FocusEventClaimResponseDto(
    val sheet: CharacterSheetDto,
    val grantedItem: InventoryItemDto? = null,
)

fun FocusEventDto.toDomain(): FocusEvent = FocusEvent(
    key = key,
    type = FocusEventType.fromKey(type),
    weight = weight,
    textRu = textRu,
    textEn = textEn,
    characterXp = characterXp,
    skillXp = skillXp,
    item = item?.let {
        FocusEventItem(
            key = it.key,
            nameRu = it.nameRu,
            nameEn = it.nameEn,
            descriptionRu = it.descriptionRu,
            descriptionEn = it.descriptionEn,
            icon = it.icon,
            slot = it.slot,
            rarity = it.rarity,
            bonusStatKey = it.bonusStatKey,
            bonusStatNameRu = it.bonusStatNameRu,
            bonusStatNameEn = it.bonusStatNameEn,
            bonusValue = it.bonusValue,
        )
    },
    newSkill = newSkill?.let { FocusEventNewSkill(key = it.key, nameRu = it.nameRu, nameEn = it.nameEn) },
)

// Сериализация домена в Room-кэш и обратно (item/новый навык — JSON-колонки).
fun FocusEvent.toEntity(): LocalFocusEventEntity = LocalFocusEventEntity(
    eventKey = key,
    eventType = type.name,
    weight = weight,
    textRu = textRu,
    textEn = textEn,
    characterXp = characterXp,
    skillXp = skillXp,
    itemJson = item?.let { ApiJson.encodeToString(it) },
    newSkillJson = newSkill?.let { ApiJson.encodeToString(it) },
)

fun LocalFocusEventEntity.toDomain(): FocusEvent = FocusEvent(
    key = eventKey,
    type = FocusEventType.fromKey(eventType),
    weight = weight,
    textRu = textRu,
    textEn = textEn,
    characterXp = characterXp,
    skillXp = skillXp,
    item = itemJson?.let { runCatching { ApiJson.decodeFromString<FocusEventItem>(it) }.getOrNull() },
    newSkill = newSkillJson?.let { runCatching { ApiJson.decodeFromString<FocusEventNewSkill>(it) }.getOrNull() },
)
