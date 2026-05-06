package com.example.mutlabocsnotes
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
    const val ACTION_OPEN_NOTE = "com.example.mutlabocsnotes.action.OPEN_DEADLINE_NOTE"

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
        if (intent?.action != ACTION_OPEN_NOTE) return null
        val extraNoteId = intent.getStringExtra(EXTRA_NOTE_ID)?.takeIf { it.isNotBlank() }
        if (extraNoteId != null) return extraNoteId

        val data = intent.data ?: return null
        if (data.scheme != NOTE_URI_SCHEME || data.host != NOTE_URI_HOST) return null
        val pathSegments = data.pathSegments
        return pathSegments
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

    private const val NOTE_URI_SCHEME = "mutlabocnotes"
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
