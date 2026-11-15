package com.abu.zikr.data

data class ZikrItem(
    val id: Int,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    /** Said only as the unlock greeting, never as a random reminder. */
    val greeting: Boolean = false,
)
