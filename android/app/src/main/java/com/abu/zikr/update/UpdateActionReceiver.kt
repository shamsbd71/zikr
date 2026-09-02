package com.abu.zikr.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "ZikrUpdateAction"
private const val ACTION_DOWNLOAD_INSTALL = "com.abu.zikr.action.DOWNLOAD_INSTALL"
private const val EXTRA_APK_URL = "apk_url"

/** Handles the "Download & Install" action on the update-available
 * notification. */
class UpdateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action}")
        if (intent.action != ACTION_DOWNLOAD_INSTALL) return
        val apkUrl = intent.getStringExtra(EXTRA_APK_URL) ?: return
        Log.d(TAG, "enqueueing download for $apkUrl")
        val id = UpdateInstaller.enqueueDownload(context, apkUrl)
        Log.d(TAG, "enqueued download id=$id")
    }

    companion object {
        fun downloadIntent(context: Context, apkUrl: String): Intent =
            Intent(context, UpdateActionReceiver::class.java)
                .setAction(ACTION_DOWNLOAD_INSTALL)
                .putExtra(EXTRA_APK_URL, apkUrl)
    }
}
