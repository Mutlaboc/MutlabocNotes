package com.example.mutlabocsnotes
import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat

// Объект-одиночка с общей логикой функциональности.
object DeadlineNotification {
    const val CHANNEL_ID = "deadline_notification"
    const val EXTRA_NOTE_ID = "extra_note_id"
    const val EXTRA_NOTE_TITLE = "extra_note_title"

    // Формирует стабильный request code уведомления по id заметки.
    fun requestCodeForId(noteId: String): Int {
        if (noteId.isBlank()) return 0
        val hash = noteId.hashCode()
        return if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash)
    }
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

        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(activityIntent)
            getPendingIntent(
                DeadlineNotification.requestCodeForId(noteId),
                PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            )
        }

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
