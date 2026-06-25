package com.littlemandarin.classics.shared.service

import com.littlemandarin.classics.shared.story.StoryAudioSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class AudioServiceTest {
    @Test
    fun playSegmentUsesRangeWhenSentenceHasStoryWideTiming() = runTest {
        val service = RecordingAudioService()
        val segment = StoryAudioSegment(
            paragraphIndex = 0,
            sentenceIndex = 1,
            text = "一句话。",
            resourcePath = "stories/sample/audio/story.wav",
            startMillis = 1200,
            endMillis = 2500,
            durationMillis = 1300,
        )

        service.playSegment(segment, speedMultiplier = 1.25f)

        assertEquals(
            RangeCall(
                resourcePath = "stories/sample/audio/story.wav",
                startMillis = 1200,
                endMillis = 2500,
                speedMultiplier = 1.25f,
            ),
            service.rangeCall,
        )
        assertEquals(null, service.playCall)
    }

    @Test
    fun playSegmentFallsBackToWholeResourceWhenRangeIsMissing() = runTest {
        val service = RecordingAudioService()
        val segment = StoryAudioSegment(
            paragraphIndex = 0,
            sentenceIndex = 1,
            text = "一句话。",
            resourcePath = "stories/sample/audio/p1_s2.wav",
            durationMillis = 1300,
        )

        service.playSegment(segment, speedMultiplier = 0.9f)

        assertEquals(PlayCall("stories/sample/audio/p1_s2.wav", 0.9f), service.playCall)
        assertEquals(null, service.rangeCall)
    }
}

private data class PlayCall(
    val resourcePath: String,
    val speedMultiplier: Float,
)

private data class RangeCall(
    val resourcePath: String,
    val startMillis: Long,
    val endMillis: Long,
    val speedMultiplier: Float,
)

private class RecordingAudioService : AudioService {
    var playCall: PlayCall? = null
    var rangeCall: RangeCall? = null

    override suspend fun play(resourcePath: String, speedMultiplier: Float) {
        playCall = PlayCall(resourcePath, speedMultiplier)
    }

    override suspend fun playRange(
        resourcePath: String,
        startMillis: Long,
        endMillis: Long,
        speedMultiplier: Float,
    ) {
        rangeCall = RangeCall(resourcePath, startMillis, endMillis, speedMultiplier)
    }

    override suspend fun pause() = Unit

    override suspend fun stop() = Unit
}
