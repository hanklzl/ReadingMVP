package com.littlemandarin.classics.shared.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object AndroidTtsServiceProvider {
    private var applicationContext: Context? = null
    private var service: TtsService? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun create(): TtsService {
        service?.let { return it }

        val context = applicationContext
            ?: error(
                "AndroidTtsServiceProvider.initialize(context) must be called before " +
                    "createTtsService().",
            )

        return AndroidTtsService(context).also { service = it }
    }
}

actual fun createTtsService(): TtsService = AndroidTtsServiceProvider.create()

private class AndroidTtsService(
    context: Context,
) : TtsService {
    private val lock = Any()
    private var isReady: Boolean = false
    private var pendingText: String? = null

    private val textToSpeech: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            configureChineseLocale()
            val text = synchronized(lock) {
                isReady = true
                pendingText.also { pendingText = null }
            }

            text?.let(::speakNow)
        } else {
            synchronized(lock) {
                pendingText = null
            }
        }
    }

    override suspend fun speak(text: String) {
        if (text.isBlank()) return

        val shouldSpeakNow = synchronized(lock) {
            pendingText = null
            textToSpeech.stop()

            if (isReady) {
                true
            } else {
                pendingText = text
                false
            }
        }

        if (shouldSpeakNow) {
            speakNow(text)
        }
    }

    override suspend fun stop() {
        synchronized(lock) {
            pendingText = null
            textToSpeech.stop()
        }
    }

    private fun configureChineseLocale() {
        val locale = listOf(Locale.SIMPLIFIED_CHINESE, Locale.CHINESE)
            .firstOrNull { textToSpeech.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }

        if (locale != null) {
            textToSpeech.language = locale
        }
    }

    private fun speakNow(text: String) {
        textToSpeech.stop()
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UtteranceId)
    }

    private companion object {
        const val UtteranceId: String = "little_mandarin_tts"
    }
}
