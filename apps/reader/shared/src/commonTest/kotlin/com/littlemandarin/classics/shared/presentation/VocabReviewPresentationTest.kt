package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.progress.InMemoryProgressService
import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.StoryRepository
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class VocabReviewPresentationTest {
    @Test
    fun schedulerQueuesNewWordsDueTodayAndMovesKnownWordsForward() {
        val today = 20L * DayMillis
        val scheduler = SrsScheduler()
        val newRecord = scheduler.newRecord(
            vocab = Vocab("结义", "jie yi", "to become sworn friends", "他们结义。"),
            storyId = "peach-garden-oath",
            todayEpochMillis = today,
        )

        assertTrue(scheduler.isDue(newRecord, todayEpochMillis = today))

        val firstKnown = scheduler.review(
            record = newRecord,
            assessment = VocabReviewAssessment.Known,
            todayEpochMillis = today,
        )
        val secondKnown = scheduler.review(
            record = firstKnown,
            assessment = VocabReviewAssessment.Known,
            todayEpochMillis = today + DayMillis,
        )

        assertEquals(1, firstKnown.intervalDays)
        assertEquals(21, firstKnown.dueEpochDay)
        assertEquals(1, firstKnown.reps)
        assertFalse(scheduler.isDue(firstKnown, todayEpochMillis = today))
        assertEquals(3, secondKnown.intervalDays)
        assertEquals(24, secondKnown.dueEpochDay)
        assertEquals(2, secondKnown.reps)
    }

    @Test
    fun schedulerKeepsPracticeWordsOnShortInterval() {
        val today = 20L * DayMillis
        val scheduler = SrsScheduler()
        val record = scheduler.review(
            record = scheduler.newRecord(
                vocab = Vocab("勇敢", "yong gan", "brave", "他很勇敢。"),
                storyId = "peach-garden-oath",
                todayEpochMillis = today,
            ),
            assessment = VocabReviewAssessment.NeedsPractice,
            todayEpochMillis = today,
        )

        assertEquals(1, record.intervalDays)
        assertEquals(21, record.dueEpochDay)
        assertEquals(0, record.reps)
        assertEquals(1, record.lapses)
        assertTrue(record.ease < SrsScheduler.DefaultEase)
    }

    @Test
    fun useCaseAddsCompletedStoryWordsAndPreservesExistingReviewState() = runTest {
        val today = 20L * DayMillis
        val repository = FakeStoryRepository(
            listOf(
                sampleStory(
                    id = "first",
                    vocab = listOf(
                        Vocab("结义", "jie yi", "to become sworn friends", "他们结义。"),
                        Vocab("勇敢", "yong gan", "brave", "他很勇敢。"),
                    ),
                ),
                sampleStory(
                    id = "second",
                    vocab = listOf(
                        Vocab("勇敢", "yong gan", "brave", "大家勇敢向前。"),
                        Vocab("计策", "ji ce", "plan", "他想到好计策。"),
                    ),
                ),
            ),
        )
        val progressService = InMemoryProgressService(
            initialRecords = listOf(
                completion("first", completedAt = today - DayMillis),
                completion("second", completedAt = today),
            ),
        )
        val reviewService = InMemoryVocabReviewService()
        val useCase = VocabReviewUseCase(
            storyRepository = repository,
            progressService = progressService,
            reviewService = reviewService,
        )

        useCase.syncLearnedWords(todayEpochMillis = today)
        useCase.review(
            word = "勇敢",
            assessment = VocabReviewAssessment.Known,
            todayEpochMillis = today,
        )
        progressService.markCompleted(completion("second", completedAt = today + DayMillis))
        useCase.syncLearnedWords(todayEpochMillis = today + DayMillis)

        val summary = useCase.wordBook(todayEpochMillis = today + DayMillis)
        val brave = summary.items.single { it.word == "勇敢" }

        assertEquals(3, summary.totalWords)
        assertEquals(listOf("first", "second"), brave.sourceStoryIds)
        assertEquals(1, brave.reps)
        assertEquals(3, summary.dueCount)
        assertEquals(listOf("结义", "勇敢", "计策"), summary.items.map { it.word })
    }

    @Test
    fun useCaseReturnsOnlyDueWordsForReviewSession() = runTest {
        val today = 20L * DayMillis
        val repository = FakeStoryRepository(
            listOf(
                sampleStory(
                    id = "first",
                    vocab = listOf(
                        Vocab("结义", "jie yi", "to become sworn friends", "他们结义。"),
                        Vocab("勇敢", "yong gan", "brave", "他很勇敢。"),
                    ),
                ),
            ),
        )
        val progressService = InMemoryProgressService(
            initialRecords = listOf(completion("first", completedAt = today)),
        )
        val reviewService = InMemoryVocabReviewService()
        val useCase = VocabReviewUseCase(repository, progressService, reviewService)

        useCase.syncLearnedWords(todayEpochMillis = today)
        useCase.review("结义", VocabReviewAssessment.Known, todayEpochMillis = today)

        val dueToday = useCase.dueReviewItems(todayEpochMillis = today)
        val dueTomorrow = useCase.dueReviewItems(todayEpochMillis = today + DayMillis)

        assertEquals(listOf("勇敢"), dueToday.map { it.word })
        assertEquals(listOf("勇敢", "结义"), dueTomorrow.map { it.word })
    }

    @Test
    fun wordBookReviewPromptStateHidesStartReviewWhenCaughtUp() {
        val dueSummary = WordBookSummary(
            totalWords = 3,
            dueCount = 2,
            items = emptyList(),
        )
        val caughtUpSummary = WordBookSummary(
            totalWords = 3,
            dueCount = 0,
            items = emptyList(),
        )
        val emptySummary = WordBookSummary(
            totalWords = 0,
            dueCount = 0,
            items = emptyList(),
        )

        assertEquals(WordBookReviewPromptType.ReadyForReview, dueSummary.reviewPromptState().type)
        assertTrue(dueSummary.reviewPromptState().showStartReview)
        assertEquals(WordBookReviewPromptType.CaughtUp, caughtUpSummary.reviewPromptState().type)
        assertFalse(caughtUpSummary.reviewPromptState().showStartReview)
        assertTrue(caughtUpSummary.reviewPromptState().showReadToday)
        assertEquals(WordBookReviewPromptType.Empty, emptySummary.reviewPromptState().type)
        assertFalse(emptySummary.reviewPromptState().showStartReview)
    }
}

