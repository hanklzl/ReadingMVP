package com.littlemandarin.classics.shared.service

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechBoundary
import platform.Foundation.NSRange
import platform.darwin.NSObject

actual fun createTtsService(): TtsService = IosTtsService()

private class IosTtsService(
    private val synthesizer: AVSpeechSynthesizer = AVSpeechSynthesizer(),
) : TtsService {
    private var rangeListener: TtsRangeListener? = null

    // Bridges AVSpeechSynthesizer's willSpeakRangeOfSpeechString callback to the
    // shared TtsRangeListener so the karaoke highlight can follow the system voice.
    private val speechDelegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @OptIn(ExperimentalForeignApi::class)
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            willSpeakRangeOfSpeechString: CValue<NSRange>,
            utterance: AVSpeechUtterance,
        ) {
            willSpeakRangeOfSpeechString.useContents {
                val start = location.toInt()
                val end = (location + length).toInt()
                rangeListener?.invoke(start, end)
            }
        }
    }

    init {
        synthesizer.delegate = speechDelegate
    }

    override suspend fun speak(text: String, speedMultiplier: Float) {
        if (text.isBlank()) return

        stop()

        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(ChineseLanguageCode)
        utterance.rate = BaseSpeechRate * speedMultiplier.coerceIn(MinSpeechRate, MaxSpeechRate)
        synthesizer.speakUtterance(utterance)
    }

    override suspend fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override fun setRangeListener(listener: TtsRangeListener?) {
        rangeListener = listener
    }

    private companion object {
        const val ChineseLanguageCode: String = "zh-CN"
        const val BaseSpeechRate: Float = 0.50f
        const val MinSpeechRate: Float = 0.5f
        const val MaxSpeechRate: Float = 1.5f
    }
}
