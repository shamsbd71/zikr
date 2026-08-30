package com.abu.zikr.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class ZikrDataTest {
    private val sampleJson = """
        [
          {"id": 1, "arabic": "test-ar-1", "transliteration": "SubhanAllah", "translation": "Glory be to Allah"},
          {"id": 2, "arabic": "test-ar-2", "transliteration": "Alhamdulillah", "translation": "All praise is due to Allah"}
        ]
    """.trimIndent()

    @Test
    fun parsesAllFields() {
        val items = ZikrData.parse(sampleJson)
        assertEquals(2, items.size)
        assertEquals("SubhanAllah", items[0].transliteration)
        assertEquals("Glory be to Allah", items[0].translation)
    }

    @Test
    fun idsArePreserved() {
        val items = ZikrData.parse(sampleJson)
        assertEquals(1, items[0].id)
        assertEquals(2, items[1].id)
    }

    @Test
    fun bundledZikrJsonHasExpectedCount() {
        // Keep in sync with ZikrList.swift / zikr_data.py / ZikrData.cs.
        val file = File("src/main/res/raw/zikr.json")
        val items = ZikrData.parse(file.readText())
        assertEquals(21, items.size)
    }
}
