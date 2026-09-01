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

private const val CHANNEL_ID = "zikr_reminders"
private const val MUTE_REQUEST_CODE = 9001

object NotificationHelper {
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Zikr reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "A reminder phrase spoken throughout the day"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun show(context: Context, zikr: ZikrItem) {
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(zikr.transliteration)
            .setContentText(zikr.translation)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${zikr.arabic}\n${zikr.translation}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_notification, "Disable Sound", mutePendingIntent(context))
            .build()

        NotificationManagerCompat.from(context).notify(zikr.id, notification)
    }

    private fun mutePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MuteActionReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, MUTE_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
