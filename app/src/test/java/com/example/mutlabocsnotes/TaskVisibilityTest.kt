package com.example.mutlabocsnotes

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskVisibilityTest {
    private val now = 1_000_000L

    @Test
    fun futureRecurringTasksAreOnlyUpcomingAndSorted() {
        val notes = listOf(
            Note(id = "purchase", category = NoteCategory.SHOPPING),
            Note(id = "task", category = NoteCategory.TASKS),
            recurring("active", now),
            recurring("later", now + 2_000),
            recurring("next", now + 1_000),
            recurring("done", now + 500).copy(isCompleted = true)
        )

        assertEquals(listOf("purchase", "task", "active"), homeNotes(notes, now).map { it.id })
        assertEquals(listOf("next", "later"), upcomingNotes(notes, now).map { it.id })
    }

    @Test
    fun taskMovesFromUpcomingToHomeAtStartTime() {
        val note = recurring("scheduled", now + 1)

        assertEquals(listOf("scheduled"), upcomingNotes(listOf(note), now).map { it.id })
        assertEquals(listOf("scheduled"), homeNotes(listOf(note), now + 1).map { it.id })
        assertEquals(emptyList<String>(), upcomingNotes(listOf(note), now + 1).map { it.id })
    }

    private fun recurring(id: String, start: Long) = Note(
        id = id,
        category = NoteCategory.RECURRING_TASKS,
        startAtMillis = start,
        durationMinutes = 60,
        repeatRule = RepeatRule.DAILY
    )
}
