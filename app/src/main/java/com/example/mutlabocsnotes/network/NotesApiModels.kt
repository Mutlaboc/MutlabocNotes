package com.example.mutlabocsnotes.network

import com.example.mutlabocsnotes.ChecklistItem
import com.example.mutlabocsnotes.Note
import com.example.mutlabocsnotes.NoteCategory

data class ChecklistItemDto(
    val text: String,
    val isChecked: Boolean
)

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

private fun String.toNoteCategory(): NoteCategory {
    return runCatching { NoteCategory.valueOf(this) }
        .getOrDefault(NoteCategory.NOTES)
}

fun ChecklistItem.toDto(): ChecklistItemDto = ChecklistItemDto(
    text = text,
    isChecked = isChecked
)

fun ChecklistItemDto.toDomain(): ChecklistItem = ChecklistItem(
    text = text,
    isChecked = isChecked
)

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
