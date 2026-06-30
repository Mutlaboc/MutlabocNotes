package com.example.mutlabocsnotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditNotePreparationTest {

    @Test
    fun prepareNoteForSave_blankTitleReturnsNull() {
        val prepared = prepareNoteForSave(
            noteId = "",
            originalNote = null,
            title = "   ",
            content = "Body",
            selectedCategory = NoteCategory.NOTES,
            checklistItems = emptyList(),
            selectedDeadlineMillis = DEADLINE,
            repeatRule = RepeatRule.MONTHLY
        )

        assertNull(prepared)
    }

    @Test
    fun prepareNoteForSave_shoppingClearsHiddenContentAndFiltersBlankCheckedItems() {
        val original = Note(
            id = "shopping",
            title = "Groceries",
            content = "stale hidden content",
            category = NoteCategory.NOTES,
            coinCount = 4,
            isCompleted = true
        )

        val prepared = checkNotNull(
            prepareNoteForSave(
                noteId = "shopping",
                originalNote = original,
                title = "  Groceries  ",
                content = "stale hidden content",
                selectedCategory = NoteCategory.SHOPPING,
                checklistItems = listOf(
                    ChecklistItem(text = "  Milk  ", isChecked = false),
                    ChecklistItem(text = "   ", isChecked = true)
                ),
                selectedDeadlineMillis = DEADLINE,
                repeatRule = RepeatRule.MONTHLY
            )
        )

        assertEquals("Groceries", prepared.title)
        assertEquals("", prepared.content)
        assertEquals(NoteCategory.SHOPPING, prepared.category)
        assertEquals(listOf(ChecklistItem(text = "Milk", isChecked = false)), prepared.checklist)
        assertEquals(null, prepared.deadlineMillis)
        assertEquals(RepeatRule.NONE, prepared.repeatRule)
        assertEquals(4, prepared.coinCount)
        assertEquals(true, prepared.isCompleted)
    }

    @Test
    fun prepareNoteForSave_tasksKeepsContentAndDeadlineButClearsChecklist() {
        val prepared = checkNotNull(
            prepareNoteForSave(
                noteId = "task",
                originalNote = null,
                title = "Task",
                content = "Task body",
                selectedCategory = NoteCategory.TASKS,
                checklistItems = listOf(ChecklistItem(text = "Milk")),
                selectedDeadlineMillis = DEADLINE,
                repeatRule = RepeatRule.WEEKLY
            )
        )

        assertEquals("Task body", prepared.content)
        assertEquals(NoteCategory.TASKS, prepared.category)
        assertEquals(emptyList<ChecklistItem>(), prepared.checklist)
        assertEquals(DEADLINE, prepared.deadlineMillis)
        assertEquals(RepeatRule.WEEKLY, prepared.repeatRule)
        assertEquals(0, prepared.coinCount)
    }

    @Test
    fun prepareNoteForSave_notesKeepsContentButClearsTaskOnlyFields() {
        val prepared = checkNotNull(
            prepareNoteForSave(
                noteId = "note",
                originalNote = null,
                title = "Note",
                content = "Note body",
                selectedCategory = NoteCategory.NOTES,
                checklistItems = listOf(ChecklistItem(text = "Milk")),
                selectedDeadlineMillis = DEADLINE,
                repeatRule = RepeatRule.MONTHLY
            )
        )

        assertEquals("Note body", prepared.content)
        assertEquals(NoteCategory.NOTES, prepared.category)
        assertEquals(emptyList<ChecklistItem>(), prepared.checklist)
        assertEquals(null, prepared.deadlineMillis)
        assertEquals(RepeatRule.NONE, prepared.repeatRule)
        assertEquals(0, prepared.coinCount)
    }

    private companion object {
        const val DEADLINE = 1_700_000_000_000L
    }
}
