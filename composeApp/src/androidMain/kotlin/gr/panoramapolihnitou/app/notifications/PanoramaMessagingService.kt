package gr.panoramapolihnitou.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import gr.panoramapolihnitou.app.MainActivity
import gr.panoramapolihnitou.app.R

/**
 * Topic every install subscribes to at launch (see `PanoramaApplication.onCreate`).
 * Must match the topic name iOS subscribes to in AppDelegate.swift — no per-device
 * token registry, every install just listens for the same topic.
 */
const val NEW_ARTICLES_TOPIC = "new_articles"

/** Matches the manifest's `default_notification_channel_id` meta-data. */
const val NEW_ARTICLES_CHANNEL_ID = "new_articles"

private var notificationId = 1000

/**
 * Renders incoming FCM messages as a system notification tapping into the app.
 * FCM auto-displays "notification"-payload messages itself while the app is
 * backgrounded (using [NEW_ARTICLES_CHANNEL_ID] from the manifest meta-data);
 * this handler covers foreground delivery and data-only payloads so the result
 * looks the same either way.
 */
class PanoramaMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"]

        val intent = Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NEW_ARTICLES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching { NotificationManagerCompat.from(this).notify(notificationId++, notification) }
    }
}

/**
 * Registers [NEW_ARTICLES_CHANNEL_ID] up front so it exists before FCM ever needs
 * it — including for a "notification"-payload message the system displays itself
 * without going through [PanoramaMessagingService.onMessageReceived] at all.
 */
fun ensureNewArticlesChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (manager.getNotificationChannel(NEW_ARTICLES_CHANNEL_ID) != null) return
    manager.createNotificationChannel(
        android.app.NotificationChannel(
            NEW_ARTICLES_CHANNEL_ID,
            "Νέα άρθρα",
            NotificationManager.IMPORTANCE_DEFAULT
        )
    )
}
