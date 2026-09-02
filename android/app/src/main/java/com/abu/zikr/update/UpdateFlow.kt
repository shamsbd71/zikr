package com.abu.zikr.update

import android.content.Context
import com.abu.zikr.BuildConfig
import com.abu.zikr.notification.NotificationHelper
import com.abu.zikr.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CHECK_INTERVAL_MILLIS = 24L * 60 * 60 * 1000

/**
 * Decides *when* to check, on top of UpdateChecker's plain
 * check-only primitive - mirrors UpdateFlow.swift's separation of
 * concerns. The passive path (piggybacked on the reminder alarm, so no
 * extra scheduling infrastructure) is gated to once a day; a manual
 * check (Settings button, or on app launch) always runs.
 */
object UpdateFlow {
    /** Called from ReminderAlarmReceiver - silent no-op unless a whole
     * day has actually passed, so this doesn't hit the GitHub API on
     * every single reminder. */
    suspend fun maybeCheckForUpdate(context: Context) {
        val settings = Settings(context)
        val now = System.currentTimeMillis()
        if (now - settings.lastUpdateCheckAt() < CHECK_INTERVAL_MILLIS) return
        settings.setLastUpdateCheckAt(now)
        checkAndNotify(context)
    }

    /** Called from MainActivity on launch and Settings' "Check for
     * Updates" button - always runs, returns whether an update was
     * found so the caller can show its own feedback (e.g. "You're up
     * to date"). */
    suspend fun checkNow(context: Context): UpdateCheckResult {
        Settings(context).setLastUpdateCheckAt(System.currentTimeMillis())
        return checkAndNotify(context)
    }

    private suspend fun checkAndNotify(context: Context): UpdateCheckResult {
        val result = withContext(Dispatchers.IO) {
            UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
        }
        if (result is UpdateCheckResult.Available) {
            NotificationHelper.showUpdateAvailable(context, result.info)
        }
        return result
    }
}
