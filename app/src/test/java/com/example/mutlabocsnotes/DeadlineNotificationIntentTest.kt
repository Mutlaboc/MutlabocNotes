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
    fun repeatingDeadlineNoteFromAlarm_buildsTaskOnlyForValidRepeatRuleAlarm() {
        val note = repeatingDeadlineNoteFromAlarm(
            noteId = "task",
            noteTitle = "Task",
            deadlineMillis = DEADLINE,
            repeatRule = RepeatRule.WEEKLY
        )

        assertEquals(
            Note(
                id = "task",
                title = "Task",
                category = NoteCategory.TASKS,
                deadlineMillis = DEADLINE,
                repeatRule = RepeatRule.WEEKLY
            ),
            note
        )
        assertNull(
            repeatingDeadlineNoteFromAlarm(
                noteId = "task",
                noteTitle = "Task",
                deadlineMillis = DEADLINE,
                repeatRule = RepeatRule.NONE
            )
        )
        assertNull(
            repeatingDeadlineNoteFromAlarm(
                noteId = "",
                noteTitle = "Task",
                deadlineMillis = DEADLINE,
                repeatRule = RepeatRule.DAILY
            )
        )
        assertNull(
            repeatingDeadlineNoteFromAlarm(
                noteId = "task",
                noteTitle = "Task",
                deadlineMillis = 0L,
                repeatRule = RepeatRule.DAILY
            )
        )
    }

    private companion object {
        const val DEADLINE = 1_700_000_000_000L
    }
}
