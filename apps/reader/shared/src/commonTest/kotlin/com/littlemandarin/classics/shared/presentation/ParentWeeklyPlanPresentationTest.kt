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

class ParentWeeklyPlanPresentationTest {
    private val useCases = ParentWeeklyPlanUseCases()

    private val dayMillis = 24L * 60L * 60L * 1_000L
    private val now = 1_000L * dayMillis // arbitrary "now"
    private val todayEpochDay = 1_000

    private fun story(id: String, retell: String = "复述$id"): Story = Story(
        id = id,
        titleZh = "故事$id",
        titleEn = "Story $id",
        level = 1,
        ageRange = "5-8",
        sourceNote = "public domain",
        paragraphs = listOf(Paragraph("第一句。第二句。", "")),
        vocab = listOf(Vocab("词", "cí", "word")),
        questions = listOf(Question("q", "single_choice", "?", listOf("A", "B"), "A", "")),
        retellPrompt = retell,
    )

    private fun word(name: String, interval: Int, due: Int, lapses: Int = 0) =
        WeeklyPlanWordState(name, "$name-py", "$name-mean", intervalDays = interval, dueEpochDay = due, lapses = lapses)

    @Test
    fun storiesThisWeekFilterByWindowDistinctAndTitled() {
        val stories = listOf(story("a"), story("b"), story("c"))
        val records = listOf(
            CompletionRecord("a", now - 2 * dayMillis, 1, 3, 3),
            CompletionRecord("a", now - 1 * dayMillis, 1, 3, 3), // duplicate story → distinct
            CompletionRecord("b", now - 30 * dayMillis, 1, 3, 3), // outside 7-day window
        )

        val plan = useCases.buildWeeklyPlan(stories, records, emptyList(), todayEpochDay, now, streakDays = 3)

        assertEquals(listOf("a"), plan.storiesReadThisWeek.map { it.storyId })
        assertEquals("故事a", plan.storiesReadThisWeek.first().titleZh)
        assertEquals(1, plan.shareCard.storiesThisWeek)
    }

    @Test
    fun masteredVsWeakWordClassification() {
        val stories = listOf(story("a"))
        val records = listOf(CompletionRecord("a", now, 1, 3, 3))
        val words = listOf(
            word("强", interval = 14, due = todayEpochDay + 14), // mastered
            word("弱", interval = 1, due = todayEpochDay),       // due → weak
            word("败", interval = 3, due = todayEpochDay + 5, lapses = 2), // lapsed → weak
        )

        val plan = useCases.buildWeeklyPlan(stories, records, words, todayEpochDay, now, streakDays = 1)

        assertEquals(listOf("强"), plan.masteredWords.map { it.word })
        assertEquals(setOf("弱", "败"), plan.weakWords.map { it.word }.toSet())
        assertEquals(1, plan.shareCard.masteredWords)
        assertEquals(3, plan.shareCard.wordsInNotebook)
        assertEquals(ParentAdviceType.ReviewDueWords, plan.topAdvice)
    }

    @Test
    fun rereadPicksLowestAccuracyStoryAndNullWhenAllPerfect() {
        val stories = listOf(story("a"), story("b"))
        val mixed = listOf(
            CompletionRecord("a", now - dayMillis, 1, correctCount = 3, questionCount = 3), // 100%
            CompletionRecord("b", now, 1, correctCount = 1, questionCount = 3),             // 33% → reread
        )
        val plan = useCases.buildWeeklyPlan(stories, mixed, emptyList(), todayEpochDay, now, streakDays = 1)
        assertEquals("b", plan.rereadStoryId)
        assertEquals("第一句。", plan.rereadSentence)

        val allPerfect = listOf(CompletionRecord("a", now, 1, 3, 3))
        val perfectPlan = useCases.buildWeeklyPlan(stories, allPerfect, emptyList(), todayEpochDay, now, streakDays = 1)
        assertNull(perfectPlan.rereadStoryId)
        assertNull(perfectPlan.rereadSentence)
    }

    @Test
    fun weekendRetellComesFromMostRecentStoryThisWeek() {
        val stories = listOf(story("a", retell = "讲讲A"), story("b", retell = "讲讲B"))
        val records = listOf(
            CompletionRecord("a", now - 3 * dayMillis, 1, 3, 3),
            CompletionRecord("b", now - 1 * dayMillis, 1, 3, 3), // most recent
        )
        val plan = useCases.buildWeeklyPlan(stories, records, emptyList(), todayEpochDay, now, streakDays = 1)
        assertEquals("讲讲B", plan.weekendRetellPrompt)
    }

    @Test
    fun adviceFallsBackToReadTogetherWhenNothingThisWeek() {
        val stories = listOf(story("a"))
        val plan = useCases.buildWeeklyPlan(stories, emptyList(), emptyList(), todayEpochDay, now, streakDays = 0)
        assertEquals(ParentAdviceType.ReadTogetherToday, plan.topAdvice)
        assertTrue(plan.storiesReadThisWeek.isEmpty())
        assertEquals(0, plan.shareCard.streakDays)
    }
}
