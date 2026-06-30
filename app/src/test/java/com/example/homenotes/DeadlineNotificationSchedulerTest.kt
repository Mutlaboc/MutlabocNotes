package com.example.homenotes

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DeadlineNotificationSchedulerTest {

    private val nowMillis = calendarMillis(2026, Calendar.JANUARY, 10, 12)
    private val futureDeadlineMillis = calendarMillis(2026, Calendar.JANUARY, 11, 12)
    private val pastDeadlineMillis = calendarMillis(2026, Calendar.JANUARY, 9, 12)

    @Test
    fun apiBelow31_schedulesExactWithoutCheckingCapability() {
        val backend = FakeDeadlineAlarmBackend(
            sdkInt = Build.VERSION_CODES.R,
            canScheduleExactAlarms = false
        )
        val scheduler = scheduler(backend)

        val result = scheduler.schedule(task(deadlineMillis = futureDeadlineMillis))

        assertEquals(DeadlineScheduleResult.ScheduledExact, result)
        assertEquals(0, backend.canScheduleExactAlarmChecks)
        assertEquals(listOf("task"), backend.cancelCalls)
        assertEquals(listOf("task"), backend.exactCalls.map { it.note.id })
        assertTrue(backend.inexactCalls.isEmpty())
    }

    @Test
    fun api31Plus_withCapabilityGranted_schedulesExact() {
        val backend = FakeDeadlineAlarmBackend(
            sdkInt = Build.VERSION_CODES.S,
            canScheduleExactAlarms = true
        )
        val scheduler = scheduler(backend)

        val result = scheduler.schedule(task(deadlineMillis = futureDeadlineMillis))

        assertEquals(DeadlineScheduleResult.ScheduledExact, result)
        assertEquals(1, backend.canScheduleExactAlarmChecks)
        assertEquals(listOf("task"), backend.exactCalls.map { it.note.id })
        assertTrue(backend.inexactCalls.isEmpty())
    }

    @Test
    fun api31Plus_withoutCapability_schedulesInexactAndReportsPermissionRequired() {
        val backend = FakeDeadlineAlarmBackend(
            sdkInt = Build.VERSION_CODES.S,
            canScheduleExactAlarms = false
        )
        val scheduler = scheduler(backend)

        val result = scheduler.schedule(task(deadlineMillis = futureDeadlineMillis))

        assertEquals(DeadlineScheduleResult.ScheduledInexactPermissionRequired, result)
        assertEquals(1, backend.canScheduleExactAlarmChecks)
        assertTrue(backend.exactCalls.isEmpty())
        assertEquals(listOf("task"), backend.inexactCalls.map { it.note.id })
    }

    @Test
    fun exactSecurityException_schedulesInexactAndReportsPermissionRequired() {
        val backend = FakeDeadlineAlarmBackend(
            sdkInt = Build.VERSION_CODES.S,
            canScheduleExactAlarms = true,
            throwOnExact = true
        )
        val scheduler = scheduler(backend)

        val result = scheduler.schedule(task(deadlineMillis = futureDeadlineMillis))

        assertEquals(DeadlineScheduleResult.ScheduledInexactPermissionRequired, result)
        assertEquals(listOf("task"), backend.inexactCalls.map { it.note.id })
    }

    @Test
    fun invalidOrInactiveNotesDoNotSchedule() {
        val backend = FakeDeadlineAlarmBackend()
        val scheduler = scheduler(backend)

        val blankIdResult = scheduler.schedule(task(id = "", deadlineMillis = futureDeadlineMillis))
        val noteResult = scheduler.schedule(
            task(id = "note", category = NoteCategory.NOTES, deadlineMillis = futureDeadlineMillis)
        )
        val completedResult = scheduler.schedule(
            task(id = "done", isCompleted = true, deadlineMillis = futureDeadlineMillis)
        )
        val noDeadlineResult = scheduler.schedule(task(id = "no-deadline", deadlineMillis = null))
        val pastResult = scheduler.schedule(task(id = "past", deadlineMillis = pastDeadlineMillis))

        assertFalse(blankIdResult.scheduled)
        assertFalse(noteResult.scheduled)
        assertFalse(completedResult.scheduled)
        assertFalse(noDeadlineResult.scheduled)
        assertFalse(pastResult.scheduled)
        assertTrue(backend.exactCalls.isEmpty())
        assertTrue(backend.inexactCalls.isEmpty())
    }

    @Test
    fun repeatingPastDeadlineRollsForwardToNextDailyNineAm() {
        val backend = FakeDeadlineAlarmBackend()
        val scheduler = scheduler(backend)

        val result = scheduler.schedule(
            task(
                deadlineMillis = pastDeadlineMillis,
                repeatRule = RepeatRule.DAILY
            )
        )

        assertEquals(DeadlineScheduleResult.ScheduledExact, result)
        assertEquals(
            calendarMillis(2026, Calendar.JANUARY, 11, 9),
            backend.exactCalls.single().triggerAtMillis
        )
    }

    @Test
    fun weeklyPastDeadlineRollsForwardToNextMatchingWeekdayNineAm() {
        val trigger = nextDeadlineTriggerMillis(
            deadlineMillis = calendarMillis(2026, Calendar.JANUARY, 9, 12),
            nowMillis = calendarMillis(2026, Calendar.JANUARY, 10, 12),
            repeatRule = RepeatRule.WEEKLY
        )

        assertEquals(calendarMillis(2026, Calendar.JANUARY, 16, 9), trigger)
    }

    @Test
    fun monthlyPastDeadlineUsesLastValidDayThenOriginalAnchorWhenPossible() {
        val februaryTrigger = nextDeadlineTriggerMillis(
            deadlineMillis = calendarMillis(2026, Calendar.JANUARY, 31, 12),
            nowMillis = calendarMillis(2026, Calendar.FEBRUARY, 1, 12),
            repeatRule = RepeatRule.MONTHLY
        )
        val marchTrigger = nextDeadlineTriggerMillis(
            deadlineMillis = calendarMillis(2026, Calendar.JANUARY, 31, 12),
            nowMillis = calendarMillis(2026, Calendar.MARCH, 1, 12),
            repeatRule = RepeatRule.MONTHLY
        )

        assertEquals(calendarMillis(2026, Calendar.FEBRUARY, 28, 9), februaryTrigger)
        assertEquals(calendarMillis(2026, Calendar.MARCH, 31, 9), marchTrigger)
    }

    @Test
    fun scheduleAllAggregatesExactAlarmPermissionWarning() {
        val backend = FakeDeadlineAlarmBackend(
            sdkInt = Build.VERSION_CODES.S,
            canScheduleExactAlarms = false
        )
        val scheduler = scheduler(backend)

        val result = scheduler.scheduleAll(
            listOf(
                task(id = "needs-permission", deadlineMillis = futureDeadlineMillis),
                task(id = "plain-note", category = NoteCategory.NOTES, deadlineMillis = futureDeadlineMillis)
            )
        )

        assertEquals(DeadlineScheduleResult.ScheduledInexactPermissionRequired, result)
        assertEquals(listOf("needs-permission"), backend.inexactCalls.map { it.note.id })
    }

    @Test
    fun scheduleAllCancelsExistingAlarmsBeforeReschedulingCurrentNotes() {
        val backend = FakeDeadlineAlarmBackend()
        val scheduler = scheduler(backend)
        val first = task(id = "first", deadlineMillis = futureDeadlineMillis)
        val second = task(id = "second", deadlineMillis = futureDeadlineMillis)

        val result = scheduler.scheduleAll(listOf(first, second))

        assertEquals(DeadlineScheduleResult.ScheduledExact, result)
        assertEquals(1, backend.cancelAllCalls)
        assertEquals(listOf("first", "second"), backend.exactCalls.map { it.note.id })
    }

    @Test
    fun cancelAllDelegatesToBackendWhenAvailable() {
        val backend = FakeDeadlineAlarmBackend()
        val scheduler = scheduler(backend)

        scheduler.cancelAll()

        assertEquals(1, backend.cancelAllCalls)
    }

    private fun scheduler(backend: FakeDeadlineAlarmBackend): DeadlineNotificationScheduler {
        return DeadlineNotificationScheduler(
            alarmBackend = backend,
            nowProvider = { nowMillis }
        )
    }

    private fun task(
        id: String = "task",
        category: NoteCategory = NoteCategory.TASKS,
        deadlineMillis: Long? = futureDeadlineMillis,
        isCompleted: Boolean = false,
        repeatRule: RepeatRule = RepeatRule.NONE
    ): Note {
        return Note(
            id = id,
            title = "Task",
            category = category,
            deadlineMillis = deadlineMillis,
            isCompleted = isCompleted,
            repeatRule = repeatRule
        )
    }

    private fun calendarMillis(year: Int, month: Int, day: Int, hour: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

private data class ScheduledAlarmCall(
    val triggerAtMillis: Long,
    val note: Note
)

private class FakeDeadlineAlarmBackend(
    override val sdkInt: Int = Build.VERSION_CODES.S,
    override val isAvailable: Boolean = true,
    private val canScheduleExactAlarms: Boolean = true,
    private val throwOnExact: Boolean = false
) : DeadlineAlarmBackend {
    var canScheduleExactAlarmChecks = 0
    val exactCalls = mutableListOf<ScheduledAlarmCall>()
    val inexactCalls = mutableListOf<ScheduledAlarmCall>()
    val cancelCalls = mutableListOf<String>()
    var cancelAllCalls = 0

    override fun canScheduleExactAlarms(): Boolean {
        canScheduleExactAlarmChecks += 1
        return canScheduleExactAlarms
    }

    override fun scheduleExact(triggerAtMillis: Long, note: Note) {
        if (throwOnExact) throw SecurityException("exact alarms denied")
        exactCalls.add(ScheduledAlarmCall(triggerAtMillis, note))
    }

    override fun scheduleInexact(triggerAtMillis: Long, note: Note) {
        inexactCalls.add(ScheduledAlarmCall(triggerAtMillis, note))
    }

    override fun cancel(noteId: String) {
        cancelCalls.add(noteId)
    }

    override fun cancelAll() {
        cancelAllCalls += 1
    }
}
