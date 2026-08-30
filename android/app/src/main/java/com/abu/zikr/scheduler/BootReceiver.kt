package com.abu.zikr.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms the alarm after a reboot. Unlike WorkManager (which persists
 * its own schedule automatically), a raw AlarmManager alarm does not
 * survive a device restart on its own - this receiver is what replaces
 * that guarantee.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AlarmScheduler(context).start()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
