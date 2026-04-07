package com.example.mutlabocsnotes.network

import com.example.mutlabocsnotes.HomeField
import com.example.mutlabocsnotes.HomeInfoCard
import com.example.mutlabocsnotes.HomeSection

// Модель данных, общая для слоёв этого модуля.
data class HomeFieldDto(
    val key: String,
    val value: String,
)

// Модель данных, общая для слоёв этого модуля.
data class HomeCardDto(
    val id: String,
    val title: String,
    val section: String,
    val fields: List<HomeFieldDto>,
    val note: String,
    val links: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
)

// Модель данных для request payload, отправляемого в backend.
data class HomeCardUpsertRequestDto(
    val title: String,
    val section: String,
    val fields: List<HomeFieldDto>,
    val note: String,
    val links: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
)

// Преобразует строку раздела из backend в enum доменной модели с безопасным fallback.
private fun String.toHomeSection(): HomeSection {
    return runCatching { HomeSection.valueOf(this) }
        .getOrDefault(HomeSection.OTHER)
}

// Преобразует доменную модель в её DTO-представление.
fun HomeField.toDto(): HomeFieldDto = HomeFieldDto(
    key = key,
    value = value,
)

// Преобразует данные DTO в доменную модель.
fun HomeFieldDto.toDomain(): HomeField = HomeField(
    key = key,
    value = value,
)

// Преобразует данные DTO в доменную модель.
fun HomeCardDto.toDomain(): HomeInfoCard = HomeInfoCard(
    id = id,
    title = title,
    section = section.toHomeSection(),
    fields = fields.map { it.toDomain() },
    note = note,
    links = links,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// Формирует DTO upsert-запроса из данных доменной модели.
fun HomeInfoCard.toUpsertRequestDto(): HomeCardUpsertRequestDto = HomeCardUpsertRequestDto(
    title = title,
    section = section.name,
    fields = fields.map { it.toDto() },
    note = note,
    links = links,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
