package com.example.mutlabocsnotes.network

import com.example.mutlabocsnotes.CharacterSheet
import com.example.mutlabocsnotes.CharacterSkill
import com.example.mutlabocsnotes.CharacterStat
import com.example.mutlabocsnotes.R

// DTO одной характеристики.
data class CharacterStatDto(
    val key: String,
    val name: String,
    val description: String,
    val value: Int,
)

// DTO одного навыка.
data class CharacterSkillDto(
    val key: String,
    val name: String,
    val level: Int,
    val progress: Double,
)

// DTO полного листа персонажа (используется и для ответа GET, и для тела PUT).
data class CharacterSheetDto(
    val name: String,
    val level: Int,
    val xp: Int,
    val xpToNext: Int,
    val stats: List<CharacterStatDto>,
    val skills: List<CharacterSkillDto>,
)

// Тело запроса на начисление опыта.
data class CharacterXpRequestDto(
    val characterXp: Int,
    val skillKey: String?,
    val skillXp: Int,
)

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
