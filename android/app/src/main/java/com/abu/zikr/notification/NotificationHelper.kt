package com.abu.zikr.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.abu.zikr.R
import com.abu.zikr.data.ZikrItem
import com.abu.zikr.update.UpdateActionReceiver
import com.abu.zikr.update.UpdateInfo

// "_v2": Android locks a channel's sound once created, so an app can
// never silence an existing channel by re-creating it with the same
// id - a new id is the only way to actually change this for installs
// that already have the old, noisy "zikr_reminders" channel. The old
// one is deleted in ensureChannel() as cleanup.
private const val CHANNEL_ID = "zikr_reminders_v2"
private const val LEGACY_CHANNEL_ID = "zikr_reminders"
private const val MUTE_REQUEST_CODE = 9001
private const val RESUME_REQUEST_CODE = 9002
private const val PAUSE_REQUEST_CODE_BASE = 9100
private const val UPDATE_REQUEST_CODE = 9003

/** A fixed id for the "paused" control card, distinct from any zikr's
 * own id (1-22) so it never collides with or gets replaced by a real
 * reminder notification. */
const val CONTROL_NOTIFICATION_ID = 999999

/** A fixed id for the "update available" card, same reasoning. */
const val UPDATE_NOTIFICATION_ID = 999998

object NotificationHelper {
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Zikr reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "A reminder phrase spoken throughout the day"
            // No channel sound: the spoken zikr itself (or silence, if
            // "Disable Sound" is on) is the audio - a separate chime
            // playing first, before speech even starts, was the
            // "notification pop before the zikr" bug.
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, zikr: ZikrItem) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(zikr.transliteration)
            .setContentText(zikr.translation)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${zikr.arabic}\n${zikr.translation}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_notification, "Pause", pausePendingIntent(context, zikr.id))
            .addAction(R.drawable.ic_notification, "Disable Sound", mutePendingIntent(context))
            .build()

        NotificationManagerCompat.from(context).notify(zikr.id, notification)
    }

    /**
     * Swaps the just-shown reminder for a small "paused" control card
     * with a Resume action - the music-player pause/resume idiom,
     * without a persistent foreground-service notification: this only
     * appears while actually paused, not continuously the way a music
     * player's now-playing card does.
     */
    fun showPaused(context: Context, dismissedNotificationId: Int?) {
        ensureChannel(context)
        dismissedNotificationId?.let { NotificationManagerCompat.from(context).cancel(it) }
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zikr reminders paused")
            .setContentText("Tap Resume to start reminding you again.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification, "Resume", resumePendingIntent(context))
            .build()

        NotificationManagerCompat.from(context).notify(CONTROL_NOTIFICATION_ID, notification)
    }

    fun clearPaused(context: Context) {
        NotificationManagerCompat.from(context).cancel(CONTROL_NOTIFICATION_ID)
    }

    /** "Download & Install" still requires the user to confirm the
     * actual system install prompt - Android never skips that for a
     * sideloaded app - but this is the one-tap trigger for it. */
    fun showUpdateAvailable(context: Context, info: UpdateInfo) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zikr ${info.version} is available")
            .setContentText("Tap Download & Install to update.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_notification, "Download & Install", downloadInstallPendingIntent(context, info.apkDownloadUrl))
            .build()

        NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun mutePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MuteActionReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, MUTE_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pausePendingIntent(context: Context, notificationId: Int): PendingIntent {
        val intent = PauseResumeActionReceiver.pauseIntent(context, notificationId)
        return PendingIntent.getBroadcast(
            context, PAUSE_REQUEST_CODE_BASE + notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun resumePendingIntent(context: Context): PendingIntent {
        val intent = PauseResumeActionReceiver.resumeIntent(context)
        return PendingIntent.getBroadcast(
            context, RESUME_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun downloadInstallPendingIntent(context: Context, apkUrl: String): PendingIntent {
        val intent = UpdateActionReceiver.downloadIntent(context, apkUrl)
        return PendingIntent.getBroadcast(
            context, UPDATE_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
