package com.example.mutlabocsnotes

/**
 * Represents a single item inside a checklist. Each item stores the
 * text entered by the user and whether it was marked as completed.
 */

data class CheklistItem(
    val text: String = "",
    val isChecked: Boolean = false
)

/**
 * Categories supported by a note.  They map directly to the user facing
 * options «Покупки», «Дела» и «Заметки».
 */

enum class NoteCategory {
    SHOPPING,
    TASKS,
    NOTES
}

/**
 * Core domain model used across the application and stored in Firestore.
 */

data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: NoteCategory = NoteCategory.NOTES,
    val checklist: List<CheklistItem> = emptyList(),
    val deadlineMillis: Long? = null,
    val isRepeating: Boolean = false,
    val coinCount: Int = 0
)
