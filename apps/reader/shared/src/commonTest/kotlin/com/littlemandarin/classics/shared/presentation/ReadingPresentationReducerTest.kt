package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.StoryAudioManifest
import com.littlemandarin.classics.shared.story.StoryAudioSegment
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingPresentationReducerTest {
    @Test
    fun readAlongDefaultsToAutoContinueDefaultSlowAndRecordedSource() {
        val story = sampleReducerStory("桃园开花。三人说：好！")
        val manifest = StoryAudioManifest(
            storyId = story.id,
            segments = listOf(
                StoryAudioSegment(
                    paragraphIndex = 0,
                    sentenceIndex = 0,
                    text = "桃园开花。",
                    resourcePath = "stories/sample/audio/p1_s1.wav",
                ),
            ),
        )
        val reducer = ReadingSessionReducer()

        val initial = reducer.initialState(story, 0, manifest)
        val playing = reducer.playReadAlong(story, initial, manifest)
        val advanced = reducer.sentenceFinished(story, playing, manifest)

        assertEquals(ReadingSessionMode.ReadAlong, initial.playbackMode)
        assertTrue(initial.autoContinue)
        assertEquals(ReadingPlaybackSpeed.DefaultSlow, initial.playbackSpeed)
        assertEquals(0.9, initial.playbackSpeed.multiplier)
        assertEquals("default_slow", initial.playbackSpeedBucket)
        assertEquals(ReadingAudioSource.Recorded, initial.audioSource)
        assertTrue(playing.shouldAutoAdvanceAfterSentence)
        assertEquals(ReadingPlaybackStatus.Playing, advanced.state.playbackStatus)
        assertEquals(1, advanced.state.sentenceIndex)
        assertFalse(advanced.shouldOpenVocabulary)
    }

    @Test
    fun tapToListenAndRepeatStopOnCurrentSentence() {
        val story = sampleReducerStory("桃园开花。三人说：好！")
        val reducer = ReadingSessionReducer()
        val tapMode = reducer.setPlaybackMode(
            reducer.initialState(story, 0),
            ReadingSessionMode.TapToListen,
        )

        val tapped = reducer.playSentence(story, tapMode, paragraphIndex = 0, sentenceIndex = 1)
        val tappedDone = reducer.sentenceFinished(story, tapped)
        val repeated = reducer.repeatSentence(story, reducer.stateFor(story, 0, 1))
        val repeatedDone = reducer.sentenceFinished(story, repeated)

        assertEquals(ReadingSessionMode.TapToListen, tapped.playbackMode)
        assertFalse(tapped.shouldAutoAdvanceAfterSentence)
        assertEquals(ReadingPlaybackStatus.Stopped, tappedDone.state.playbackStatus)
        assertEquals(1, tappedDone.state.sentenceIndex)
        assertFalse(tappedDone.shouldOpenVocabulary)
        assertFalse(repeated.shouldAutoAdvanceAfterSentence)
        assertEquals(1, repeatedDone.state.sentenceIndex)
    }

    @Test
    fun autoContinueOffStopsWithoutAdvancing() {
        val story = sampleReducerStory("桃园开花。三人说：好！")
        val reducer = ReadingSessionReducer()
        val state = reducer.setAutoContinue(reducer.initialState(story, 0), enabled = false)

        val playing = reducer.playReadAlong(story, state)
        val finished = reducer.sentenceFinished(story, playing)

        assertFalse(playing.shouldAutoAdvanceAfterSentence)
        assertEquals(ReadingPlaybackStatus.Stopped, finished.state.playbackStatus)
        assertEquals(0, finished.state.sentenceIndex)
        assertFalse(finished.shouldOpenVocabulary)
    }

    @Test
    fun nextAndPreviousSentenceCrossParagraphs() {
        val story = sampleReducerStory(
            firstParagraphText = "桃园开花。三人说：好！",
            secondParagraphText = "大家点头。一起出发。",
        )
        val reducer = ReadingSessionReducer()
        val lastFirstParagraph = reducer.stateFor(story, paragraphIndex = 0, sentenceIndex = 1)

        val next = reducer.nextSentence(story, lastFirstParagraph).state
        val previous = reducer.previousSentence(story, next).state

        assertEquals(1, next.paragraphIndex)
        assertEquals(0, next.sentenceIndex)
        assertEquals(0, previous.paragraphIndex)
        assertEquals(1, previous.sentenceIndex)
    }
}

private fun sampleReducerStory(
    firstParagraphText: String,
    secondParagraphText: String = "大家点头。",
): Story = Story(
    id = "sample",
    titleZh = "桃园结义",
    titleEn = "Oath in the Peach Garden",
    level = 1,
    ageRange = "5-8",
    sourceNote = "public domain",
    paragraphs = listOf(
        Paragraph(firstParagraphText, "tao yuan"),
        Paragraph(secondParagraphText, "da jia"),
    ),
    vocab = listOf(
        Vocab("结义", "jie yi", "to become sworn friends", "他们结义。"),
    ),
    questions = listOf(
        Question("q1", "single_choice", "谁说好一起做事？", listOf("A", "B", "C"), "A", "他们要互相帮助。"),
        Question("q2", "single_choice", "他们想做什么？", listOf("A", "B", "C"), "B", "故事说他们想做正直的事。"),
        Question("q3", "single_choice", "这个故事强调什么？", listOf("A", "B", "C"), "C", "朋友之间要合作。"),
    ),
    retellPrompt = "说说三个人为什么要合作。",
)
