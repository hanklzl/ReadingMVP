package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AssessmentPresentationTest {
    private val useCases = ReadingLevelAssessmentUseCases()

    @Test
    fun assessmentItemSelectionIsDeterministicAndBalancedAcrossAvailableLevels() {
        val stories = listOf(
            assessmentStory(id = "level-one-a", level = 1, words = listOf("桃园", "结义")),
            assessmentStory(id = "level-two-a", level = 2, words = listOf("草船", "借箭")),
            assessmentStory(id = "level-three-a", level = 3, words = listOf("空城", "妙计")),
            assessmentStory(id = "level-one-b", level = 1, words = listOf("勇敢", "帮助")),
        )

        val first = useCases.selectAssessmentItems(stories = stories, seed = 42, itemCount = 5)
        val second = useCases.selectAssessmentItems(stories = stories, seed = 42, itemCount = 5)
        val differentSeed = useCases.selectAssessmentItems(stories = stories, seed = 43, itemCount = 5)

        assertEquals(first, second)
        assertNotEquals(first.map { it.id }, differentSeed.map { it.id })
        assertEquals(5, first.size)
        assertTrue(first.map { it.targetLevel }.containsAll(listOf(1, 2, 3)))
        assertTrue(first.all { item -> item.options.size in 2..3 })
        assertTrue(first.all { item -> item.correctAnswer in item.options })
    }

    @Test
    fun assessmentScoringMapsAllCorrectAllWrongAndMixedToLevels() {
        val items = listOf(
            assessmentItem(id = "a", level = 1, answer = "one"),
            assessmentItem(id = "b", level = 2, answer = "two"),
            assessmentItem(id = "c", level = 3, answer = "three"),
        )

        val allCorrect = useCases.scoreAssessment(
            items = items,
            answersByItemId = mapOf("a" to "one", "b" to "two", "c" to "three"),
        )
        val allWrong = useCases.scoreAssessment(
            items = items,
            answersByItemId = mapOf("a" to "x", "b" to "y", "c" to "z"),
        )
        val mixed = useCases.scoreAssessment(
            items = items,
            answersByItemId = mapOf("a" to "one", "b" to "two", "c" to "z"),
        )

        assertEquals(3, allCorrect.level)
        assertEquals(3, allCorrect.correctCount)
        assertEquals(1, allWrong.level)
        assertEquals(0, allWrong.correctCount)
        assertEquals(2, mixed.level)
        assertEquals(2, mixed.correctCount)
    }

    @Test
    fun onboardingPreferencesDeserializeOldJsonAndAssessedLevelTakesPrecedence() {
        val oldJson = """
            {
              "completed": true,
              "skipped": false,
              "childAgeBand": "Age7To8",
              "language": "English",
              "dailyGoalStories": 2
            }
        """.trimIndent()

        val oldPreferences = OnboardingPreferencesJsonCodec.decode(oldJson)
        val assessedPreferences = oldPreferences.copy(assessedReadingLevel = 3)
        val invalidAssessment = OnboardingPreferencesJsonCodec.decode(
            """
                {
                  "completed": true,
                  "childAgeBand": "Age7To8",
                  "assessedReadingLevel": 99
                }
            """.trimIndent(),
        )

        assertEquals(null, oldPreferences.assessedReadingLevel)
        assertEquals(2, oldPreferences.recommendedLevel)
        assertEquals(3, assessedPreferences.recommendedLevel)
        assertEquals(null, invalidAssessment.assessedReadingLevel)
        assertEquals(2, invalidAssessment.recommendedLevel)
    }

    @Test
    fun recommenderChoosesNextIncompleteAtOrBelowLevelAndSuggestsReviewAmount() {
        val stories = listOf(
            assessmentStory(id = "level-one", level = 1),
            assessmentStory(id = "level-three", level = 3),
            assessmentStory(id = "level-two", level = 2),
        )
        val completed = listOf(completionRecord("level-one", correct = 3, total = 3))

        val recommendation = AdaptiveReadingPathRecommender().recommend(
            stories = stories,
            readingLevel = 2,
            completionRecords = completed,
            readingPositions = emptyMap(),
            dueVocabWordCount = 8,
        )

        assertEquals("level-two", recommendation.nextStory?.id)
        assertEquals(2, recommendation.readingLevel)
        assertEquals(5, recommendation.reviewWordCount)
    }

    @Test
    fun recommenderKeepsInProgressStoryAndStepsDownAfterRecentLowQuizAccuracy() {
        val stories = listOf(
            assessmentStory(id = "done", level = 1),
            assessmentStory(id = "in-progress", level = 3),
            assessmentStory(id = "review-level", level = 2),
        )
        val lowQuizRecords = listOf(
            completionRecord("done", correct = 1, total = 3),
        )

        val inProgressRecommendation = AdaptiveReadingPathRecommender().recommend(
            stories = stories,
            readingLevel = 3,
            completionRecords = lowQuizRecords,
            readingPositions = mapOf("in-progress" to 1),
            dueVocabWordCount = 2,
        )
        val reviewRecommendation = AdaptiveReadingPathRecommender().recommend(
            stories = stories,
            readingLevel = 3,
            completionRecords = lowQuizRecords,
            readingPositions = emptyMap(),
            dueVocabWordCount = 2,
        )

        assertEquals("in-progress", inProgressRecommendation.nextStory?.id)
        assertEquals("review-level", reviewRecommendation.nextStory?.id)
        assertEquals(2, reviewRecommendation.reviewWordCount)
    }

    @Test
    fun todaySelectionCanUseAdaptiveRecommendationWithoutBreakingFallback() {
        val useCases = StoryPresentationUseCases()
        val stories = listOf(
            assessmentStory(id = "first", level = 1),
            assessmentStory(id = "second", level = 3),
            assessmentStory(id = "third", level = 2),
        )

        val recommended = useCases.selectTodayStories(
            stories = stories,
            completedStoryIds = setOf("first"),
            recommendedStoryId = "third",
        )
        val fallback = useCases.selectTodayStories(
            stories = stories,
            completedStoryIds = setOf("first"),
            recommendedStoryId = "missing",
        )

        assertEquals("third", recommended.todayStory?.id)
        assertEquals(null, recommended.upNextStory)
        assertEquals("second", fallback.todayStory?.id)
    }
}

