package com.littlemandarin.classics.shared.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
    private var pendingUtterance: PendingUtterance? = null

    @Volatile
    private var rangeListener: TtsRangeListener? = null

    private val textToSpeech: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            configureChineseLocale()
            textToSpeech.setOnUtteranceProgressListener(progressListener)
            val utterance = synchronized(lock) {
                isReady = true
                pendingUtterance.also { pendingUtterance = null }
            }

            utterance?.let { speakNow(it.text, it.speedMultiplier) }
        } else {
            synchronized(lock) {
                pendingUtterance = null
            }
        }
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) = Unit

        @Deprecated("Deprecated in Java", ReplaceWith(""))
        override fun onError(utteranceId: String?) = Unit

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            rangeListener?.invoke(start, end)
        }
    }

    override fun setRangeListener(listener: TtsRangeListener?) {
        rangeListener = listener
    }

    override suspend fun speak(text: String, speedMultiplier: Float) {
        if (text.isBlank()) return
        val safeSpeed = speedMultiplier.coerceIn(MinSpeechRate, MaxSpeechRate)

        val shouldSpeakNow = synchronized(lock) {
            pendingUtterance = null
            textToSpeech.stop()

            if (isReady) {
                true
            } else {
                pendingUtterance = PendingUtterance(text, safeSpeed)
                false
            }
        }

        if (shouldSpeakNow) {
            speakNow(text, safeSpeed)
        }
    }

    override suspend fun stop() {
        synchronized(lock) {
            pendingUtterance = null
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

    private fun speakNow(text: String, speedMultiplier: Float) {
        textToSpeech.stop()
        textToSpeech.setSpeechRate(speedMultiplier)
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UtteranceId)
    }

    private data class PendingUtterance(
        val text: String,
        val speedMultiplier: Float,
    )

    private companion object {
        const val UtteranceId: String = "little_mandarin_tts"
        const val MinSpeechRate: Float = 0.5f
        const val MaxSpeechRate: Float = 1.5f
    }
}
