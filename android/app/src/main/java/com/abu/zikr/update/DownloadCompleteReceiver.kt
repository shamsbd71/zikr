package com.abu.zikr.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "ZikrDownloadComplete"

/**
 * Fires when any DownloadManager download completes system-wide, so it
 * must check the id matches the one UpdateInstaller started before
 * doing anything - otherwise this would try to install-prompt for
 * unrelated downloads (a browser download, another app's), which would
 * be a real bug, not just an annoyance.
 */
class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action}")
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        Log.d(TAG, "download id=$id pending=${UpdateInstaller.isPendingDownload(context, id)}")
        if (id == -1L || !UpdateInstaller.isPendingDownload(context, id)) return
        UpdateInstaller.promptInstall(context)
    }
}
