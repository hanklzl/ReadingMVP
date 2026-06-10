package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.StoryAudioCharTiming
import com.littlemandarin.classics.shared.story.StoryAudioSegment

/**
 * One character in a karaoke timeline: the visible character plus the time
 * window (ms, relative to the sentence clip start) during which it is the
 * active "current" character.
 *
 * [charIndex] is the index of this character inside the sentence text measured
 * in Unicode code points so that the UI can map it back to the matching ruby
 * cell regardless of platform string indexing.
 */
data class KaraokeChar(
    val charIndex: Int,
    val character: String,
    val startMillis: Long,
    val endMillis: Long,
)

/**
 * The ordered per-character windows for a single sentence. Built once when a
 * sentence starts playing; the UI then asks [charIndexAt] for the active
 * character as playback progresses (recorded audio), or feeds native TTS range
 * callbacks through [charIndexForUtf16Range] (system-voice fallback).
 */
data class KaraokeTimeline(
    val chars: List<KaraokeChar>,
) {
    val isEmpty: Boolean get() = chars.isEmpty()

    val totalMillis: Long get() = chars.lastOrNull()?.endMillis ?: 0L

    companion object {
        val Empty: KaraokeTimeline = KaraokeTimeline(emptyList())
    }
}

/**
 * Pure business logic for character-level ("karaoke") read-along highlighting.
 *
 * Responsibilities, kept in [shared] so Android and iOS only render:
 * - build a per-character timeline for the active sentence, preferring real
 *   manifest timings ([StoryAudioSegment.chars]) and falling back to an even
 *   split when timings are missing (older manifests / system-TTS fallback),
 * - resolve the active character index from a playback position in ms,
 * - resolve the active character index from a native TTS UTF-16 range callback
 *   (`UtteranceProgressListener.onRangeStart` on Android,
 *   `willSpeakRangeOfSpeechString` on iOS).
 *
 * All offsets returned to the UI are Unicode code-point indices into the
 * sentence text, so a code point that is encoded as a surrogate pair on one
 * platform still maps to a single ruby cell.
 */
class ReadAlongKaraokeReducer {

    /** Build the timeline for the sentence backing [segment] (recorded audio). */
    fun timelineForSegment(segment: StoryAudioSegment): KaraokeTimeline =
        if (segment.chars.isNotEmpty()) {
            timelineFromTimings(segment.text, segment.chars)
        } else {
            timelineForText(segment.text, segment.durationMillis ?: 0L)
        }

    /**
     * Build a timeline from sentence [text] and a known [durationMillis] by
     * dividing the duration evenly across code points. Used when there are no
     * manifest timings (older content) but a clip duration is known.
     */
    fun timelineForText(text: String, durationMillis: Long): KaraokeTimeline {
        val codePoints = text.toCodePointStrings()
        if (codePoints.isEmpty()) return KaraokeTimeline.Empty

        val total = durationMillis.coerceAtLeast(0L)
        val count = codePoints.size
        val chars = codePoints.mapIndexed { index, character ->
            val start = total * index / count
            val end = if (index == count - 1) total else total * (index + 1) / count
            KaraokeChar(
                charIndex = index,
                character = character,
                startMillis = start,
                endMillis = maxOf(start, end),
            )
        }
        return KaraokeTimeline(chars)
    }

    private fun timelineFromTimings(
        text: String,
        timings: List<StoryAudioCharTiming>,
    ): KaraokeTimeline {
        val codePoints = text.toCodePointStrings()
        // Trust the manifest contract (chars 1:1 with text); if it ever drifts,
        // fall back to an even split keyed off the last available end time.
        if (codePoints.size != timings.size) {
            val fallbackDuration = timings.lastOrNull()?.endMillis ?: 0L
            return timelineForText(text, fallbackDuration)
        }
        var previousEnd = 0L
        val chars = timings.mapIndexed { index, timing ->
            val start = maxOf(timing.startMillis, previousEnd).coerceAtLeast(0L)
            val end = maxOf(timing.endMillis, start)
            previousEnd = end
            KaraokeChar(
                charIndex = index,
                character = codePoints[index],
                startMillis = start,
                endMillis = end,
            )
        }
        return KaraokeTimeline(chars)
    }

    /**
     * The active character index for a playback [positionMillis] within the
     * sentence clip, or `null` before the first character / when empty. The last
     * character stays active through the clip end so the highlight never blinks
     * off at the tail.
     */
    fun charIndexAt(timeline: KaraokeTimeline, positionMillis: Long): Int? {
        if (timeline.isEmpty) return null
        val position = positionMillis.coerceAtLeast(0L)
        timeline.chars.forEach { char ->
            if (position < char.endMillis) return char.charIndex
        }
        return timeline.chars.last().charIndex
    }

    /**
     * Map a native TTS UTF-16 range (start offset + length into the original
     * spoken string) to a code-point index in the sentence text.
     *
     * Android's `onRangeStart(utteranceId, start, end, frame)` and iOS'
     * `willSpeakRangeOfSpeechString` both report UTF-16 offsets, so we convert
     * the UTF-16 [utf16Start] into a code-point index.
     */
    fun charIndexForUtf16Range(text: String, utf16Start: Int): Int? {
        if (text.isEmpty() || utf16Start < 0) return null
        val clamped = utf16Start.coerceAtMost(text.length)
        var codePointIndex = 0
        var offset = 0
        while (offset < clamped && offset < text.length) {
            offset += charCountAt(text, offset)
            if (offset <= clamped) codePointIndex += 1
        }
        val lastIndex = text.toCodePointStrings().lastIndex
        if (lastIndex < 0) return null
        return codePointIndex.coerceIn(0, lastIndex)
    }
}

/** Split a string into one entry per Unicode code point (surrogate-pair safe). */
internal fun String.toCodePointStrings(): List<String> {
    if (isEmpty()) return emptyList()
    val result = ArrayList<String>(length)
    var index = 0
    while (index < length) {
        val count = charCountAt(this, index)
        result.add(substring(index, index + count))
        index += count
    }
    return result
}

private fun charCountAt(text: String, index: Int): Int {
    val high = text[index]
    if (high.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
        return 2
    }
    return 1
}
