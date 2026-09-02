package com.abu.zikr.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abu.zikr.data.ZikrData
import com.abu.zikr.notification.NotificationHelper
import com.abu.zikr.settings.Settings
import com.abu.zikr.speech.Speech
import com.abu.zikr.update.UpdateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the exact alarm fired by AlarmScheduler: shows/speaks a
 * random zikr, then reschedules the next one. This is the auto-fire
 * path - manual "Test Zikr" bypasses it entirely and calls
 * NotificationHelper/Speech directly, same bypass-everything
 * convention as every other platform.
 *
 * Uses goAsync() since BroadcastReceiver.onReceive() must return
 * quickly, but showing a notification plus a short TTS utterance needs
 * a little more time than that (goAsync grants roughly 10s).
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = Settings(context)
                val snapshot = settings.snapshot()

                // Piggybacks on this alarm wake rather than adding a
                // separate schedule - gated to once a day internally.
                UpdateFlow.maybeCheckForUpdate(context)

                if (snapshot.enabled) {
                    val zikr = ZikrData.random(context)
                    NotificationHelper.show(context, zikr)
                    if (snapshot.speakAloud) {
                        Speech.speak(context, zikr, snapshot.selectedVoiceName)
                    }
                    AlarmScheduler(context).scheduleNext(snapshot.minIntervalMinutes, snapshot.maxIntervalMinutes)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
