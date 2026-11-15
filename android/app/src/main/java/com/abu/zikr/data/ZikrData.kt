package com.abu.zikr.data

import android.content.Context
import com.abu.zikr.R
import org.json.JSONArray
import kotlin.random.Random

/**
 * Loads the bundled zikr list. res/raw/zikr.json is copied straight from
 * the repo-root data/zikr.json at build time (see syncZikrList in
 * build.gradle.kts), so this build reads the same list as every other
 * platform rather than a copy kept in step by hand.
 */
object ZikrData {
    @Volatile private var cached: List<ZikrItem>? = null

    fun all(context: Context): List<ZikrItem> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val text = context.resources.openRawResource(R.raw.zikr)
                .bufferedReader()
                .use { it.readText() }
            val loaded = parse(text)
            cached = loaded
            return loaded
        }
    }

    /** Pure function so parsing is unit-testable without a Context. */
    fun parse(jsonText: String): List<ZikrItem> {
        val array = JSONArray(jsonText)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            ZikrItem(
                id = obj.getInt("id"),
                arabic = obj.getString("arabic"),
                transliteration = obj.getString("transliteration"),
                translation = obj.getString("translation"),
                greeting = obj.optBoolean("greeting", false),
            )
        }
    }

    /**
     * How often the unlock greeting is one of the greeting-only verses
     * instead of Bismillah - same value as ZikrSelector.swift on macOS.
     */
    const val GREETING_VERSE_CHANCE = 1.0 / 3.0

    /** Greeting-only entries are said on unlock, never as a reminder. */
    fun reminders(items: List<ZikrItem>): List<ZikrItem> = items.filter { !it.greeting }

    fun random(context: Context): ZikrItem = reminders(all(context)).random()

    /** Id 22 in zikr.json - said on unlock, mirroring UnlockGreeter.swift. */
    fun bismillah(context: Context): ZikrItem = all(context).first { it.id == 22 }

    fun unlockGreeting(context: Context): ZikrItem =
        pickGreeting(all(context), Random.nextDouble())

    /**
     * Bismillah, or with GREETING_VERSE_CHANCE one of the greeting-only
     * entries. [roll] is in [0, 1) and decides both whether and which,
     * so the choice is testable with a fixed number.
     */
    fun pickGreeting(items: List<ZikrItem>, roll: Double): ZikrItem {
        val bismillah = items.first { it.id == 22 }
        val verses = items.filter { it.greeting }
        if (verses.isEmpty() || roll >= GREETING_VERSE_CHANCE) return bismillah
        val index = (roll / GREETING_VERSE_CHANCE * verses.size).toInt()
        return verses[minOf(index, verses.size - 1)]
    }
}
