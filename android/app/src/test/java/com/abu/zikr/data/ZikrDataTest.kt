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
        assertEquals(52, items.size)
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

    private val greetingJson = """
        [
          {"id": 22, "arabic": "a", "transliteration": "Bismillah", "translation": "t"},
          {"id": 1, "arabic": "a", "transliteration": "SubhanAllah", "translation": "t"},
          {"id": 54, "arabic": "a", "transliteration": "Verse", "translation": "t", "greeting": true}
        ]
    """.trimIndent()

    @Test
    fun greetingFlagDefaultsToFalse() {
        val items = ZikrData.parse(greetingJson)
        assertEquals(listOf(false, false, true), items.map { it.greeting })
    }

    @Test
    fun greetingOnlyEntriesAreNeverReminders() {
        val items = ZikrData.parse(greetingJson)
        assertEquals(listOf(22, 1), ZikrData.reminders(items).map { it.id })
    }

    @Test
    fun unlockGreetingIsSometimesTheVerse() {
        val items = ZikrData.parse(greetingJson)
        assertEquals(54, ZikrData.pickGreeting(items, 0.0).id)
        assertEquals(54, ZikrData.pickGreeting(items, 0.33).id)
        assertEquals(22, ZikrData.pickGreeting(items, 0.34).id)
        assertEquals(22, ZikrData.pickGreeting(items, 0.99).id)
    }

    @Test
    fun unlockGreetingIsBismillahWithoutVerses() {
        val items = ZikrData.reminders(ZikrData.parse(greetingJson))
        assertEquals(22, ZikrData.pickGreeting(items, 0.0).id)
    }

    @Test
    fun canonicalListHasTheCallUponMeGreeting() {
        val items = ZikrData.parse(File("../../data/zikr.json").readText())
        assertEquals(listOf(54), items.filter { it.greeting }.map { it.id })
    }
}
