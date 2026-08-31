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
 */
object Speech {
    suspend fun speak(context: Context, zikr: ZikrItem) = suspendCoroutine { continuation ->
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) {
                engine?.shutdown()
                continuation.resume(Unit)
                return@TextToSpeech
            }

            val arabicAvailable = engine.isLanguageAvailable(Locale("ar")) >= TextToSpeech.LANG_AVAILABLE
            val (locale, text) = if (arabicAvailable) {
                Locale("ar") to zikr.arabic
            } else {
                Locale.US to zikr.transliteration
            }
            engine.language = locale

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

            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zikr-utterance")
        }
    }
}