private fun assessmentItem(
    id: String,
    level: Int,
    answer: String,
): AssessmentItem = AssessmentItem(
    id = id,
    storyId = "story-$id",
    targetLevel = level,
    word = "词$id",
    pinyin = "ci $id",
    correctAnswer = answer,
    options = listOf(answer, "other"),
)

private fun assessmentStory(
    id: String,
    level: Int,
    words: List<String> = listOf("词一", "词二", "词三"),
): Story = Story(
    id = id,
    titleZh = "故事$id",
    titleEn = "Story $id",
    level = level,
    ageRange = "5-8",
    sourceNote = "public domain",
    paragraphs = listOf(Paragraph("桃园里大家一起想办法。", "tao yuan")),
    vocab = words.mapIndexed { index, word ->
        Vocab(
            word = word,
            pinyin = "pin $index",
            meaning = "meaning $id $index",
            example = "$word 在故事里出现。",
        )
    },
    questions = listOf(
        Question("q1", "single_choice", "谁一起想办法？", listOf("A", "B"), "A", "提示"),
        Question("q2", "single_choice", "在哪里？", listOf("A", "B"), "A", "提示"),
        Question("q3", "single_choice", "他们做什么？", listOf("A", "B"), "A", "提示"),
    ),
    retellPrompt = "说说大家怎样合作。",
)

private fun completionRecord(
    storyId: String,
    correct: Int,
    total: Int,
): CompletionRecord = CompletionRecord(
    storyId = storyId,
    completedAtEpochMillis = 1_800_000_000_000L,
    vocabCount = 6,
    correctCount = correct,
    questionCount = total,
)
