package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class InteractivePracticePresentationTest {
    private val useCases = InteractivePracticeUseCases()

    private fun story(): Story = Story(
        id = "s1",
        titleZh = "故事",
        titleEn = "Story",
        level = 1,
        ageRange = "5-8",
        sourceNote = "public domain",
        paragraphs = listOf(
            Paragraph("第一句话。第二句话。第三句话。", ""),
        ),
        vocab = listOf(
            Vocab("第一", "dì yī", "first"),
            Vocab("句话", "jù huà", "sentence"),
            Vocab("故事", "gù shì", "story"),
        ),
        questions = listOf(Question("q", "single_choice", "?", listOf("A", "B"), "A", "")),
        retellPrompt = "复述",
    )

    @Test
    fun generateIsDeterministicBySeed() {
        val a = useCases.generate(story(), seed = 7)
        val b = useCases.generate(story(), seed = 7)
        assertEquals(a, b)
        assertTrue(a.isNotEmpty())
    }

    @Test
    fun orderingShufflesButScoresAgainstOriginalOrder() {
        val item = useCases.generate(story(), seed = 7)
            .filterIsInstance<InteractivePracticeItem.SentenceOrdering>()
            .first()

        assertEquals(listOf("第一句话。", "第二句话。", "第三句话。"), item.correctOrder)
        assertNotEquals(item.correctOrder, item.shuffled) // genuinely shuffled
        assertEquals(item.correctOrder.toSet(), item.shuffled.toSet()) // same sentences
        assertTrue(useCases.scoreOrdering(item, item.correctOrder))
        assertFalse(useCases.scoreOrdering(item, item.shuffled.takeIf { it != item.correctOrder } ?: item.correctOrder.reversed()))
    }

    @Test
    fun matchingScoresAllPairsCorrect() {
        val item = useCases.generate(story(), seed = 7)
            .filterIsInstance<InteractivePracticeItem.WordMatching>()
            .first()

        val allCorrect = item.pairs.associate { it.word to it.meaning }
        assertTrue(useCases.scoreMatching(item, allCorrect))

        val oneWrong = allCorrect.toMutableMap().apply {
            this[item.pairs.first().word] = "wrong-meaning"
        }
        assertFalse(useCases.scoreMatching(item, oneWrong))
    }

    @Test
    fun clozeBlanksAWordAndScores() {
        val item = useCases.generate(story(), seed = 7)
            .filterIsInstance<InteractivePracticeItem.Cloze>()
            .firstOrNull() ?: return // cloze requires >=3 vocab present in text

        assertTrue(item.answer in item.options)
        assertFalse((item.sentenceBefore + item.sentenceAfter).contains(item.answer))
        assertTrue(useCases.scoreCloze(item, item.answer))
        assertFalse(useCases.scoreCloze(item, "错误"))
    }

    @Test
    fun degenerateStoryProducesNoCrashAndFewerItems() {
        val tiny = story().copy(
            paragraphs = listOf(Paragraph("只有一句。", "")),
            vocab = listOf(Vocab("一句", "yī jù", "one sentence")),
        )
        val items = useCases.generate(tiny, seed = 1)
        // No multi-sentence paragraph and <2 vocab → no ordering/matching/cloze.
        assertTrue(items.none { it is InteractivePracticeItem.SentenceOrdering })
        assertTrue(items.none { it is InteractivePracticeItem.WordMatching })
    }
}
