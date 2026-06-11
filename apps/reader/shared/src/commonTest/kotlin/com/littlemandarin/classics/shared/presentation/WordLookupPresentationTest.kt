package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WordLookupPresentationTest {
    private val useCases = WordLookupUseCase()

    private fun storyWith(vocab: List<Vocab>): Story = Story(
        id = "s1",
        titleZh = "故事",
        titleEn = "Story",
        level = 1,
        ageRange = "5-8",
        sourceNote = "public domain",
        paragraphs = listOf(Paragraph("大家一起想办法，最后成功了。", "")),
        vocab = vocab,
        questions = listOf(Question("q1", "single_choice", "?", listOf("A", "B"), "A", "")),
        retellPrompt = "复述",
    )

    private val vocab = listOf(
        Vocab("办法", "bàn fǎ", "method", example = "想个办法。"),
        Vocab("成功", "chéng gōng", "success"),
    )

    @Test
    fun exactMatchReturnsCuratedEntry() {
        val result = useCases.lookup(storyWith(vocab), "办法")
        assertIs<WordLookupResult.Curated>(result)
        assertEquals("办法", result.word)
        assertEquals("bàn fǎ", result.pinyin)
        assertEquals("method", result.meaning)
        assertEquals("想个办法。", result.example)
        assertEquals("s1", result.sourceStoryId)
    }

    @Test
    fun singleCharWithinVocabWordResolvesToThatWord() {
        val result = useCases.lookup(storyWith(vocab), "办")
        assertIs<WordLookupResult.Curated>(result)
        assertEquals("办法", result.word)
    }

    @Test
    fun longestRelatedVocabWordWins() {
        val withOverlap = listOf(
            Vocab("成", "chéng", "become"),
            Vocab("成功", "chéng gōng", "success"),
        )
        // Exact "成" would match the single-char entry, so probe a span that contains both.
        val result = useCases.lookup(storyWith(withOverlap), "成功了")
        assertIs<WordLookupResult.Curated>(result)
        assertEquals("成功", result.word) // longest (len 2) beats "成" (len 1)
    }

    @Test
    fun noCuratedMatchNeedsAi() {
        val result = useCases.lookup(storyWith(vocab), "龙")
        assertIs<WordLookupResult.NeedsAi>(result)
        assertEquals("龙", result.token)
    }

    @Test
    fun blankTokenNeedsAiWithEmptyToken() {
        val result = useCases.lookup(storyWith(vocab), "   ")
        assertIs<WordLookupResult.NeedsAi>(result)
        assertEquals("", result.token)
    }
}
