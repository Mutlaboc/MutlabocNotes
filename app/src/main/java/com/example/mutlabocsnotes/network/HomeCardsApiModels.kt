package com.example.mutlabocsnotes.network

import com.example.mutlabocsnotes.HomeField
import com.example.mutlabocsnotes.HomeInfoCard
import com.example.mutlabocsnotes.HomeSection

data class HomeFieldDto(
    val key: String,
    val value: String,
)

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

data class HomeCardUpsertRequestDto(
    val title: String,
    val section: String,
    val fields: List<HomeFieldDto>,
    val note: String,
    val links: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
)

private fun String.toHomeSection(): HomeSection {
    return runCatching { HomeSection.valueOf(this) }
        .getOrDefault(HomeSection.OTHER)
}

fun HomeField.toDto(): HomeFieldDto = HomeFieldDto(
    key = key,
    value = value,
)

fun HomeFieldDto.toDomain(): HomeField = HomeField(
    key = key,
    value = value,
)

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

fun HomeInfoCard.toUpsertRequestDto(): HomeCardUpsertRequestDto = HomeCardUpsertRequestDto(
    title = title,
    section = section.name,
    fields = fields.map { it.toDto() },
    note = note,
    links = links,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
