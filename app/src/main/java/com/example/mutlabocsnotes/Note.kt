package com.example.mutlabocsnotes

/**
Один пункт в чек-листе
 */

data class CheklistItem(
    val text: String = "",
    val isChecked: Boolean = false
)

/**
 * Категории.   «Покупки», «Дела» и «Заметки».
 */

enum class NoteCategory {
    SHOPPING,
    TASKS,
    NOTES
}

/**
 * Базовая модель
 */

data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: NoteCategory = NoteCategory.NOTES,
    val checklist: List<CheklistItem> = emptyList(),
    val deadlineMillis: Long? = null,
    val isRepeating: Boolean = false,
    val coinCount: Int = 0,
    val isCompleted: Boolean = false
)
