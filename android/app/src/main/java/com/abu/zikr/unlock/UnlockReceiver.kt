package com.abu.zikr.unlock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abu.zikr.data.ZikrData
import com.abu.zikr.notification.NotificationHelper
import com.abu.zikr.settings.Settings
import com.abu.zikr.speech.Speech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Says Bismillah once each time the screen is unlocked, mirroring
 * UnlockGreeter.swift on macOS.
 *
 * ACTION_USER_PRESENT can only be received via a dynamically registered
 * receiver - Android has forbidden declaring it (and most other
 * implicit system broadcasts) in the manifest since API 26 - so this is
 * registered in ZikrApplication.onCreate() rather than as a <receiver>
 * entry. That means it only fires while the app's process is alive: not
 * a hard guarantee the way the AlarmManager-driven reminders are, since
 * there's no persistent-foreground-service-free way to keep a process
 * permanently alive on Android. In practice the periodic reminder alarm
 * itself keeps waking the process every so often, which keeps this
 * reasonably fresh too.
 */
class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = Settings(context)
                val snapshot = settings.snapshot()
                if (snapshot.bismillahOnUnlock) {
                    val zikr = ZikrData.bismillah(context)
                    NotificationHelper.show(context, zikr)
                    if (snapshot.speakAloud) {
                        Speech.speak(context, zikr, snapshot.selectedVoiceName)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
