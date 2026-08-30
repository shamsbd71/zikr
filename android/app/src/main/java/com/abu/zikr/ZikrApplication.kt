package com.abu.zikr

import android.app.Application
import com.abu.zikr.notification.NotificationHelper
import com.abu.zikr.scheduler.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ZikrApplication : Application() {
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        scope.launch {
            ReminderScheduler(this@ZikrApplication).start()
        }
    }
}
