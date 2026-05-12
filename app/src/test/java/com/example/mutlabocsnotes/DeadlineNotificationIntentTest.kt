package com.example.mutlabocsnotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeadlineNotificationIntentTest {

    @Test
    fun noteIdFromIntentParts_prefersNonBlankExtraId() {
        val noteId = DeadlineNotification.noteIdFromIntentParts(
            action = DeadlineNotification.ACTION_OPEN_NOTE,
            extraNoteId = "from-extra",
            dataScheme = "mutlabocnotes",
            dataHost = "deadline",
            dataPathSegments = listOf("note", "from-uri")
        )

        assertEquals("from-extra", noteId)
    }

    @Test
    fun noteIdFromIntentParts_readsUriWhenExtraIsBlank() {
        val noteId = DeadlineNotification.noteIdFromIntentParts(
            action = DeadlineNotification.ACTION_OPEN_NOTE,
            extraNoteId = "",
            dataScheme = "mutlabocnotes",
            dataHost = "deadline",
            dataPathSegments = listOf("note", "from-uri")
        )

        assertEquals("from-uri", noteId)
    }

    @Test
    fun noteIdFromIntentParts_rejectsWrongActionOrMalformedUri() {
        assertNull(
            DeadlineNotification.noteIdFromIntentParts(
                action = "wrong",
                extraNoteId = "note",
                dataScheme = "mutlabocnotes",
                dataHost = "deadline",
                dataPathSegments = listOf("note", "from-uri")
            )
        )
        assertNull(
            DeadlineNotification.noteIdFromIntentParts(
                action = DeadlineNotification.ACTION_OPEN_NOTE,
                extraNoteId = "",
                dataScheme = "https",
                dataHost = "deadline",
                dataPathSegments = listOf("note", "from-uri")
            )
        )
        assertNull(
            DeadlineNotification.noteIdFromIntentParts(
                action = DeadlineNotification.ACTION_OPEN_NOTE,
                extraNoteId = "",
                dataScheme = "mutlabocnotes",
                dataHost = "deadline",
                dataPathSegments = listOf("wrong", "from-uri")
            )
        )
    }

    @Test
    fun repeatingDeadlineNoteFromAlarm_buildsDailyTaskOnlyForValidRepeatingAlarm() {
        val note = repeatingDeadlineNoteFromAlarm(
            noteId = "task",
            noteTitle = "Task",
            deadlineMillis = DEADLINE,
            repeatsDaily = true
        )

        assertEquals(
            Note(
                id = "task",
                title = "Task",
                category = NoteCategory.TASKS,
                deadlineMillis = DEADLINE,
                isRepeating = true
            ),
            note
        )
        assertNull(
            repeatingDeadlineNoteFromAlarm(
                noteId = "task",
                noteTitle = "Task",
                deadlineMillis = DEADLINE,
                repeatsDaily = false
            )
        )
        assertNull(
            repeatingDeadlineNoteFromAlarm(
                noteId = "",
                noteTitle = "Task",
                deadlineMillis = DEADLINE,
                repeatsDaily = true
            )
        )
        assertNull(
            repeatingDeadlineNoteFromAlarm(
                noteId = "task",
                noteTitle = "Task",
                deadlineMillis = 0L,
                repeatsDaily = true
            )
        )
    }

    private companion object {
        const val DEADLINE = 1_700_000_000_000L
    }
}
