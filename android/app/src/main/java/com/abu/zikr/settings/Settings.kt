package com.abu.zikr.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "zikr_settings")

/**
 * Persisted preferences, backed by Jetpack DataStore. Mirrors
 * AppSettings.swift / settings.py / Settings.cs: one small store, no
 * database, same defaults and min/max clamp behavior across all
 * platforms.
 */
class Settings(private val context: Context) {
    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val MIN_INTERVAL = intPreferencesKey("min_interval_minutes")
        val MAX_INTERVAL = intPreferencesKey("max_interval_minutes")
        val SPEAK_ALOUD = booleanPreferencesKey("speak_aloud")
        val BISMILLAH_ON_UNLOCK = booleanPreferencesKey("bismillah_on_unlock")
        val SELECTED_VOICE = stringPreferencesKey("selected_voice_name")
        val LAST_UPDATE_CHECK_AT = longPreferencesKey("last_update_check_at")
    }

    val enabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ENABLED] ?: true }
    val minIntervalMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.MIN_INTERVAL] ?: 20 }
    val maxIntervalMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.MAX_INTERVAL] ?: 45 }
    val speakAloud: Flow<Boolean> = context.dataStore.data.map { it[Keys.SPEAK_ALOUD] ?: true }
    val bismillahOnUnlock: Flow<Boolean> = context.dataStore.data.map { it[Keys.BISMILLAH_ON_UNLOCK] ?: true }

    /** Null means "automatic" - Speech picks the default voice for the
     * chosen language, same as before this setting existed. */
    val selectedVoiceName: Flow<String?> = context.dataStore.data.map { it[Keys.SELECTED_VOICE] }

    suspend fun setEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.ENABLED] = value }
    }

    suspend fun setMinInterval(minutes: Int) {
        context.dataStore.edit { prefs ->
            val currentMax = prefs[Keys.MAX_INTERVAL] ?: 45
            val (min, max) = IntervalClamp.onMinChanged(minutes, currentMax)
            prefs[Keys.MIN_INTERVAL] = min
            prefs[Keys.MAX_INTERVAL] = max
        }
    }

    suspend fun setMaxInterval(minutes: Int) {
        context.dataStore.edit { prefs ->
            val currentMin = prefs[Keys.MIN_INTERVAL] ?: 20
            val (min, max) = IntervalClamp.onMaxChanged(minutes, currentMin)
            prefs[Keys.MIN_INTERVAL] = min
            prefs[Keys.MAX_INTERVAL] = max
        }
    }

    suspend fun setSpeakAloud(value: Boolean) {
        context.dataStore.edit { it[Keys.SPEAK_ALOUD] = value }
    }

    suspend fun setBismillahOnUnlock(value: Boolean) {
        context.dataStore.edit { it[Keys.BISMILLAH_ON_UNLOCK] = value }
    }

    suspend fun setSelectedVoiceName(name: String?) {
        context.dataStore.edit { prefs ->
            if (name == null) prefs.remove(Keys.SELECTED_VOICE) else prefs[Keys.SELECTED_VOICE] = name
        }
    }

    /** Not exposed in Settings UI - internal bookkeeping for
     * UpdateFlow's once-a-day passive check cadence. */
    suspend fun lastUpdateCheckAt(): Long = context.dataStore.data.first()[Keys.LAST_UPDATE_CHECK_AT] ?: 0L

    suspend fun setLastUpdateCheckAt(value: Long) {
        context.dataStore.edit { it[Keys.LAST_UPDATE_CHECK_AT] = value }
    }

    suspend fun snapshot(): SettingsSnapshot {
        val prefs = context.dataStore.data.first()
        return SettingsSnapshot(
            enabled = prefs[Keys.ENABLED] ?: true,
            minIntervalMinutes = prefs[Keys.MIN_INTERVAL] ?: 20,
            maxIntervalMinutes = prefs[Keys.MAX_INTERVAL] ?: 45,
            speakAloud = prefs[Keys.SPEAK_ALOUD] ?: true,
            bismillahOnUnlock = prefs[Keys.BISMILLAH_ON_UNLOCK] ?: true,
            selectedVoiceName = prefs[Keys.SELECTED_VOICE],
        )
    }
}

data class SettingsSnapshot(
    val enabled: Boolean,
    val minIntervalMinutes: Int,
    val maxIntervalMinutes: Int,
    val speakAloud: Boolean,
    val bismillahOnUnlock: Boolean,
    val selectedVoiceName: String?,
)
