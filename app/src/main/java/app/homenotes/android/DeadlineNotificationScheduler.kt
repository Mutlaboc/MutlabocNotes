package app.homenotes.android

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Calendar

data class DeadlineScheduleResult(
    val scheduled: Boolean,
    val exactAlarmPermissionRequired: Boolean
) {
    companion object {
        val NotScheduled = DeadlineScheduleResult(
            scheduled = false,
            exactAlarmPermissionRequired = false
        )
        val ScheduledExact = DeadlineScheduleResult(
            scheduled = true,
            exactAlarmPermissionRequired = false
        )
        val ScheduledInexactPermissionRequired = DeadlineScheduleResult(
            scheduled = true,
            exactAlarmPermissionRequired = true
        )

        fun aggregate(results: List<DeadlineScheduleResult>): DeadlineScheduleResult {
            return DeadlineScheduleResult(
                scheduled = results.any { it.scheduled },
                exactAlarmPermissionRequired = results.any { it.exactAlarmPermissionRequired }
            )
        }
    }
}

interface DeadlineScheduler {
    fun schedule(note: Note): DeadlineScheduleResult
    fun cancel(noteId: String)
    fun scheduleAll(notes: List<Note>): DeadlineScheduleResult
    fun cancelAll()
}

class DeadlineNotificationScheduler internal constructor(
    private val alarmBackend: DeadlineAlarmBackend,
    private val nowProvider: () -> Long = System::currentTimeMillis
) : DeadlineScheduler {

    constructor(context: Context) : this(AndroidDeadlineAlarmBackend(context))

    override fun schedule(note: Note): DeadlineScheduleResult {
        if (!alarmBackend.isAvailable || note.id.isBlank()) {
            return DeadlineScheduleResult.NotScheduled
        }

        cancel(note.id)

        if (note.category != NoteCategory.TASKS) return DeadlineScheduleResult.NotScheduled
        val deadlineMillis = note.deadlineMillis ?: return DeadlineScheduleResult.NotScheduled
        if (note.isCompleted) return DeadlineScheduleResult.NotScheduled

        val triggerAtMillis = nextDeadlineTriggerMillis(
            deadlineMillis = deadlineMillis,
            nowMillis = nowProvider(),
            repeatRule = note.repeatRule
        ) ?: return DeadlineScheduleResult.NotScheduled
        if (requiresExactAlarmPermission() && !alarmBackend.canScheduleExactAlarms()) {
            alarmBackend.scheduleInexact(triggerAtMillis, note)
            return DeadlineScheduleResult.ScheduledInexactPermissionRequired
        }

        return try {
            alarmBackend.scheduleExact(triggerAtMillis, note)
            DeadlineScheduleResult.ScheduledExact
        } catch (error: SecurityException) {
            alarmBackend.scheduleInexact(triggerAtMillis, note)
            DeadlineScheduleResult.ScheduledInexactPermissionRequired
        }
    }

    override fun cancel(noteId: String) {
        if (!alarmBackend.isAvailable || noteId.isBlank()) return
        alarmBackend.cancel(noteId)
    }

    override fun scheduleAll(notes: List<Note>): DeadlineScheduleResult {
        cancelAll()
        return DeadlineScheduleResult.aggregate(notes.map { schedule(it) })
    }

    override fun cancelAll() {
        if (!alarmBackend.isAvailable) return
        alarmBackend.cancelAll()
    }

    private fun requiresExactAlarmPermission(): Boolean {
        return alarmBackend.sdkInt >= Build.VERSION_CODES.S
    }
}

