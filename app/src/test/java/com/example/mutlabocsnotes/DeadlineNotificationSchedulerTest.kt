package com.example.mutlabocsnotes

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
        isCompleted: Boolean = false
    ): Note {
        return Note(
            id = id,
            title = "Task",
            category = category,
            deadlineMillis = deadlineMillis,
            isCompleted = isCompleted
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
}
