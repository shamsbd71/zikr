package com.abu.zikr.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    }

    val enabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ENABLED] ?: true }
    val minIntervalMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.MIN_INTERVAL] ?: 20 }
    val maxIntervalMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.MAX_INTERVAL] ?: 45 }
    val speakAloud: Flow<Boolean> = context.dataStore.data.map { it[Keys.SPEAK_ALOUD] ?: true }

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

    suspend fun snapshot(): SettingsSnapshot {
        val prefs = context.dataStore.data.first()
        return SettingsSnapshot(
            enabled = prefs[Keys.ENABLED] ?: true,
            minIntervalMinutes = prefs[Keys.MIN_INTERVAL] ?: 20,
            maxIntervalMinutes = prefs[Keys.MAX_INTERVAL] ?: 45,
            speakAloud = prefs[Keys.SPEAK_ALOUD] ?: true,
        )
    }
}

data class SettingsSnapshot(
    val enabled: Boolean,
    val minIntervalMinutes: Int,
    val maxIntervalMinutes: Int,
    val speakAloud: Boolean,
)
