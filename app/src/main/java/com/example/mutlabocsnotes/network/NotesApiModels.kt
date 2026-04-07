package com.example.mutlabocsnotes.network

import com.example.mutlabocsnotes.ChecklistItem
import com.example.mutlabocsnotes.Note
import com.example.mutlabocsnotes.NoteCategory

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
    val isRepeating: Boolean,
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
    val isRepeating: Boolean,
    val coinCount: Int,
    val isCompleted: Boolean,
)

// Преобразует строку категории из backend в enum доменной модели с безопасным fallback.
private fun String.toNoteCategory(): NoteCategory {
    return runCatching { NoteCategory.valueOf(this) }
        .getOrDefault(NoteCategory.NOTES)
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
    isRepeating = isRepeating,
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
    isRepeating = isRepeating,
    coinCount = coinCount,
    isCompleted = isCompleted
)
