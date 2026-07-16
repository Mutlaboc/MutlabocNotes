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
            selectedCategory = NoteCategory.TASKS,
            checklistItems = emptyList(),
            selectedDeadlineMillis = DEADLINE,
            repeatRule = RepeatRule.MONTHLY,
            coinCountText = "2",
            defaultCoinCount = 1
        )

        assertNull(prepared)
    }

    @Test
    fun prepareNoteForSave_shoppingClearsHiddenContentAndFiltersBlankCheckedItems() {
        val original = Note(
            id = "shopping",
            title = "Groceries",
            content = "stale hidden content",
            category = NoteCategory.TASKS,
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
                repeatRule = RepeatRule.MONTHLY,
                coinCountText = "4",
                defaultCoinCount = 1
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
                repeatRule = RepeatRule.WEEKLY,
                coinCountText = "bad",
                defaultCoinCount = 3
            )
        )

        assertEquals("Task body", prepared.content)
        assertEquals(NoteCategory.TASKS, prepared.category)
        assertEquals(emptyList<ChecklistItem>(), prepared.checklist)
        assertEquals(DEADLINE, prepared.deadlineMillis)
        assertEquals(RepeatRule.NONE, prepared.repeatRule)
        assertEquals(3, prepared.coinCount)
    }

    @Test
    fun prepareNoteForSave_recurringTaskKeepsScheduleAndClearsDeadline() {
        val prepared = checkNotNull(
            prepareNoteForSave(
                noteId = "note",
                originalNote = null,
                title = "Note",
                content = "Note body",
                selectedCategory = NoteCategory.RECURRING_TASKS,
                checklistItems = listOf(ChecklistItem(text = "Milk")),
                selectedDeadlineMillis = DEADLINE,
                selectedStartAtMillis = DEADLINE,
                durationDays = 1,
                durationHours = 2,
                repeatRule = RepeatRule.MONTHLY,
                coinCountText = "5",
                defaultCoinCount = 1
            )
        )

        assertEquals("Note body", prepared.content)
        assertEquals(NoteCategory.RECURRING_TASKS, prepared.category)
        assertEquals(emptyList<ChecklistItem>(), prepared.checklist)
        assertEquals(null, prepared.deadlineMillis)
        assertEquals(DEADLINE, prepared.startAtMillis)
        assertEquals(1_560L, prepared.durationMinutes)
        assertEquals(RepeatRule.MONTHLY, prepared.repeatRule)
        assertEquals(5, prepared.coinCount)
    }

    private companion object {
        const val DEADLINE = 1_700_000_000_000L
    }
}
