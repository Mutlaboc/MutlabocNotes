package app.homenotes.android.network

import app.homenotes.android.HomeField
import app.homenotes.android.HomeInfoCard
import app.homenotes.android.HomeSection
import kotlinx.serialization.Serializable

// Модель данных поля карточки дома.
@Serializable
data class HomeFieldDto(
    val key: String,
    val value: String,
)

// Модель данных для карточки дома.
@Serializable
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
@Serializable
data class HomeCardUpsertRequestDto(
    val title: String,
    val section: String,
    val fields: List<HomeFieldDto>,
    val note: String,
    val links: List<String>,
    val clientMutationId: String? = null,
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
fun HomeInfoCard.toUpsertRequestDto(clientMutationId: String? = null): HomeCardUpsertRequestDto = HomeCardUpsertRequestDto(
    title = title,
    section = section.name,
    fields = fields.map { it.toDto() },
    note = note,
    links = links,
    clientMutationId = clientMutationId,
)
