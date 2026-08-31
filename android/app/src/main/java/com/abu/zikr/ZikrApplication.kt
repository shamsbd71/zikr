package com.abu.zikr

import android.app.Application
import com.abu.zikr.notification.NotificationHelper
import com.abu.zikr.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ZikrApplication : Application() {
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        scope.launch {
            AlarmScheduler(this@ZikrApplication).start()
        }
    }
}
