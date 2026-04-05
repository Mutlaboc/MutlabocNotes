package com.example.mutlabocsnotes.network

import com.example.mutlabocsnotes.ChecklistItem
import com.example.mutlabocsnotes.Note
import com.example.mutlabocsnotes.NoteCategory

// Data model shared between layers of this module.
data class ChecklistItemDto(
    val text: String,
    val isChecked: Boolean
)

// Data model shared between layers of this module.
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

// Data model for request payloads sent to the backend.
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

// Parses backend category strings into the domain enum with safe fallback.
private fun String.toNoteCategory(): NoteCategory {
    return runCatching { NoteCategory.valueOf(this) }
        .getOrDefault(NoteCategory.NOTES)
}

// Converts a domain model to its DTO representation.
fun ChecklistItem.toDto(): ChecklistItemDto = ChecklistItemDto(
    text = text,
    isChecked = isChecked
)

// Converts DTO data to a domain model.
fun ChecklistItemDto.toDomain(): ChecklistItem = ChecklistItem(
    text = text,
    isChecked = isChecked
)

// Converts DTO data to a domain model.
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

// Builds an upsert request DTO from domain data.
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
