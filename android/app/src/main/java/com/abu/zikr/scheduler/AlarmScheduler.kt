package com.abu.zikr.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.abu.zikr.settings.Settings
import kotlin.random.Random

private const val REQUEST_CODE = 1001

/**
 * Pure function so the random-interval math is unit-testable without
 * AlarmManager. Mirrors pick_delay_seconds (Linux) / PickDelaySeconds
 * (Windows) / the equivalent inline math in ReminderScheduler.swift.
 */
fun pickDelaySeconds(minIntervalMinutes: Int, maxIntervalMinutes: Int, random: Random = Random.Default): Long {
    val lo = maxOf(1, minIntervalMinutes) * 60L
    val hi = maxOf(lo, maxIntervalMinutes * 60L)
    return if (lo == hi) lo else random.nextLong(lo, hi + 1)
}

/**
 * Drives the app via AlarmManager.setExactAndAllowWhileIdle rather than
 * a plain WorkManager OneTimeWorkRequest (the original design). That
 * was a real bug, not a hypothetical one: testing on a real emulator
 * showed a WorkManager job can sit "READY" - all constraints satisfied,
 * overdue - and simply never get dispatched once the app's process is
 * frozen in the background. That's exactly the "no zikr in duration"
 * behavior reported after installing on a real phone. Exact alarms are
 * the standard mechanism every alarm/reminder app uses for this,
 * specifically because they're allowed to wake an idle/Doze'd device -
 * ordinary background jobs aren't.
 */
class AlarmScheduler(private val context: Context) {
    private val settings = Settings(context)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private val pendingIntent: PendingIntent
        get() {
            val intent = Intent(context, ReminderAlarmReceiver::class.java)
            return PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

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
        val triggerAtMillis = System.currentTimeMillis() + delaySeconds * 1000

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // Falls back to an inexact-but-Doze-aware alarm if the user
            // hasn't granted "Alarms & reminders" - still far more
            // reliable than a plain WorkManager job, just not
            // to-the-minute precise.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent)
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
