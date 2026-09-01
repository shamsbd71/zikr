package com.abu.zikr.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abu.zikr.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the "Disable Sound" action button on the zikr notification -
 * turns off "Speak zikr aloud" without needing to open the app.
 */
class MuteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Settings(context).setSpeakAloud(false)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
