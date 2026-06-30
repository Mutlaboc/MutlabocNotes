package com.example.homenotes

/**
Один пункт в чек-листе
 */

data class ChecklistItem(
    val text: String = "",
    val isChecked: Boolean = false
)

/**
 * Категории.  «Покупки», «Дела» и «Заметки».
 */

enum class NoteCategory {
    SHOPPING,
    TASKS,
    NOTES
}

enum class RepeatRule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY
}

/**
 * Базовая модель
 */

data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: NoteCategory = NoteCategory.NOTES,
    val checklist: List<ChecklistItem> = emptyList(),
    val deadlineMillis: Long? = null,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val coinCount: Int = 0,
    val isCompleted: Boolean = false
)
