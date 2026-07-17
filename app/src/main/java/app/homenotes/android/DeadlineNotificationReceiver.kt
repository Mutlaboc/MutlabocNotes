package app.homenotes.android
import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

// Объект-одиночка с общей логикой функциональности.
object DeadlineNotification {
    const val CHANNEL_ID = "deadline_notification"
    const val EXTRA_NOTE_ID = "extra_note_id"
    const val EXTRA_NOTE_TITLE = "extra_note_title"
    const val EXTRA_DEADLINE_MILLIS = "extra_deadline_millis"
    const val EXTRA_REPEAT_RULE = "extra_repeat_rule"
    const val ACTION_OPEN_NOTE = "app.homenotes.android.action.OPEN_DEADLINE_NOTE"

    // Формирует стабильный request code уведомления по id заметки.
    fun requestCodeForId(noteId: String): Int {
        if (noteId.isBlank()) return 0
        val hash = noteId.hashCode()
        return if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash)
    }

    fun openNoteIntent(context: Context, noteId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_NOTE
            data = openNoteUri(noteId)
            putExtra(EXTRA_NOTE_ID, noteId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }

    fun noteIdFromIntent(intent: Intent?): String? {
        val data = intent?.data
        return noteIdFromIntentParts(
            action = intent?.action,
            extraNoteId = intent?.getStringExtra(EXTRA_NOTE_ID),
            dataScheme = data?.scheme,
            dataHost = data?.host,
            dataPathSegments = data?.pathSegments.orEmpty()
        )
    }

    internal fun noteIdFromIntentParts(
        action: String?,
        extraNoteId: String?,
        dataScheme: String?,
        dataHost: String?,
        dataPathSegments: List<String>
    ): String? {
        if (action != ACTION_OPEN_NOTE) return null
        extraNoteId?.takeIf { it.isNotBlank() }?.let { return it }
        if (dataScheme != NOTE_URI_SCHEME || dataHost != NOTE_URI_HOST) return null
        return dataPathSegments
            .takeIf { it.size == 2 && it[0] == NOTE_URI_NOTE_PATH }
            ?.get(1)
            ?.takeIf { it.isNotBlank() }
    }

    private fun openNoteUri(noteId: String): Uri {
        return Uri.Builder()
            .scheme(NOTE_URI_SCHEME)
            .authority(NOTE_URI_HOST)
            .appendPath(NOTE_URI_NOTE_PATH)
            .appendPath(noteId)
            .build()
    }

    private const val NOTE_URI_SCHEME = "homenotes"
    private const val NOTE_URI_HOST = "deadline"
    private const val NOTE_URI_NOTE_PATH = "note"
}
// BroadcastReceiver, реагирующий на системные события.
class DeadlineNotificationReceiver: BroadcastReceiver() {
    // Обрабатывает входящие broadcast-события и показывает уведомление.
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(DeadlineNotification.EXTRA_NOTE_ID).orEmpty()
        val noteTitle = intent.getStringExtra(DeadlineNotification.EXTRA_NOTE_TITLE)
        val contentText = context.getString(
            R.string.deadline_notification_message,
            noteTitle?.takeIf { it.isNotBlank() } ?: context.getString(R.string.app_name)
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            DeadlineNotification.requestCodeForId(noteId),
            DeadlineNotification.openNoteIntent(context, noteId),
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )

        val notification = NotificationCompat.Builder(context, DeadlineNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(contentText)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(
                DeadlineNotification.requestCodeForId(noteId),
                notification
            )
        }
    }


    // Добавляет флаг immutable на поддерживаемых версиях Android для безопасности PendingIntent.
    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

}
@Deprecated("Recurring notifications are now backed by separate task instances")
internal fun repeatingDeadlineNoteFromAlarm(
    noteId: String,
    noteTitle: String?,
    deadlineMillis: Long,
    repeatRule: RepeatRule
): Note? {
    if (noteId.isBlank() || repeatRule == RepeatRule.NONE || deadlineMillis <= 0L) return null
    return Note(
        id = noteId,
        title = noteTitle.orEmpty(),
        category = NoteCategory.RECURRING_TASKS,
        startAtMillis = deadlineMillis - 60 * 60_000L,
        durationMinutes = 60,
        repeatRule = repeatRule
    )
}