internal fun nextDeadlineTriggerMillis(
    deadlineMillis: Long,
    nowMillis: Long,
    repeatRule: RepeatRule
): Long? {
    val triggerCalendar = Calendar.getInstance().apply {
        timeInMillis = deadlineMillis
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (triggerCalendar.timeInMillis <= nowMillis) {
        when (repeatRule) {
            RepeatRule.NONE -> return null
            RepeatRule.DAILY -> {
                while (triggerCalendar.timeInMillis <= nowMillis) {
                    triggerCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            RepeatRule.WEEKLY -> {
                while (triggerCalendar.timeInMillis <= nowMillis) {
                    triggerCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            RepeatRule.MONTHLY -> return nextMonthlyDeadlineTriggerMillis(
                baseTriggerCalendar = triggerCalendar,
                nowMillis = nowMillis
            )
        }
    }
    return triggerCalendar.timeInMillis
}

private fun nextMonthlyDeadlineTriggerMillis(
    baseTriggerCalendar: Calendar,
    nowMillis: Long
): Long {
    val anchorDay = baseTriggerCalendar.get(Calendar.DAY_OF_MONTH)
    var year = baseTriggerCalendar.get(Calendar.YEAR)
    var month = baseTriggerCalendar.get(Calendar.MONTH)

    while (true) {
        val candidate = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, minOf(anchorDay, getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        if (candidate.timeInMillis > nowMillis) {
            return candidate.timeInMillis
        }
        month += 1
        if (month > Calendar.DECEMBER) {
            month = Calendar.JANUARY
            year += 1
        }
    }
}

internal interface DeadlineAlarmBackend {
    val sdkInt: Int
    val isAvailable: Boolean
    fun canScheduleExactAlarms(): Boolean
    fun scheduleExact(triggerAtMillis: Long, note: Note)
    fun scheduleInexact(triggerAtMillis: Long, note: Note)
    fun cancel(noteId: String)
    fun cancelAll()
}

private class AndroidDeadlineAlarmBackend(
    private val context: Context
) : DeadlineAlarmBackend {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override val sdkInt: Int = Build.VERSION.SDK_INT
    override val isAvailable: Boolean = alarmManager != null

    @SuppressLint("NewApi")
    override fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        if (sdkInt < Build.VERSION_CODES.S) return true
        return canScheduleExactAlarmsOnAndroidS(manager)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun canScheduleExactAlarmsOnAndroidS(manager: AlarmManager): Boolean {
        return manager.canScheduleExactAlarms()
    }

    override fun scheduleExact(triggerAtMillis: Long, note: Note) {
        val manager = alarmManager ?: return
        val pendingIntent = createPendingIntent(note, PendingIntent.FLAG_UPDATE_CURRENT)
        if (sdkInt >= Build.VERSION_CODES.M) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            manager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        rememberScheduledNote(note.id)
    }

    override fun scheduleInexact(triggerAtMillis: Long, note: Note) {
        val manager = alarmManager ?: return
        val pendingIntent = createPendingIntent(note, PendingIntent.FLAG_UPDATE_CURRENT)
        if (sdkInt >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            manager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        rememberScheduledNote(note.id)
    }

    override fun cancel(noteId: String) {
        cancelAlarm(noteId)
        forgetScheduledNote(noteId)
    }

    override fun cancelAll() {
        scheduledNoteIds().forEach { noteId ->
            cancelAlarm(noteId)
        }
        scheduledAlarmPrefs.edit()
            .remove(KEY_SCHEDULED_NOTE_IDS)
            .apply()
    }

    private fun cancelAlarm(noteId: String) {
        val manager = alarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DeadlineNotification.requestCodeForId(noteId),
            Intent(context, DeadlineNotificationReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or immutableFlag()
        )
        if (pendingIntent != null) {
            manager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private val scheduledAlarmPrefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun scheduledNoteIds(): Set<String> {
        return scheduledAlarmPrefs.getStringSet(KEY_SCHEDULED_NOTE_IDS, emptySet()).orEmpty()
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun rememberScheduledNote(noteId: String) {
        if (noteId.isBlank()) return
        val updatedIds = scheduledNoteIds() + noteId
        scheduledAlarmPrefs.edit()
            .putStringSet(KEY_SCHEDULED_NOTE_IDS, updatedIds)
            .apply()
    }

    private fun forgetScheduledNote(noteId: String) {
        if (noteId.isBlank()) return
        val updatedIds = scheduledNoteIds() - noteId
        scheduledAlarmPrefs.edit()
            .putStringSet(KEY_SCHEDULED_NOTE_IDS, updatedIds)
            .apply()
    }

    private fun createPendingIntent(note: Note, flags: Int): PendingIntent {
        val intent = Intent(context, DeadlineNotificationReceiver::class.java).apply {
            putExtra(DeadlineNotification.EXTRA_NOTE_ID, note.id)
            putExtra(DeadlineNotification.EXTRA_NOTE_TITLE, note.title)
            putExtra(DeadlineNotification.EXTRA_DEADLINE_MILLIS, note.deadlineMillis ?: 0L)
            putExtra(DeadlineNotification.EXTRA_REPEAT_RULE, note.repeatRule.name)
        }
        return PendingIntent.getBroadcast(
            context,
            DeadlineNotification.requestCodeForId(note.id),
            intent,
            flags or immutableFlag()
        )
    }

    private fun immutableFlag(): Int {
        return if (sdkInt >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }

    private companion object {
        const val PREFS_NAME = "deadline_notification_alarms"
        const val KEY_SCHEDULED_NOTE_IDS = "scheduled_note_ids"
    }
}
