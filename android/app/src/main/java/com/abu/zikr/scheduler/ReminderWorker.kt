package com.abu.zikr.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abu.zikr.data.ZikrData
import com.abu.zikr.notification.NotificationHelper
import com.abu.zikr.settings.Settings
import com.abu.zikr.speech.Speech

/**
 * The auto-fire path: picks a random zikr, shows it, speaks it if
 * enabled, then reschedules the next one. Manual "Test Zikr" from the
 * UI bypasses this entirely and calls NotificationHelper/Speech
 * directly, same bypass-everything convention as every other platform.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        val snapshot = settings.snapshot()
        if (!snapshot.enabled) return Result.success()

        val zikr = ZikrData.random(applicationContext)
        NotificationHelper.show(applicationContext, zikr)
        if (snapshot.speakAloud) {
            Speech.speak(applicationContext, zikr)
        }

        ReminderScheduler(applicationContext)
            .scheduleNext(snapshot.minIntervalMinutes, snapshot.maxIntervalMinutes)
        return Result.success()
    }
}
