package com.abu.zikr.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.abu.zikr.data.ZikrItem
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Speaks a zikr aloud, preferring an installed Arabic voice and falling
 * back to reading the English transliteration if none is available -
 * same fallback convention as ZikrSpeaker.swift / speech.py / Speech.cs.
 * Suspends until speech finishes (or fails) so the caller's execution
 * window (a goAsync() coroutine, typically) covers it.
 *
 * The system default voice for a language is often female with no way
 * to change it from the app's own UI otherwise - `listVoicesForCurrentLanguage`
 * and `preview` back the "Voice" picker in Settings so that's a real
 * choice, not just whatever the device defaults to.
 */
object Speech {
    suspend fun speak(context: Context, zikr: ZikrItem, voiceName: String? = null) {
        val engine = initTts(context) ?: return
        val (locale, text) = resolveLocaleAndText(engine, zikr)
        engine.language = locale
        applyVoice(engine, voiceName)
        speakAndWait(engine, text, "zikr-utterance")
    }

    /** Voice names available for whichever language `speak` would
     * actually use right now (Arabic if installed, else English) -
     * network-only voices are excluded to keep the app's "runs entirely
     * offline" promise intact. */
    suspend fun listVoicesForCurrentLanguage(context: Context): List<String> {
        val engine = initTts(context) ?: return emptyList()
        val locale = preferredLocale(engine)
        val names = engine.voices
            ?.filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired }
            ?.map { it.name }
            ?.sorted()
            .orEmpty()
        engine.shutdown()
        return names
    }

    suspend fun preview(context: Context, voiceName: String, sampleText: String) {
        val engine = initTts(context) ?: return
        applyVoice(engine, voiceName)
        speakAndWait(engine, sampleText, "zikr-preview")
    }

    private fun preferredLocale(engine: TextToSpeech): Locale {
        val arabicAvailable = engine.isLanguageAvailable(Locale("ar")) >= TextToSpeech.LANG_AVAILABLE
        return if (arabicAvailable) Locale("ar") else Locale.US
    }

    private fun resolveLocaleAndText(engine: TextToSpeech, zikr: ZikrItem): Pair<Locale, String> {
        val locale = preferredLocale(engine)
        return if (locale.language == "ar") locale to zikr.arabic else locale to zikr.transliteration
    }

    private fun applyVoice(engine: TextToSpeech, voiceName: String?) {
        if (voiceName == null) return
        engine.voices?.firstOrNull { it.name == voiceName }?.let {
            engine.voice = it
            engine.language = it.locale
        }
    }

    private suspend fun initTts(context: Context): TextToSpeech? = suspendCoroutine { continuation ->
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                continuation.resume(tts)
            } else {
                tts?.shutdown()
                continuation.resume(null)
            }
        }
    }

    private suspend fun speakAndWait(engine: TextToSpeech, text: String, utteranceId: String) =
        suspendCoroutine<Unit> { continuation ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    engine.shutdown()
                    continuation.resume(Unit)
                }

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(utteranceId: String?) {
                    engine.shutdown()
                    continuation.resume(Unit)
                }
            })
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
}
