package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isEnabled: Boolean = true

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Throwable) {
            // Handled gracefully if TTS engine is absent in test/container
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            try {
                val nlLocale = Locale("nl", "NL")
                val langResult = tts?.setLanguage(nlLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale("nl")
                }
                // Snappy speech rate so tactile navigation feels super responsive
                tts?.setSpeechRate(1.25f)
                tts?.setPitch(1.0f)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Speaks the given text. When [interrupt] is true, any ongoing speech
     * is instantly cancelled so the new speech plays immediately.
     */
    fun speak(text: String, interrupt: Boolean = true) {
        if (!isEnabled || text.isBlank()) return
        try {
            if (interrupt) {
                tts?.stop()
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
        } catch (_: Throwable) {}
    }

    /**
     * Immediately stops any speech in progress.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {}
    }

    /**
     * Clean up TTS resources.
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Throwable) {}
    }
}
