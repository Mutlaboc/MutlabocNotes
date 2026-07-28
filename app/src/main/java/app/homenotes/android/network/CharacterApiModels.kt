package app.homenotes.android.network

import app.homenotes.android.CharacterSheet
import app.homenotes.android.CharacterSkill
import app.homenotes.android.CharacterStat
import app.homenotes.android.R
import kotlinx.serialization.Serializable

// DTO одной характеристики.
@Serializable
data class CharacterStatDto(
    val key: String,
    val name: String,
    val description: String,
    val value: Int,
)

// DTO одного навыка.
@Serializable
data class CharacterSkillDto(
    val key: String,
    val name: String,
    val level: Int,
    val progress: Double,
)

// DTO полного листа персонажа (используется и для ответа GET, и для тела PUT).
@Serializable
data class CharacterSheetDto(
    val name: String,
    val level: Int,
    val xp: Int,
    val xpToNext: Int,
    val stats: List<CharacterStatDto>,
    val skills: List<CharacterSkillDto>,
    val wallet: CharacterWalletDto = CharacterWalletDto(),
)

@Serializable
data class CharacterWalletDto(
    val earnedCoins: Int = 0,
    val spentCoins: Int = 0,
    val availableCoins: Int = 0,
)

// Тело запроса на начисление опыта.
@Serializable
data class CharacterXpRequestDto(
    val characterXp: Int,
    val skillKey: String?,
    val skillXp: Int,
    val operationId: String? = null,
)

@Serializable
data class CharacterStatUpgradeRequestDto(val operationId: String, val statKey: String)
@Serializable
data class CharacterRenameRequestDto(val operationId: String, val name: String)

// Преобразует DTO в доменную модель. Портрет — клиентский ресурс, его на бекенде нет.
fun CharacterSheetDto.toDomain(): CharacterSheet = CharacterSheet(
    name = name,
    level = level,
    xp = xp,
    xpToNext = xpToNext,
    portraitRes = R.drawable.mascot_stand_01,
    stats = stats.map {
        CharacterStat(name = it.name, description = it.description, value = it.value, key = it.key)
    },
    skills = skills.map {
        CharacterSkill(name = it.name, level = it.level, progress = it.progress.toFloat(), key = it.key)
    },
)

// Формирует тело PUT-запроса из доменной модели.
fun CharacterSheet.toUpdateRequest(): CharacterSheetDto = CharacterSheetDto(
    name = name,
    level = level,
    xp = xp,
    xpToNext = xpToNext,
    stats = stats.map {
        CharacterStatDto(key = it.key, name = it.name, description = it.description, value = it.value)
    },
    skills = skills.map {
        CharacterSkillDto(key = it.key, name = it.name, level = it.level, progress = it.progress.toDouble())
    },
)
