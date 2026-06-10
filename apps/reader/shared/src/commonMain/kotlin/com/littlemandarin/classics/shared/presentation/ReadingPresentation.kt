package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.StoryAudioManifest

enum class ReadingPlaybackStatus {
    Stopped,
    Playing,
    Paused,
}

enum class ReadingSessionMode {
    ReadAlong,
    TapToListen,
}

typealias ReadingPlaybackMode = ReadingSessionMode

enum class ReadingAudioSource(val analyticsValue: String) {
    Recorded("recorded"),
    Tts("tts"),
}

enum class ReadingPlaybackSpeed(
    val multiplier: Double,
    val analyticsBucket: String,
) {
    Slow(0.80, "slow"),
    DefaultSlow(0.90, "default_slow"),
    Normal(1.00, "normal"),
}

data class SentencePlaybackTarget(
    val paragraphIndex: Int,
    val sentenceIndex: Int,
)

data class ReadingSessionState(
    val storyId: String,
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val paragraphCount: Int,
    val currentParagraph: Paragraph?,
    val segments: List<SentenceSegment>,
    val currentSentence: SentenceSegment?,
    val currentAudioResourcePath: String?,
    val hasGeneratedAudio: Boolean,
    val playbackStatus: ReadingPlaybackStatus,
    val playbackMode: ReadingSessionMode,
    val autoContinue: Boolean,
    val playbackSpeed: ReadingPlaybackSpeed,
    val shouldAutoAdvanceAfterSentence: Boolean,
    val progressFraction: Double,
    val audioSource: ReadingAudioSource,
    val canGoPrevious: Boolean,
    val canGoPreviousSentence: Boolean,
    val canGoNextSentence: Boolean,
    val isLastParagraph: Boolean,
) {
    val playbackSpeedBucket: String
        get() = playbackSpeed.analyticsBucket
}

data class ReadingSessionTransition(
    val state: ReadingSessionState,
    val shouldOpenVocabulary: Boolean = false,
)

class ReadingSessionReducer {
    fun initialState(
        story: Story,
        savedParagraphIndex: Int,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
        savedSentenceIndex: Int = 0,
        playbackMode: ReadingSessionMode = ReadingSessionMode.ReadAlong,
        autoContinue: Boolean = true,
        playbackSpeed: ReadingPlaybackSpeed = ReadingPlaybackSpeed.DefaultSlow,
    ): ReadingSessionState = stateFor(
        story = story,
        paragraphIndex = savedParagraphIndex,
        sentenceIndex = savedSentenceIndex,
        audioManifest = audioManifest,
        playbackMode = playbackMode,
        autoContinue = autoContinue,
        playbackSpeed = playbackSpeed,
    )

