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
    fun canonicalZikrJsonParses() {
        // The repo-root list every platform reads - copied into res/raw
        // at build time by syncZikrList, never duplicated in the source
        // tree. Path is relative to the module dir, which is Gradle's
        // working directory for unit tests.
        val items = ZikrData.parse(File("../../data/zikr.json").readText())
        assertEquals(49, items.size)
        assertEquals(items.size, items.map { it.id }.distinct().size)
    }

    @Test
    fun bismillahIdIsPresent() {
        // ZikrData.bismillah() does first { it.id == 22 } for the unlock
        // greeting, so losing id 22 from the list is a crash, not a
        // missing phrase.
        val items = ZikrData.parse(File("../../data/zikr.json").readText())
        assertEquals(1, items.count { it.id == 22 })
    }
}
