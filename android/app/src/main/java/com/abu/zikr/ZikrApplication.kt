package com.abu.zikr

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import com.abu.zikr.notification.NotificationHelper
import com.abu.zikr.scheduler.AlarmScheduler
import com.abu.zikr.unlock.UnlockReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ZikrApplication : Application() {
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        registerReceiver(UnlockReceiver(), IntentFilter(Intent.ACTION_USER_PRESENT))
        scope.launch {
            AlarmScheduler(this@ZikrApplication).start()
        }
    }
}
