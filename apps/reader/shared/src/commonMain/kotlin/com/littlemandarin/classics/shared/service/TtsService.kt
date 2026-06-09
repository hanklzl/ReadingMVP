package com.littlemandarin.classics.shared.service

interface TtsService {
    suspend fun speak(text: String)

    suspend fun stop()
}

object NoOpTtsService : TtsService {
    override suspend fun speak(text: String) = Unit

    override suspend fun stop() = Unit
}

expect fun createTtsService(): TtsService
