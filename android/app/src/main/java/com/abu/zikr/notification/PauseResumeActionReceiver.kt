package com.abu.zikr.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abu.zikr.scheduler.AlarmScheduler
import com.abu.zikr.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val ACTION_PAUSE = "com.abu.zikr.action.PAUSE"
private const val ACTION_RESUME = "com.abu.zikr.action.RESUME"
private const val EXTRA_NOTIFICATION_ID = "notification_id"

/**
 * Handles the "Pause"/"Resume" action buttons on the reminder
 * notification - lets reminders be paused and resumed directly from
 * the notification, like a music player's play/pause, without opening
 * the app. Pausing swaps the reminder notification for a small
 * "paused, tap Resume" card rather than leaving anything persistent
 * up - unlike a real music player's now-playing bar, this only exists
 * while actually paused.
 */
class PauseResumeActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = Settings(context)
                val scheduler = AlarmScheduler(context)
                when (intent.action) {
                    ACTION_PAUSE -> {
                        settings.setEnabled(false)
                        scheduler.cancel()
                        val dismissedId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1).takeIf { it != -1 }
                        NotificationHelper.showPaused(context, dismissedId)
                    }
                    ACTION_RESUME -> {
                        settings.setEnabled(true)
                        scheduler.start()
                        NotificationHelper.clearPaused(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun pauseIntent(context: Context, notificationId: Int): Intent =
            Intent(context, PauseResumeActionReceiver::class.java)
                .setAction(ACTION_PAUSE)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)

        fun resumeIntent(context: Context): Intent =
            Intent(context, PauseResumeActionReceiver::class.java).setAction(ACTION_RESUME)
    }
}
