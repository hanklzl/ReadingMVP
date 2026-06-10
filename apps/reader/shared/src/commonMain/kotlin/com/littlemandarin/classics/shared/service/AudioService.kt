package com.littlemandarin.classics.shared.service

interface AudioService {
    suspend fun play(resourcePath: String)

    suspend fun hasSentenceAudio(
        storyId: String,
        paragraphIndex: Int,
        sentenceIndex: Int,
    ): Boolean = false

    suspend fun playSentence(
        storyId: String,
        paragraphIndex: Int,
        sentenceIndex: Int,
    ) {
        play(sentenceAudioResourcePath(storyId, paragraphIndex, sentenceIndex))
    }

    suspend fun pause()

    suspend fun stop()
}

object NoOpAudioService : AudioService {
    override suspend fun play(resourcePath: String) = Unit

    override suspend fun pause() = Unit

    override suspend fun stop() = Unit
}

fun sentenceAudioResourcePath(
    storyId: String,
    paragraphIndex: Int,
    sentenceIndex: Int,
): String = "stories/${storyId.trim()}/audio/p${paragraphIndex + 1}_s${sentenceIndex + 1}.wav"

expect fun createAudioService(): AudioService
