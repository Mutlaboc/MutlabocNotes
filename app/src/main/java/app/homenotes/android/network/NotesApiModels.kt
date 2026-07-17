package app.homenotes.android.network

import app.homenotes.android.ChecklistItem
import app.homenotes.android.Note
import app.homenotes.android.NoteCategory
import app.homenotes.android.RepeatRule

// Модель данных, общая для слоёв этого модуля.
data class ChecklistItemDto(
    val text: String,
    val isChecked: Boolean
)

// Модель данных, общая для слоёв этого модуля.
data class NoteDto(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val checklist: List<ChecklistItemDto>,
    val deadlineMillis: Long?,
    val startAtMillis: Long? = null,
    val durationMinutes: Long? = null,
    val repeatRule: String?,
    val coinCount: Int,
    val isCompleted: Boolean,
)

// Модель данных для request payload, отправляемого в backend.
data class NoteUpsertRequestDto(
    val title: String,
    val content: String,
    val category: String,
    val checklist: List<ChecklistItemDto>,
    val deadlineMillis: Long?,
    val startAtMillis: Long? = null,
    val durationMinutes: Long? = null,
    val repeatRule: String,
    val coinCount: Int,
    val isCompleted: Boolean,
)

data class NoteCompletionRequestDto(
    val isCompleted: Boolean,
)

data class NoteCompletionResponseDto(
    val completedNote: NoteDto,
    val nextNote: NoteDto?
)

// Преобразует строку категории из backend в enum доменной модели с безопасным fallback.
private fun String.toNoteCategory(): NoteCategory {
    return runCatching { NoteCategory.valueOf(this) }
        .getOrDefault(NoteCategory.TASKS)
}

private fun String?.toRepeatRule(): RepeatRule {
    return runCatching { RepeatRule.valueOf(this.orEmpty()) }
        .getOrDefault(RepeatRule.NONE)
}

// Преобразует доменную модель в её DTO-представление.
fun ChecklistItem.toDto(): ChecklistItemDto = ChecklistItemDto(
    text = text,
    isChecked = isChecked
)

// Преобразует данные DTO в доменную модель.
fun ChecklistItemDto.toDomain(): ChecklistItem = ChecklistItem(
    text = text,
    isChecked = isChecked
)

// Преобразует данные DTO в доменную модель.
fun NoteDto.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    category = category.toNoteCategory(),
    checklist = checklist.map { it.toDomain() },
    deadlineMillis = deadlineMillis,
    startAtMillis = startAtMillis,
    durationMinutes = durationMinutes,
    repeatRule = repeatRule.toRepeatRule(),
    coinCount = coinCount,
    isCompleted = isCompleted
)

// Формирует DTO upsert-запроса из данных доменной модели.
fun Note.toUpsertRequestDto(): NoteUpsertRequestDto = NoteUpsertRequestDto(
    title = title,
    content = content,
    category = category.name,
    checklist = checklist.map { it.toDto() },
    deadlineMillis = deadlineMillis,
    startAtMillis = startAtMillis,
    durationMinutes = durationMinutes,
    repeatRule = repeatRule.name,
    coinCount = coinCount,
    isCompleted = isCompleted
)
