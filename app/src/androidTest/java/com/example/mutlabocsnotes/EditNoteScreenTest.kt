package com.example.mutlabocsnotes

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class EditNoteScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savingWithEmptyTitleShowsValidationAndDoesNotSave() {
        var savedNote: Note? = null
        setEditNoteContent(
            note = null,
            onSaveClick = { savedNote = it }
        )

        clickSave()

        composeRule.onNodeWithTag(EDIT_NOTE_TITLE_ERROR_TEST_TAG).assertIsDisplayed()
        composeRule.runOnIdle {
            assertNull(savedNote)
        }
    }

    @Test
    fun checklistItemCanBeDeletedBeforeSave() {
        var savedNote: Note? = null
        setEditNoteContent(
            note = Note(
                id = "shopping",
                title = "Groceries",
                category = NoteCategory.SHOPPING,
                checklist = listOf(
                    ChecklistItem(text = "Milk"),
                    ChecklistItem(text = "Bread")
                )
            ),
            onSaveClick = { savedNote = it }
        )

        composeRule.onNodeWithTag(editNoteChecklistItemDeleteButtonTestTag(0)).performClick()
        composeRule.onAllNodesWithTag(editNoteChecklistItemFieldTestTag(1)).assertCountEquals(0)
        clickSave()

        composeRule.runOnIdle {
            assertEquals(listOf(ChecklistItem(text = "Bread")), checkNotNull(savedNote).checklist)
        }
    }

    @Test
    fun checklistItemCanBeAddedBeforeSave() {
        var savedNote: Note? = null
        setEditNoteContent(
            note = Note(
                id = "shopping",
                title = "Groceries",
                category = NoteCategory.SHOPPING,
                checklist = listOf(ChecklistItem(text = "Milk"))
            ),
            onSaveClick = { savedNote = it }
        )

        composeRule.onNodeWithTag(EDIT_NOTE_CHECKLIST_ADD_BUTTON_TEST_TAG).performClick()
        composeRule.onNodeWithTag(editNoteChecklistItemFieldTestTag(1)).performTextInput("Bread")
        clickSave()

        composeRule.runOnIdle {
            assertEquals(
                listOf(ChecklistItem(text = "Milk"), ChecklistItem(text = "Bread")),
                checkNotNull(savedNote).checklist
            )
        }
    }

    @Test
    fun deleteRequiresConfirmBeforeCallback() {
        var deleteCalls = 0
        setEditNoteContent(
            note = Note(id = "delete-me", title = "Delete me"),
            onDeleteClick = { deleteCalls += 1 }
        )

        composeRule.onNodeWithTag(EDIT_NOTE_DELETE_BUTTON_TEST_TAG).performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(0, deleteCalls)
        }

        composeRule.onNodeWithTag(EDIT_NOTE_DELETE_CONFIRM_BUTTON_TEST_TAG).performClick()

        composeRule.runOnIdle {
            assertEquals(1, deleteCalls)
        }
    }

    @Test
    fun savingNotesEmitsNotesCategoryFields() {
        var savedNote: Note? = null
        setEditNoteContent(
            note = null,
            onSaveClick = { savedNote = it }
        )

        composeRule.onNodeWithTag(EDIT_NOTE_TITLE_FIELD_TEST_TAG).performTextInput("Note")
        composeRule.onNodeWithTag(EDIT_NOTE_CONTENT_FIELD_TEST_TAG).performTextInput("Note body")
        clickSave()

        composeRule.runOnIdle {
            val note = checkNotNull(savedNote)
            assertEquals("Note", note.title)
            assertEquals("Note body", note.content)
            assertEquals(NoteCategory.NOTES, note.category)
            assertEquals(emptyList<ChecklistItem>(), note.checklist)
            assertEquals(null, note.deadlineMillis)
            assertEquals(false, note.isRepeating)
        }
    }

    @Test
    fun savingTasksEmitsTaskOnlyFields() {
        var savedNote: Note? = null
        setEditNoteContent(
            note = Note(
                id = "task",
                title = "Task",
                content = "Task body",
                category = NoteCategory.TASKS,
                checklist = listOf(ChecklistItem(text = "hidden")),
                deadlineMillis = DEADLINE,
                isRepeating = true,
                coinCount = 2
            ),
            onSaveClick = { savedNote = it }
        )

        clickSave()

        composeRule.runOnIdle {
            val note = checkNotNull(savedNote)
            assertEquals("Task body", note.content)
            assertEquals(NoteCategory.TASKS, note.category)
            assertEquals(emptyList<ChecklistItem>(), note.checklist)
            assertEquals(DEADLINE, note.deadlineMillis)
            assertEquals(true, note.isRepeating)
        }
    }

    @Test
    fun savingShoppingEmitsShoppingOnlyFields() {
        var savedNote: Note? = null
        setEditNoteContent(
            note = Note(
                id = "shopping",
                title = "Groceries",
                content = "hidden content",
                category = NoteCategory.SHOPPING,
                checklist = listOf(
                    ChecklistItem(text = " Milk "),
                    ChecklistItem(text = " ", isChecked = true)
                ),
                deadlineMillis = DEADLINE,
                isRepeating = true
            ),
            onSaveClick = { savedNote = it }
        )

        clickSave()

        composeRule.runOnIdle {
            val note = checkNotNull(savedNote)
            assertEquals("", note.content)
            assertEquals(NoteCategory.SHOPPING, note.category)
            assertEquals(listOf(ChecklistItem(text = "Milk")), note.checklist)
            assertEquals(null, note.deadlineMillis)
            assertEquals(false, note.isRepeating)
        }
    }

    private fun setEditNoteContent(
        note: Note?,
        onSaveClick: (Note) -> Unit = {},
        onDeleteClick: (() -> Unit)? = null
    ) {
        composeRule.setContent {
            MaterialTheme {
                EditNoteScreen(
                    note = note,
                    onSaveClick = onSaveClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }

    private fun clickSave() {
        composeRule.onNodeWithTag(EDIT_NOTE_SAVE_BUTTON_TEST_TAG).performScrollTo().performClick()
    }

    private companion object {
        const val DEADLINE = 1_700_000_000_000L
    }
}
