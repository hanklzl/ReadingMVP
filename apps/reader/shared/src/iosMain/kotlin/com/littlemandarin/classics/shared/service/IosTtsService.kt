package com.littlemandarin.classics.shared.service

import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechBoundary

actual fun createTtsService(): TtsService = IosTtsService()

private class IosTtsService(
    private val synthesizer: AVSpeechSynthesizer = AVSpeechSynthesizer(),
) : TtsService {
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

    private companion object {
        const val ChineseLanguageCode: String = "zh-CN"
        const val BaseSpeechRate: Float = 0.50f
        const val MinSpeechRate: Float = 0.5f
        const val MaxSpeechRate: Float = 1.5f
    }
}
