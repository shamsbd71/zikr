package com.abu.zikr.update

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ZikrUpdateChecker"

data class UpdateInfo(
    val version: String,
    val apkDownloadUrl: String,
)

sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

/**
 * Checks GitHub Releases for a newer tag than the running app's
 * version - mirrors UpdateChecker.swift / update_checker.py /
 * UpdateChecker.cs, including the three-way CheckResult so a failed
 * check (no internet, GitHub API rate limit) is never confused with
 * genuinely being up to date. Unlike those, this one *can* self-install:
 * Android lets a sideloaded app download a file and hand it to the
 * system install flow (the user still confirms - Android never skips
 * that - but there's no separate "open a browser, download, find the
 * file" dance the desktop builds' no-self-updater decision was avoiding
 * for un-signed installs).
 */
object UpdateChecker {
    private const val API_URL = "https://api.github.com/repos/shamsbd71/zikr/releases/latest"
    const val RELEASES_URL = "https://github.com/shamsbd71/zikr/releases/latest"

    /** -1 if a<b, 0 if equal, 1 if a>b, comparing dot-separated numeric
     * components. Pure function, mirrors CompareVersions (Windows) /
     * compare_versions (Linux). */
    fun compareVersions(a: String, b: String): Int {
        fun parts(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 }
        val pa = parts(a)
        val pb = parts(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val va = pa.getOrElse(i) { 0 }
            val vb = pb.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }

    /** Blocking network call - always invoke from a background thread
     * (e.g. Dispatchers.IO). */
    fun checkForUpdate(currentVersion: String): UpdateCheckResult {
        return try {
            val release = fetchLatestRelease()
                ?: return UpdateCheckResult.Error("Couldn't check for updates. Try again later.")
            val (latestVersion, apkUrl) = release
            Log.d(TAG, "current=$currentVersion latest=$latestVersion")
            if (compareVersions(latestVersion, currentVersion) > 0) {
                UpdateCheckResult.Available(UpdateInfo(latestVersion, apkUrl))
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            Log.w(TAG, "checkForUpdate failed", e)
            UpdateCheckResult.Error("Couldn't check for updates. Try again later.")
        }
    }

    private fun fetchLatestRelease(): Pair<String, String>? {
        val connection = URL(API_URL).openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val text = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val tag = json.optString("tag_name")
            if (tag.isEmpty()) return null
            val version = if (tag.startsWith("v")) tag.substring(1) else tag

            val assets = json.optJSONArray("assets") ?: return null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk")) {
                    val url = asset.optString("browser_download_url")
                    if (url.isNotEmpty()) return version to url
                }
            }
            return null
        } finally {
            connection.disconnect()
        }
    }
}
