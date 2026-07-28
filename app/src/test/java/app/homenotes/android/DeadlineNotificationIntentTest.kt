package app.homenotes.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeadlineNotificationIntentTest {

    @Test
    fun noteIdFromIntentParts_prefersNonBlankExtraId() {
        val noteId = DeadlineNotification.noteIdFromIntentParts(
            action = DeadlineNotification.ACTION_OPEN_NOTE,
            extraNoteId = "from-extra",
            dataScheme = "homenotes",
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
            dataScheme = "homenotes",
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
                dataScheme = "homenotes",
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
                dataScheme = "homenotes",
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
                category = NoteCategory.RECURRING_TASKS,
                startAtMillis = DEADLINE - 60 * 60_000L,
                durationMinutes = 60,
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
