package com.littlemandarin.classics.shared.service

/**
 * Reports the spoken word/character range as UTF-16 offsets (start, endExclusive)
 * into the utterance string. Modeled as a function type so both Android (lambda)
 * and iOS/Swift (closure) can supply it without implementing a Kotlin interface.
 */
typealias TtsRangeListener = (utf16Start: Int, utf16End: Int) -> Unit

interface TtsService {
    suspend fun speak(text: String, speedMultiplier: Float = 1.0f)

    suspend fun stop()

    /**
     * Register a listener for native per-range speech callbacks
     * (`UtteranceProgressListener.onRangeStart` on Android,
     * `willSpeakRangeOfSpeechString` on iOS). Used to drive character-level
     * karaoke highlight on the system-TTS fallback path. Pass `null` to clear.
     * No-op by default so platforms without range support keep sentence-level
     * highlighting.
     */
    fun setRangeListener(listener: TtsRangeListener?) {}
}

object NoOpTtsService : TtsService {
    override suspend fun speak(text: String, speedMultiplier: Float) = Unit

    override suspend fun stop() = Unit

    override fun setRangeListener(listener: TtsRangeListener?) = Unit
}

expect fun createTtsService(): TtsService
