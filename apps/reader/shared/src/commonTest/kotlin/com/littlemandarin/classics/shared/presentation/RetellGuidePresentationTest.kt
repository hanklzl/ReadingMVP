package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RetellGuidePresentationTest {
    private val useCases = RetellGuideUseCases()

    private fun story(vocab: List<Vocab>, retell: String = "说说发生了什么。"): Story = Story(
        id = "s1",
        titleZh = "故事",
        titleEn = "Story",
        level = 1,
        ageRange = "5-8",
        sourceNote = "public domain",
        paragraphs = listOf(Paragraph("正文。", "")),
        vocab = vocab,
        questions = listOf(Question("q", "single_choice", "?", listOf("A", "B"), "A", "")),
        retellPrompt = retell,
    )

    @Test
    fun guideUsesRetellPromptAndVocabCheckItemsCapped() {
        val vocab = (1..8).map { Vocab("词$it", "p$it", "mean$it") }
        val guide = useCases.buildGuide(story(vocab), maxItems = 5)

        assertEquals("s1", guide.storyId)
        assertEquals("说说发生了什么。", guide.prompt)
        assertEquals(5, guide.checkItems.size)
        assertEquals("词1", guide.checkItems.first().text)
        assertEquals("mean1", guide.checkItems.first().meaning)
    }

    @Test
    fun blankMeaningBecomesNull() {
        val guide = useCases.buildGuide(story(listOf(Vocab("词", "p", ""))))
        assertEquals(null, guide.checkItems.single().meaning)
    }

    @Test
    fun encouragementIsAlwaysPositiveAndTiered() {
        assertEquals(RetellEncouragement.JustStarted, useCases.encouragementFor(0, 5))
        assertEquals(RetellEncouragement.JustStarted, useCases.encouragementFor(1, 5)) // 0.2
        assertEquals(RetellEncouragement.GoodProgress, useCases.encouragementFor(2, 5)) // 0.4
        assertEquals(RetellEncouragement.GreatRetell, useCases.encouragementFor(5, 5)) // 1.0
        assertEquals(RetellEncouragement.JustStarted, useCases.encouragementFor(3, 0)) // no items
    }

    @Test
    fun emptyVocabYieldsEmptyChecklistButKeepsPrompt() {
        val guide = useCases.buildGuide(story(emptyList(), retell = "复述提示"))
        assertTrue(guide.checkItems.isEmpty())
        assertEquals("复述提示", guide.prompt)
    }
}
