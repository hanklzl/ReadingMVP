package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AbilityMapPresentationTest {
    private val useCases = AbilityMapUseCases()

    private fun story(
        id: String,
        vocab: List<Vocab> = listOf(Vocab("词", "cí", "word")),
        questions: List<Question> = listOf(Question("q", "single_choice", "?", listOf("A", "B"), "A", "")),
        retell: String = "复述",
        paragraphs: List<Paragraph> = listOf(Paragraph("大家一起。", "")),
    ): Story = Story(
        id = id,
        titleZh = "故事$id",
        titleEn = "Story $id",
        level = 1,
        ageRange = "5-8",
        sourceNote = "Based on public-domain 《三国演义》.",
        paragraphs = paragraphs,
        vocab = vocab,
        questions = questions,
        retellPrompt = retell,
    )

    @Test
    fun fullStoryExercisesAllDimensions() {
        val abilities = useCases.storyAbilities(story("s1"))
        assertEquals(AbilityDimension.entries.toSet(), abilities)
    }

    @Test
    fun storyWithoutQuestionsOrRetellDropsThoseDimensions() {
        val abilities = useCases.storyAbilities(
            story("s2", questions = emptyList(), retell = "  "),
        )
        assertTrue(AbilityDimension.Comprehension !in abilities)
        assertTrue(AbilityDimension.Retelling !in abilities)
        assertTrue(AbilityDimension.CharacterRecognition in abilities)
    }

    @Test
    fun masteryIsCoverageOfCompletedExercisingStories() {
        val stories = listOf(story("a"), story("b"), story("c"))
        val records = listOf(
            CompletionRecord("a", 1L, vocabCount = 1, correctCount = 2, questionCount = 3),
        )

        val map = useCases.buildAbilityMap(stories, records)

        val comprehension = map.dimensions.first { it.dimension == AbilityDimension.Comprehension }
        assertEquals(3, comprehension.totalStories)
        assertEquals(1, comprehension.practicedStories)
        assertEquals(1.0 / 3.0, comprehension.masteryFraction, 1e-9)
    }

    @Test
    fun recentlyPracticedReflectsLastStory() {
        val stories = listOf(story("a"), story("b", questions = emptyList()))
        val map = useCases.buildAbilityMap(
            stories,
            completionRecords = listOf(CompletionRecord("b", 1L, 1, 0, 0)),
            recentSessionStoryId = "b",
        )
        assertTrue(AbilityDimension.Comprehension !in map.recentlyPracticed)
        assertTrue(AbilityDimension.CharacterRecognition in map.recentlyPracticed)
    }

    @Test
    fun comprehensionAccuracyAggregatesAcrossRecordsAndIsNullWhenNoQuestions() {
        val stories = listOf(story("a"), story("b"))
        val withQuiz = useCases.buildAbilityMap(
            stories,
            listOf(
                CompletionRecord("a", 1L, 1, correctCount = 2, questionCount = 3),
                CompletionRecord("b", 2L, 1, correctCount = 3, questionCount = 3),
            ),
        )
        assertEquals(5.0 / 6.0, withQuiz.comprehensionAccuracy!!, 1e-9)

        val noQuiz = useCases.buildAbilityMap(
            stories,
            listOf(CompletionRecord("a", 1L, 1, correctCount = 0, questionCount = 0)),
        )
        assertNull(noQuiz.comprehensionAccuracy)
    }
}
