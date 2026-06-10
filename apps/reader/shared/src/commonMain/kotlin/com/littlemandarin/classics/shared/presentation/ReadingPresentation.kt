package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.StoryAudioManifest

enum class ReadingPlaybackStatus {
    Stopped,
    Playing,
    Paused,
}

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
    val progressFraction: Double,
    val canGoPrevious: Boolean,
    val isLastParagraph: Boolean,
)

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
    ): ReadingSessionState = stateFor(
        story = story,
        paragraphIndex = savedParagraphIndex,
        sentenceIndex = savedSentenceIndex,
        audioManifest = audioManifest,
    )

    fun previous(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionState = stateFor(
        story = story,
        paragraphIndex = state.paragraphIndex - 1,
        audioManifest = audioManifest,
    )

    fun next(
        story: Story,
        state: ReadingSessionState,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
    ): ReadingSessionTransition {
        val normalizedState = stateFor(
            story = story,
            paragraphIndex = state.paragraphIndex,
            audioManifest = audioManifest,
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
            ),
        )
    }

    fun start(state: ReadingSessionState): ReadingSessionState =
        if (state.currentSentence == null) {
            state.copy(playbackStatus = ReadingPlaybackStatus.Stopped)
        } else {
            state.copy(playbackStatus = ReadingPlaybackStatus.Playing)
        }

    fun pause(state: ReadingSessionState): ReadingSessionState =
        if (state.playbackStatus == ReadingPlaybackStatus.Playing) {
            state.copy(playbackStatus = ReadingPlaybackStatus.Paused)
        } else {
            state
        }

    fun stop(state: ReadingSessionState): ReadingSessionState =
        state.copy(playbackStatus = ReadingPlaybackStatus.Stopped)

    fun nextSentence(
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
        )
        val nextSentenceIndex = normalizedState.sentenceIndex + 1

        if (nextSentenceIndex < normalizedState.segments.size) {
            return ReadingSessionTransition(
                state = stateFor(
                    story = story,
                    paragraphIndex = normalizedState.paragraphIndex,
                    sentenceIndex = nextSentenceIndex,
                    audioManifest = audioManifest,
                    playbackStatus = normalizedState.playbackStatus,
                ),
            )
        }

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
                sentenceIndex = 0,
                audioManifest = audioManifest,
                playbackStatus = normalizedState.playbackStatus,
            ),
        )
    }

    fun stateFor(
        story: Story,
        paragraphIndex: Int,
        sentenceIndex: Int = 0,
        audioManifest: StoryAudioManifest = StoryAudioManifest.empty(story.id),
        playbackStatus: ReadingPlaybackStatus = ReadingPlaybackStatus.Stopped,
    ): ReadingSessionState {
        val paragraphCount = story.paragraphs.size.coerceAtLeast(1)
        val clampedIndex = paragraphIndex.coerceIn(0, paragraphCount - 1)
        val paragraph = story.paragraphs.getOrNull(clampedIndex)
        val segments = paragraph?.let { SentenceSegmenter.segment(it.text) }.orEmpty()
        val clampedSentenceIndex = if (segments.isEmpty()) {
            0
        } else {
            sentenceIndex.coerceIn(0, segments.lastIndex)
        }
        val audioSegment = audioManifest.segmentFor(
            paragraphIndex = clampedIndex,
            sentenceIndex = clampedSentenceIndex,
        )
        val progress = if (story.paragraphs.isEmpty()) {
            0.0
        } else {
            (clampedIndex + 1).toDouble() / paragraphCount.toDouble()
        }

        return ReadingSessionState(
            storyId = story.id,
            paragraphIndex = clampedIndex,
            sentenceIndex = clampedSentenceIndex,
            paragraphCount = paragraphCount,
            currentParagraph = paragraph,
            segments = segments,
            currentSentence = segments.getOrNull(clampedSentenceIndex),
            currentAudioResourcePath = audioSegment?.resourcePath,
            hasGeneratedAudio = audioSegment != null,
            playbackStatus = playbackStatus,
            progressFraction = progress.coerceIn(0.0, 1.0),
            canGoPrevious = clampedIndex > 0,
            isLastParagraph = clampedIndex >= paragraphCount - 1,
        )
    }

}

class BuildSpeechTextUseCase {
    fun story(story: Story): String =
        story.paragraphs.joinToString(separator = "\n") { it.text }

    fun vocab(word: com.littlemandarin.classics.shared.story.Vocab): String =
        listOfNotNull(word.word, word.example).joinToString(separator = "。")
}
