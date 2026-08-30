package com.abu.zikr.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.abu.zikr.settings.Settings
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val WORK_NAME = "zikr-reminder"

/**
 * Pure function so the random-interval math is unit-testable without
 * WorkManager. Mirrors pick_delay_seconds (Linux) / PickDelaySeconds
 * (Windows) / the equivalent inline math in ReminderScheduler.swift.
 */
fun pickDelaySeconds(minIntervalMinutes: Int, maxIntervalMinutes: Int, random: Random = Random.Default): Long {
    val lo = maxOf(1, minIntervalMinutes) * 60L
    val hi = maxOf(lo, maxIntervalMinutes * 60L)
    return if (lo == hi) lo else random.nextLong(lo, hi + 1)
}

/**
 * Drives the app: a single chained WorkManager job that fires at a
 * random interval, shows one random zikr (ReminderWorker), then
 * reschedules itself. WorkManager persists its own schedule and
 * re-arms automatically after a device reboot, so no boot receiver is
 * needed here (unlike a raw AlarmManager approach).
 */
class ReminderScheduler(private val context: Context) {
    private val settings = Settings(context)

    suspend fun start() {
        val snapshot = settings.snapshot()
        if (snapshot.enabled) {
            scheduleNext(snapshot.minIntervalMinutes, snapshot.maxIntervalMinutes)
        } else {
            cancel()
        }
    }

    suspend fun onSettingsChanged() = start()

    fun scheduleNext(minIntervalMinutes: Int, maxIntervalMinutes: Int) {
        val delaySeconds = pickDelaySeconds(minIntervalMinutes, maxIntervalMinutes)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
