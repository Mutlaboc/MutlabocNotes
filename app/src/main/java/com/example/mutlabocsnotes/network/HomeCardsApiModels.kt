package com.example.mutlabocsnotes.network

import com.example.mutlabocsnotes.HomeField
import com.example.mutlabocsnotes.HomeInfoCard
import com.example.mutlabocsnotes.HomeSection

// Data model shared between layers of this module.
data class HomeFieldDto(
    val key: String,
    val value: String,
)

// Data model shared between layers of this module.
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

// Data model for request payloads sent to the backend.
data class HomeCardUpsertRequestDto(
    val title: String,
    val section: String,
    val fields: List<HomeFieldDto>,
    val note: String,
    val links: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
)

// Parses backend section strings into the domain enum with safe fallback.
private fun String.toHomeSection(): HomeSection {
    return runCatching { HomeSection.valueOf(this) }
        .getOrDefault(HomeSection.OTHER)
}

// Converts a domain model to its DTO representation.
fun HomeField.toDto(): HomeFieldDto = HomeFieldDto(
    key = key,
    value = value,
)

// Converts DTO data to a domain model.
fun HomeFieldDto.toDomain(): HomeField = HomeField(
    key = key,
    value = value,
)

// Converts DTO data to a domain model.
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

// Builds an upsert request DTO from domain data.
fun HomeInfoCard.toUpsertRequestDto(): HomeCardUpsertRequestDto = HomeCardUpsertRequestDto(
    title = title,
    section = section.name,
    fields = fields.map { it.toDto() },
    note = note,
    links = links,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
