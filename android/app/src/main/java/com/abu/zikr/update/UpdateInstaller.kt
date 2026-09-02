package com.abu.zikr.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

private const val APK_FILE_NAME = "zikr-update.apk"
private const val PREFS_NAME = "zikr_update"
private const val PREF_PENDING_DOWNLOAD_ID = "pending_download_id"

/**
 * Downloads the update APK via the system DownloadManager (native
 * progress notification, handles retry/pause for free, no extra
 * dependency) and hands it to the standard system install flow once
 * done. Android always requires the user to confirm the actual
 * install - including, the first time, an interstitial "allow Zikr to
 * install unknown apps?" toggle if not already granted - there's no
 * way around that, nor should there be.
 */
object UpdateInstaller {
    fun enqueueDownload(context: Context, apkUrl: String): Long {
        val destFile = File(context.getExternalFilesDir(null), APK_FILE_NAME)
        if (destFile.exists()) destFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Zikr update")
            .setDescription("Downloading the latest version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destFile))
            .setAllowedOverMetered(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = downloadManager.enqueue(request)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(PREF_PENDING_DOWNLOAD_ID, id)
            .apply()

        return id
    }

    fun isPendingDownload(context: Context, downloadId: Long): Boolean {
        val expected = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(PREF_PENDING_DOWNLOAD_ID, -1)
        return expected != -1L && expected == downloadId
    }

    fun promptInstall(context: Context) {
        val destFile = File(context.getExternalFilesDir(null), APK_FILE_NAME)
        if (!destFile.exists()) return

        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
