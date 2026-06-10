package com.littlemandarin.classics.shared.service

interface TtsService {
    suspend fun speak(text: String, speedMultiplier: Float = 1.0f)

    suspend fun stop()
}

object NoOpTtsService : TtsService {
    override suspend fun speak(text: String, speedMultiplier: Float) = Unit

    override suspend fun stop() = Unit
}

expect fun createTtsService(): TtsService
