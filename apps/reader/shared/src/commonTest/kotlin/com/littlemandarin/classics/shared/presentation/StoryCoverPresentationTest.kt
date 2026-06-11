package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoryCoverPresentationTest {
    private val useCases = StoryCoverUseCases()

    private fun story(id: String, level: Int = 1): Story = Story(
        id = id,
        titleZh = "故事",
        titleEn = "Story",
        level = level,
        ageRange = "5-8",
        sourceNote = "public domain",
        paragraphs = listOf(Paragraph("正文。", "")),
        vocab = listOf(Vocab("词", "cí", "word")),
        questions = listOf(Question("q", "single_choice", "?", listOf("A", "B"), "A", "")),
        retellPrompt = "复述",
    )

    @Test
    fun themeIsDeterministicForSameId() {
        val a = useCases.coverThemeFor(story("peach-garden-oath"))
        val b = useCases.coverThemeFor(story("peach-garden-oath"))
        assertEquals(a, b)
    }

    @Test
    fun paletteIndexIsWithinRange() {
        val ids = listOf(
            "peach-garden-oath", "red-cliffs", "empty-fort", "borrow-east-wind",
            "wooden-ox-flowing-horse", "debate-scholars", "huarong-path",
        )
        ids.forEach { id ->
            val theme = useCases.coverThemeFor(story(id), paletteCount = 6)
            assertTrue(theme.paletteIndex in 0..5, "$id paletteIndex ${theme.paletteIndex} out of range")
        }
    }

    @Test
    fun curatedMotifUsedForKnownStoryFallbackOtherwise() {
        assertEquals("🌸", useCases.coverThemeFor(story("peach-garden-oath")).motif)
        assertEquals("📖", useCases.coverThemeFor(story("unknown-story-id")).motif)
    }

    @Test
    fun levelCarriedThrough() {
        assertEquals(3, useCases.coverThemeFor(story("debate-scholars", level = 3)).level)
    }

    @Test
    fun paletteCountOneAlwaysIndexZero() {
        assertEquals(0, useCases.coverThemeFor(story("any"), paletteCount = 1).paletteIndex)
    }
}
