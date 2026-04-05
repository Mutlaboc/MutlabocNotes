import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.mutlabocsnotes.DeadlineNotification
import com.example.mutlabocsnotes.DeadlineNotificationReceiver
import com.example.mutlabocsnotes.Note
import com.example.mutlabocsnotes.NoteCategory
import java.util.Calendar

// Schedules and cancels background notification tasks.
class DeadlineNotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    // Schedules deadline notifications for note data.
    fun schedule(note: Note) {
        if (alarmManager == null || note.id.isBlank()) return

        cancel(note.id)

        if (note.category != NoteCategory.TASKS) return
        val deadlineMillis = note.deadlineMillis ?: return
        if (note.isCompleted) return

        val triggerCalendar = Calendar.getInstance().apply {
            timeInMillis = deadlineMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        if (triggerCalendar.timeInMillis <= now) {
            if (note.isRepeating) {
                while (triggerCalendar.timeInMillis <= now) {
                    triggerCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                return
            }
        }

        val intent = Intent(context, DeadlineNotificationReceiver::class.java).apply {
            putExtra(DeadlineNotification.EXTRA_NOTE_ID, note.id)
            putExtra(DeadlineNotification.EXTRA_NOTE_TITLE, note.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DeadlineNotification.requestCodeForId(note.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )

        val triggerAtMillis = triggerCalendar.timeInMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    // Cancels previously scheduled deadline notifications.
    fun cancel(noteId: String) {
        if (alarmManager == null || noteId.isBlank()) return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DeadlineNotification.requestCodeForId(noteId),
            Intent(context, DeadlineNotificationReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or immutableFlag()
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    // Schedules deadline notifications for note data.
    fun scheduleAll(notes: List<Note>) {
        notes.forEach { schedule(it) }
    }

    // Adds immutable flag on supported Android versions for PendingIntent safety.
    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
