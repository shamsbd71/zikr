package com.abu.zikr.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ZikrUpdateInstaller"
private const val APK_FILE_NAME = "zikr-update.apk"

/**
 * Downloads the update APK inside the app (with progress shown in the
 * in-app banner) and hands it to the system installer *from the
 * foreground activity*.
 *
 * Why not DownloadManager + a completion receiver, as before: Android 10+
 * blocks starting an activity from a background receiver, so the install
 * prompt never appeared once the download finished, and the update
 * silently went nowhere. Launching the installer from the activity the
 * user just tapped in is always allowed.
 *
 * Android still asks the user to confirm the install (and, the first
 * time, to allow Zikr to install apps). There is no way around that, nor
 * should there be.
 */
object UpdateInstaller {
    private fun apkFile(context: Context): File =
        File(File(context.cacheDir, "updates").apply { mkdirs() }, APK_FILE_NAME)

    /** Blocking download on Dispatchers.IO. Reports 0..100, or -1 when the
     * server doesn't send a length. Throws on failure. */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dest = apkFile(context)
            val partial = File(dest.parentFile, "$APK_FILE_NAME.part")
            var url = URL(info.apkDownloadUrl)
            // GitHub redirects asset downloads to a CDN host; follow it
            // by hand so a cross-host redirect can't be silently dropped.
            var connection: HttpURLConnection
            var redirects = 0
            while (true) {
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "Zikr-Android-Updater")
                val code = connection.responseCode
                if (code in 300..399 && redirects < 5) {
                    val location = connection.getHeaderField("Location") ?: break
                    connection.disconnect()
                    url = URL(url, location)
                    redirects++
                    continue
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    throw IllegalStateException("HTTP $code")
                }
                break
            }
            try {
                val total = connection.contentLengthLong
                var read = 0L
                var lastReported = -2
                connection.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            read += n
                            val pct = progressPercent(read, total)
                            if (pct != lastReported) {
                                lastReported = pct
                                withContext(Dispatchers.Main) { onProgress(pct) }
                            }
                        }
                    }
                }
                if (total > 0 && read != total) throw IllegalStateException("Download incomplete")
            } finally {
                connection.disconnect()
            }
            dest.delete()
            if (!partial.renameTo(dest)) throw IllegalStateException("Couldn't save the update")
            verifyApk(context, dest)
            dest
        }

    /** 0..100 when the total is known, -1 otherwise. Pure, unit-tested. */
    fun progressPercent(read: Long, total: Long): Int =
        if (total <= 0) -1 else ((read * 100) / total).toInt().coerceIn(0, 100)

    /** Refuses anything that isn't Zikr itself, so a bad redirect or a
     * wrong asset can never be handed to the installer. */
    private fun verifyApk(context: Context, apk: File) {
        val info = context.packageManager.getPackageArchiveInfo(apk.path, 0)
            ?: run { apk.delete(); throw IllegalStateException("Downloaded file isn't a valid app") }
        if (info.packageName != context.packageName) {
            apk.delete()
            throw IllegalStateException("Downloaded app isn't Zikr")
        }
    }

    fun hasDownloadedApk(context: Context): Boolean = apkFile(context).exists()

    /** True if Android will let Zikr open the installer right now. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Opens the one-time "Allow from this source" screen. The activity
     * calls [install] again from onResume once the user comes back. */
    fun requestInstallPermission(activity: Activity) {
        activity.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")),
        )
    }

    /** Must be called from a foreground activity. Returns false if the
     * APK is missing or Android refused to open the installer. */
    fun install(activity: Activity): Boolean {
        val apk = apkFile(activity)
        if (!apk.exists()) return false
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            activity.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "couldn't open installer", e)
            false
        }
    }

    /** Clears a downloaded APK left over from an update that has since
     * been installed (or abandoned), so it doesn't sit in the cache. */
    fun cleanUpIfInstalled(context: Context, currentVersion: String) {
        val apk = apkFile(context)
        if (!apk.exists()) return
        val archived = try {
            context.packageManager.getPackageArchiveInfo(apk.path, 0)?.versionName
        } catch (e: Exception) {
            null
        }
        if (archived == null || UpdateChecker.compareVersions(archived, currentVersion) <= 0) apk.delete()
    }
}
