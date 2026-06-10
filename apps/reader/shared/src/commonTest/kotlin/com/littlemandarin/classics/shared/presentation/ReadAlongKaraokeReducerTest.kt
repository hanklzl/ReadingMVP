package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.StoryAudioCharTiming
import com.littlemandarin.classics.shared.story.StoryAudioSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadAlongKaraokeReducerTest {
    private val reducer = ReadAlongKaraokeReducer()

    @Test
    fun timelineFromSegmentUsesManifestCharTimings() {
        val segment = StoryAudioSegment(
            paragraphIndex = 0,
            sentenceIndex = 0,
            text = "好，吗",
            resourcePath = "stories/x/audio/p1_s1.wav",
            durationMillis = 1000,
            chars = listOf(
                StoryAudioCharTiming("好", 0, 400),
                StoryAudioCharTiming("，", 400, 600),
                StoryAudioCharTiming("吗", 600, 1000),
            ),
        )

        val timeline = reducer.timelineForSegment(segment)

        assertEquals(listOf("好", "，", "吗"), timeline.chars.map { it.character })
        assertEquals(listOf(0, 1, 2), timeline.chars.map { it.charIndex })
        assertEquals(1000, timeline.totalMillis)
    }

    @Test
    fun timelineForTextEvenlySplitsDurationAcrossCodePoints() {
        val timeline = reducer.timelineForText("一二三四", 800)

        assertEquals(4, timeline.chars.size)
        assertEquals(0, timeline.chars.first().startMillis)
        assertEquals(800, timeline.chars.last().endMillis)
        // Monotonic, contiguous windows.
        timeline.chars.zipWithNext().forEach { (previous, current) ->
            assertTrue(previous.endMillis <= current.startMillis)
        }
    }

    @Test
    fun timelineFallsBackToEvenSplitWhenTimingsDriftFromText() {
        val segment = StoryAudioSegment(
            paragraphIndex = 0,
            sentenceIndex = 0,
            text = "一二三",
            resourcePath = "stories/x/audio/p1_s1.wav",
            durationMillis = 900,
            // Only two timings for three characters -> drift.
            chars = listOf(
                StoryAudioCharTiming("一", 0, 300),
                StoryAudioCharTiming("二", 300, 600),
            ),
        )

        val timeline = reducer.timelineForSegment(segment)

        assertEquals(listOf("一", "二", "三"), timeline.chars.map { it.character })
        assertEquals(600, timeline.totalMillis)
    }

    @Test
    fun charIndexAtAdvancesThroughTheTimeline() {
        val timeline = reducer.timelineForSegment(
            StoryAudioSegment(
                paragraphIndex = 0,
                sentenceIndex = 0,
                text = "好，吗",
                resourcePath = "stories/x/audio/p1_s1.wav",
                durationMillis = 1000,
                chars = listOf(
                    StoryAudioCharTiming("好", 0, 400),
                    StoryAudioCharTiming("，", 400, 600),
                    StoryAudioCharTiming("吗", 600, 1000),
                ),
            ),
        )

        assertEquals(0, reducer.charIndexAt(timeline, 0))
        assertEquals(0, reducer.charIndexAt(timeline, 399))
        assertEquals(1, reducer.charIndexAt(timeline, 400))
        assertEquals(2, reducer.charIndexAt(timeline, 700))
        // Past the end stays on the final character (no blink-off).
        assertEquals(2, reducer.charIndexAt(timeline, 5_000))
    }

    @Test
    fun charIndexAtReturnsNullForEmptyTimeline() {
        assertNull(reducer.charIndexAt(KaraokeTimeline.Empty, 100))
    }

    @Test
    fun charIndexForUtf16RangeMapsBmpOffsetsDirectly() {
        val text = "好，吗"

        assertEquals(0, reducer.charIndexForUtf16Range(text, 0))
        assertEquals(1, reducer.charIndexForUtf16Range(text, 1))
        assertEquals(2, reducer.charIndexForUtf16Range(text, 2))
        // Out-of-range clamps to the last character.
        assertEquals(2, reducer.charIndexForUtf16Range(text, 99))
    }

    @Test
    fun charIndexForUtf16RangeHandlesSurrogatePairs() {
        // A supplementary-plane CJK Extension B character (surrogate pair, 2 UTF-16 units).
        val supplementary = "𠀀" // U+20000
        val text = supplementary + "好"

        // UTF-16 offset 0 -> first code point; offset 2 -> second code point ("好").
        assertEquals(0, reducer.charIndexForUtf16Range(text, 0))
        assertEquals(1, reducer.charIndexForUtf16Range(text, 2))
        // The text is two code points even though it is three UTF-16 units long.
        assertEquals(2, text.toCodePointStrings().size)
    }
}