    fun previous(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionState = stateFor(
        story = story,
        paragraphIndex = state.paragraphIndex - 1,
        audioManifest = audioManifest,
        playbackMode = state.playbackMode,
        autoContinue = state.autoContinue,
        playbackSpeed = state.playbackSpeed,
    )

    fun next(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionTransition {
        val normalizedState = stateFor(
            story = story,
            paragraphIndex = state.paragraphIndex,
            sentenceIndex = state.sentenceIndex,
            audioManifest = audioManifest,
            playbackMode = state.playbackMode,
            autoContinue = state.autoContinue,
            playbackSpeed = state.playbackSpeed,
        )
        if (normalizedState.isLastParagraph) {
            return ReadingSessionTransition(
                state = stop(normalizedState),
                shouldOpenVocabulary = true,
            )
        }

        return ReadingSessionTransition(
            state = stateFor(
                story = story,
                paragraphIndex = normalizedState.paragraphIndex + 1,
                audioManifest = audioManifest,
                playbackMode = normalizedState.playbackMode,
                autoContinue = normalizedState.autoContinue,
                playbackSpeed = normalizedState.playbackSpeed,
            ),
        )
    }

    fun start(state: ReadingSessionState): ReadingSessionState =
        if (state.currentSentence == null) {
            state.copy(playbackStatus = ReadingPlaybackStatus.Stopped)
        } else {
            state.copy(
                playbackStatus = ReadingPlaybackStatus.Playing,
                playbackMode = ReadingSessionMode.ReadAlong,
                shouldAutoAdvanceAfterSentence = shouldAutoAdvanceAfterSentence(
                    playbackStatus = ReadingPlaybackStatus.Playing,
                    mode = ReadingSessionMode.ReadAlong,
                    autoContinue = state.autoContinue,
                ),
            )
        }

    fun playReadAlong(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionState = stateFor(
        story = story,
        paragraphIndex = state.paragraphIndex,
        sentenceIndex = state.sentenceIndex,
        audioManifest = audioManifest,
        playbackStatus = if (state.currentSentence == null) {
            ReadingPlaybackStatus.Stopped
        } else {
            ReadingPlaybackStatus.Playing
        },
        playbackMode = ReadingSessionMode.ReadAlong,
        autoContinue = state.autoContinue,
        playbackSpeed = state.playbackSpeed,
    )

    fun playSentence(
        story: Story,
        state: ReadingSessionState,
        target: SentencePlaybackTarget,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
        playbackMode: ReadingSessionMode = state.playbackMode,
    ): ReadingSessionState = playSentence(
        story = story,
        state = state,
        paragraphIndex = target.paragraphIndex,
        sentenceIndex = target.sentenceIndex,
        audioManifest = audioManifest,
        playbackMode = playbackMode,
    )

    fun playSentence(
        story: Story,
        state: ReadingSessionState,
        paragraphIndex: Int,
        sentenceIndex: Int,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
        playbackMode: ReadingSessionMode = state.playbackMode,
    ): ReadingSessionState = stateFor(
        story = story,
        paragraphIndex = paragraphIndex,
        sentenceIndex = sentenceIndex,
        audioManifest = audioManifest,
        playbackStatus = ReadingPlaybackStatus.Playing,
        playbackMode = playbackMode,
        autoContinue = state.autoContinue,
        playbackSpeed = state.playbackSpeed,
    )

    fun repeatCurrentSentence(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionState = repeatSentence(story, state, audioManifest)

    fun repeatSentence(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionState = stateFor(
        story = story,
        paragraphIndex = state.paragraphIndex,
        sentenceIndex = state.sentenceIndex,
        audioManifest = audioManifest,
        playbackStatus = if (state.currentSentence == null) {
            ReadingPlaybackStatus.Stopped
        } else {
            ReadingPlaybackStatus.Playing
        },
        playbackMode = ReadingSessionMode.TapToListen,
        autoContinue = state.autoContinue,
        playbackSpeed = state.playbackSpeed,
    )

    fun resume(state: ReadingSessionState): ReadingSessionState =
        if (state.playbackStatus == ReadingPlaybackStatus.Paused) {
            state.copy(
                playbackStatus = ReadingPlaybackStatus.Playing,
                shouldAutoAdvanceAfterSentence = shouldAutoAdvanceAfterSentence(
                    playbackStatus = ReadingPlaybackStatus.Playing,
                    mode = state.playbackMode,
                    autoContinue = state.autoContinue,
                ),
            )
        } else {
            state
        }

    fun pause(state: ReadingSessionState): ReadingSessionState =
        if (state.playbackStatus == ReadingPlaybackStatus.Playing) {
            state.copy(
                playbackStatus = ReadingPlaybackStatus.Paused,
                shouldAutoAdvanceAfterSentence = false,
            )
        } else {
            state
        }

    fun stop(state: ReadingSessionState): ReadingSessionState =
        state.copy(
            playbackStatus = ReadingPlaybackStatus.Stopped,
            shouldAutoAdvanceAfterSentence = false,
        )

    fun setAutoContinue(
        state: ReadingSessionState,
        enabled: Boolean,
    ): ReadingSessionState = state.copy(
        autoContinue = enabled,
        shouldAutoAdvanceAfterSentence = shouldAutoAdvanceAfterSentence(
            playbackStatus = state.playbackStatus,
            mode = state.playbackMode,
            autoContinue = enabled,
        ),
    )

    fun setPlaybackSpeed(
        state: ReadingSessionState,
        speed: ReadingPlaybackSpeed,
    ): ReadingSessionState = state.copy(playbackSpeed = speed)

    fun setPlaybackMode(
        state: ReadingSessionState,
        mode: ReadingSessionMode,
    ): ReadingSessionState = state.copy(
        playbackMode = mode,
        shouldAutoAdvanceAfterSentence = shouldAutoAdvanceAfterSentence(
            playbackStatus = state.playbackStatus,
            mode = mode,
            autoContinue = state.autoContinue,
        ),
    )

    fun sentenceFinished(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionTransition = onCurrentSentencePlaybackComplete(story, state, audioManifest)

    fun onCurrentSentencePlaybackComplete(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionTransition {
        val normalizedState = stateFor(
            story = story,
            paragraphIndex = state.paragraphIndex,
            sentenceIndex = state.sentenceIndex,
            audioManifest = audioManifest,
            playbackStatus = state.playbackStatus,
            playbackMode = state.playbackMode,
            autoContinue = state.autoContinue,
            playbackSpeed = state.playbackSpeed,
        )
        if (normalizedState.playbackStatus != ReadingPlaybackStatus.Playing) {
            return ReadingSessionTransition(normalizedState)
        }
        if (!normalizedState.shouldAutoAdvanceAfterSentence) {
            return ReadingSessionTransition(stop(normalizedState))
        }

        val next = nextSentence(story, normalizedState, audioManifest)
        return if (next.shouldOpenVocabulary) {
            ReadingSessionTransition(
                state = stop(normalizedState),
                shouldOpenVocabulary = true,
            )
        } else {
            next.copy(
                state = next.state.copy(playbackStatus = ReadingPlaybackStatus.Playing),
            )
        }
    }

    fun nextSentence(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionTransition {
        val target = nextSentenceTarget(story, SentencePlaybackTarget(state.paragraphIndex, state.sentenceIndex))
            ?: return ReadingSessionTransition(
                state = stop(state),
                shouldOpenVocabulary = isStoryEnd(story, state),
            )

        return ReadingSessionTransition(
            state = stateFor(
                story = story,
                paragraphIndex = target.paragraphIndex,
                sentenceIndex = target.sentenceIndex,
                audioManifest = audioManifest,
                playbackStatus = state.playbackStatus,
                playbackMode = state.playbackMode,
                autoContinue = state.autoContinue,
                playbackSpeed = state.playbackSpeed,
            ),
        )
    }

    fun previousSentence(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionTransition {
        val target = previousSentenceTarget(story, SentencePlaybackTarget(state.paragraphIndex, state.sentenceIndex))
            ?: return ReadingSessionTransition(state)

        return ReadingSessionTransition(
            state = stateFor(
                story = story,
                paragraphIndex = target.paragraphIndex,
                sentenceIndex = target.sentenceIndex,
                audioManifest = audioManifest,
                playbackStatus = state.playbackStatus,
                playbackMode = state.playbackMode,
                autoContinue = state.autoContinue,
                playbackSpeed = state.playbackSpeed,
            ),
        )
    }

    fun stateFor(
        story: Story,
        paragraphIndex: Int,
        sentenceIndex: Int = 0,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
        playbackStatus: ReadingPlaybackStatus = ReadingPlaybackStatus.Stopped,
        playbackMode: ReadingSessionMode = ReadingSessionMode.ReadAlong,
        autoContinue: Boolean = true,
        playbackSpeed: ReadingPlaybackSpeed = ReadingPlaybackSpeed.DefaultSlow,
    ): ReadingSessionState {
        val paragraphCount = story.paragraphs.size.coerceAtLeast(1)
        val clampedParagraphIndex = paragraphIndex.coerceIn(0, paragraphCount - 1)
        val paragraph = story.paragraphs.getOrNull(clampedParagraphIndex)
        val segments = paragraph?.let { SentenceSegmenter.segment(it.text) }.orEmpty()
        val clampedSentenceIndex = if (segments.isEmpty()) {
            0
        } else {
            sentenceIndex.coerceIn(0, segments.lastIndex)
        }
        val target = SentencePlaybackTarget(clampedParagraphIndex, clampedSentenceIndex)
        val audioSegment = audioManifest.segmentFor(
            paragraphIndex = clampedParagraphIndex,
            sentenceIndex = clampedSentenceIndex,
        )
        val normalizedStatus = if (segments.isEmpty()) ReadingPlaybackStatus.Stopped else playbackStatus
        val progress = if (story.paragraphs.isEmpty()) {
            0.0
        } else {
            (clampedParagraphIndex + 1).toDouble() / paragraphCount.toDouble()
        }

        return ReadingSessionState(
            storyId = story.id,
            paragraphIndex = clampedParagraphIndex,
            sentenceIndex = clampedSentenceIndex,
            paragraphCount = paragraphCount,
            currentParagraph = paragraph,
            segments = segments,
            currentSentence = segments.getOrNull(clampedSentenceIndex),
            currentAudioResourcePath = audioSegment?.resourcePath,
            hasGeneratedAudio = audioSegment != null,
            playbackStatus = normalizedStatus,
            playbackMode = playbackMode,
            autoContinue = autoContinue,
            playbackSpeed = playbackSpeed,
            shouldAutoAdvanceAfterSentence = shouldAutoAdvanceAfterSentence(
                playbackStatus = normalizedStatus,
                mode = playbackMode,
                autoContinue = autoContinue,
            ),
            progressFraction = progress.coerceIn(0.0, 1.0),
            audioSource = if (audioSegment != null) ReadingAudioSource.Recorded else ReadingAudioSource.Tts,
            canGoPrevious = clampedParagraphIndex > 0,
            canGoPreviousSentence = previousSentenceTarget(story, target) != null,
            canGoNextSentence = nextSentenceTarget(story, target) != null,
            isLastParagraph = clampedParagraphIndex >= paragraphCount - 1,
        )
    }

    private fun shouldAutoAdvanceAfterSentence(
        playbackStatus: ReadingPlaybackStatus,
        mode: ReadingSessionMode,
        autoContinue: Boolean,
    ): Boolean = playbackStatus == ReadingPlaybackStatus.Playing &&
        mode == ReadingSessionMode.ReadAlong &&
        autoContinue

    private fun nextSentenceTarget(story: Story, target: SentencePlaybackTarget): SentencePlaybackTarget? {
        var paragraphIndex = target.paragraphIndex
        var sentenceIndex = target.sentenceIndex + 1
        while (paragraphIndex < story.paragraphs.size) {
            val count = sentenceCount(story, paragraphIndex)
            if (count > 0 && sentenceIndex < count) {
                return SentencePlaybackTarget(paragraphIndex, sentenceIndex)
            }
            paragraphIndex += 1
            sentenceIndex = 0
        }
        return null
    }

    private fun previousSentenceTarget(story: Story, target: SentencePlaybackTarget): SentencePlaybackTarget? {
        if (target.sentenceIndex > 0) {
            return target.copy(sentenceIndex = target.sentenceIndex - 1)
        }
        var paragraphIndex = target.paragraphIndex - 1
        while (paragraphIndex >= 0) {
            val count = sentenceCount(story, paragraphIndex)
            if (count > 0) {
                return SentencePlaybackTarget(paragraphIndex, count - 1)
            }
            paragraphIndex -= 1
        }
        return null
    }

    private fun sentenceCount(story: Story, paragraphIndex: Int): Int =
        story.paragraphs.getOrNull(paragraphIndex)
            ?.let { SentenceSegmenter.segment(it.text).size }
            ?: 0

    private fun isStoryEnd(story: Story, state: ReadingSessionState): Boolean {
        if (state.currentSentence == null) return true
        return state.paragraphIndex >= story.paragraphs.lastIndex &&
            state.sentenceIndex >= state.segments.lastIndex
    }
}

class BuildSpeechTextUseCase {
    fun story(story: Story): String =
        story.paragraphs.joinToString(separator = "\n") { it.text }

    fun vocab(word: com.littlemandarin.classics.shared.story.Vocab): String =
        listOfNotNull(word.word, word.example).joinToString(separator = "。")
}
