package com.abu.zikr.data

import android.content.Context
import com.abu.zikr.R
import org.json.JSONArray

/**
 * Loads the bundled zikr list. Same 21 general adhkar as the other
 * builds' ZikrList.swift/zikr_data.py/ZikrData.cs - res/raw/zikr.json
 * is kept in sync with them by hand.
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
            )
        }
    }

    fun random(context: Context): ZikrItem = all(context).random()
}
