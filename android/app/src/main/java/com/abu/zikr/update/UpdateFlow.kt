package com.abu.zikr.update

import android.content.Context
import com.abu.zikr.BuildConfig
import com.abu.zikr.notification.NotificationHelper
import com.abu.zikr.settings.Settings
import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CHECK_INTERVAL_MILLIS = 24L * 60 * 60 * 1000

/**
 * Decides *when* to check, on top of UpdateChecker's plain check-only
 * primitive - mirrors UpdateFlow.swift's separation of concerns.
 *
 * Every check feeds UpdateState, which drives the banner at the top of
 * the app. Only the passive background check (piggybacked on the
 * reminder alarm, once a day) also posts a system notification - and
 * tapping that just opens the app on the banner, where the update
 * actually happens.
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
        val result = check()
        if (result is UpdateCheckResult.Available) {
            NotificationHelper.showUpdateAvailable(context, result.info)
        }
    }

    /** Called from MainActivity on launch and the Settings "Check for
     * Updates" button. The app is in front, so the banner is enough -
     * no notification. */
    suspend fun checkNow(context: Context): UpdateCheckResult {
        Settings(context).setLastUpdateCheckAt(System.currentTimeMillis())
        return check()
    }

    private suspend fun check(): UpdateCheckResult {
        val result = withContext(Dispatchers.IO) {
            UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
        }
        when (result) {
            is UpdateCheckResult.Available -> UpdateState.offer(result.info)
            is UpdateCheckResult.UpToDate -> UpdateState.clear()
            is UpdateCheckResult.Error -> Unit
        }
        return result
    }

    /** Outlives any one screen, so rotating the phone or leaving the app
     * doesn't cancel a download that's half done. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Set when we sent the user to "Allow from this source"; the
     * activity finishes the install from onResume once it's granted. */
    @Volatile var awaitingInstallPermission = false
        private set

    /** The banner's "Update now": download in the app, then open the
     * system installer from the activity the user is looking at. */
    fun downloadAndInstall(activity: Activity, info: UpdateInfo) {
        val appContext = activity.applicationContext
        NotificationHelper.clearUpdateNotification(appContext)
        UpdateState.set(UpdateUiState.Downloading(info, -1))
        scope.launch {
            try {
                UpdateInstaller.download(appContext, info) { pct ->
                    UpdateState.set(UpdateUiState.Downloading(info, pct))
                }
                UpdateState.set(UpdateUiState.ReadyToInstall(info))
                install(activity, info)
            } catch (e: Exception) {
                UpdateState.set(UpdateUiState.Failed(info, "Download failed. Check your connection and try again."))
            }
        }
    }

    /** Opens the installer, asking for the one-time install permission
     * first if Android needs it. */
    fun install(activity: Activity, info: UpdateInfo) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!UpdateInstaller.canInstall(activity)) {
            awaitingInstallPermission = true
            UpdateInstaller.requestInstallPermission(activity)
            return
        }
        awaitingInstallPermission = false
        if (!UpdateInstaller.install(activity)) {
            UpdateState.set(UpdateUiState.Failed(info, "Couldn't open the installer. Try again."))
        }
    }

    /** From MainActivity.onResume: finish an install that was waiting on
     * the user to allow Zikr to install apps. */
    fun resumePendingInstall(activity: Activity) {
        if (!awaitingInstallPermission) return
        val state = UpdateState.state.value as? UpdateUiState.ReadyToInstall ?: return
        if (UpdateInstaller.canInstall(activity)) install(activity, state.info)
    }
}
