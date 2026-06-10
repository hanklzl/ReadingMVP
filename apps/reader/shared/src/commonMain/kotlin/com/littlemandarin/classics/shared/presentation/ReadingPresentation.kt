package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Story

data class ReadingSessionState(
    val storyId: String,
    val paragraphIndex: Int,
    val paragraphCount: Int,
    val currentParagraph: Paragraph?,
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
    ): ReadingSessionState = stateFor(
        story = story,
        paragraphIndex = savedParagraphIndex,
    )

    fun previous(
        story: Story,
        state: ReadingSessionState,
    ): ReadingSessionState = stateFor(
        story = story,
        paragraphIndex = state.paragraphIndex - 1,
    )

    fun next(
        story: Story,
        state: ReadingSessionState,
    ): ReadingSessionTransition {
        val normalizedState = stateFor(
            story = story,
            paragraphIndex = state.paragraphIndex,
        )
        if (normalizedState.isLastParagraph) {
            return ReadingSessionTransition(
                state = normalizedState,
                shouldOpenVocabulary = true,
            )
        }

        return ReadingSessionTransition(
            state = stateFor(
                story = story,
                paragraphIndex = normalizedState.paragraphIndex + 1,
            ),
        )
    }

    fun stateFor(
        story: Story,
        paragraphIndex: Int,
    ): ReadingSessionState {
        val paragraphCount = story.paragraphs.size.coerceAtLeast(1)
        val clampedIndex = paragraphIndex.coerceIn(0, paragraphCount - 1)
        val progress = if (story.paragraphs.isEmpty()) {
            0.0
        } else {
            (clampedIndex + 1).toDouble() / paragraphCount.toDouble()
        }

        return ReadingSessionState(
            storyId = story.id,
            paragraphIndex = clampedIndex,
            paragraphCount = paragraphCount,
            currentParagraph = story.paragraphs.getOrNull(clampedIndex),
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