private class FakeStoryRepository(
    private val stories: List<Story>,
) : StoryRepository {
    override suspend fun listStories(): List<Story> = stories

    override suspend fun getStory(id: String): Story? = stories.firstOrNull { it.id == id }
}

private fun sampleStory(
    id: String,
    vocab: List<Vocab>,
): Story = Story(
    id = id,
    titleZh = "桃园结义",
    titleEn = "Oath in the Peach Garden",
    level = 1,
    ageRange = "5-8",
    sourceNote = "public domain",
    paragraphs = listOf(Paragraph("桃园里，三个人说好一起做正直勇敢的事。", "tao yuan")),
    vocab = vocab,
    questions = listOf(
        Question("q1", "single_choice", "谁说好一起做事？", listOf("A", "B", "C"), "A", "他们互相帮助。"),
        Question("q2", "single_choice", "他们想做什么？", listOf("A", "B", "C"), "B", "他们想做正直的事。"),
        Question("q3", "single_choice", "故事强调什么？", listOf("A", "B", "C"), "C", "朋友之间要合作。"),
    ),
    retellPrompt = "说说三个人为什么要合作。",
)

private fun completion(
    storyId: String,
    completedAt: Long,
): CompletionRecord = CompletionRecord(
    storyId = storyId,
    completedAtEpochMillis = completedAt,
    vocabCount = 2,
    correctCount = 3,
    questionCount = 3,
)

private const val DayMillis: Long = 24L * 60L * 60L * 1_000L
