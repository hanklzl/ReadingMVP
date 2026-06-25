package com.littlemandarin.classics.shared.service

import com.littlemandarin.classics.shared.story.StoryAudioSegment

interface AudioService {
    suspend fun play(resourcePath: String, speedMultiplier: Float = 1.0f)

    suspend fun playRange(
        resourcePath: String,
        startMillis: Long,
        endMillis: Long,
        speedMultiplier: Float = 1.0f,
    ) {
        play(resourcePath = resourcePath, speedMultiplier = speedMultiplier)
    }

    suspend fun playSegment(
        segment: StoryAudioSegment,
        speedMultiplier: Float = 1.0f,
    ) {
        val startMillis = segment.startMillis
        val endMillis = segment.endMillis
        if (startMillis != null && endMillis != null && endMillis > startMillis) {
            playRange(
                resourcePath = segment.resourcePath,
                startMillis = startMillis,
                endMillis = endMillis,
                speedMultiplier = speedMultiplier,
            )
        } else {
            play(resourcePath = segment.resourcePath, speedMultiplier = speedMultiplier)
        }
    }

    suspend fun hasAudio(segment: StoryAudioSegment?): Boolean = segment != null

    suspend fun hasSentenceAudio(
        storyId: String,
        paragraphIndex: Int,
        sentenceIndex: Int,
    ): Boolean = false

    suspend fun playSentence(
        storyId: String,
        paragraphIndex: Int,
        sentenceIndex: Int,
        speedMultiplier: Float = 1.0f,
    ) {
        play(
            resourcePath = sentenceAudioResourcePath(storyId, paragraphIndex, sentenceIndex),
            speedMultiplier = speedMultiplier,
        )
    }

    suspend fun pause()

    suspend fun stop()

    /**
     * Current playback position of the active clip in milliseconds, or `null`
     * when nothing is playing / unsupported. Used by the karaoke highlight to
     * resolve the active character from the manifest timings. Polling-friendly:
     * implementations should return quickly and never throw.
     */
    fun currentPositionMillis(): Long? = null
}

object NoOpAudioService : AudioService {
    override suspend fun play(resourcePath: String, speedMultiplier: Float) = Unit

    override suspend fun pause() = Unit

    override suspend fun stop() = Unit

    override fun currentPositionMillis(): Long? = null
}

fun sentenceAudioResourcePath(
    storyId: String,
    paragraphIndex: Int,
    sentenceIndex: Int,
): String = "stories/${storyId.trim()}/audio/p${paragraphIndex + 1}_s${sentenceIndex + 1}.wav"

expect fun createAudioService(): AudioService
