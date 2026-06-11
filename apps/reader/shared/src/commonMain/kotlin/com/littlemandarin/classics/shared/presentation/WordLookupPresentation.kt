package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Story

/**
 * Result of looking up a tapped word/character in the reading text.
 *
 * [Curated] comes from the story's reviewed vocabulary (the trustworthy, content-pipeline
 * layer). [NeedsAi] means there is no curated entry, so the UI may offer the existing
 * controlled `explain_word` AI path — clearly labeled as AI (AGENTS.md §7, no open chat).
 */
sealed interface WordLookupResult {
    data class Curated(
        val word: String,
        val pinyin: String,
        val meaning: String,
        val example: String?,
        val sourceStoryId: String,
    ) : WordLookupResult

    data class NeedsAi(val token: String) : WordLookupResult
}

/**
 * Resolve a tapped token against the story's curated vocabulary first; fall back to AI
 * only when nothing curated matches. Pure and deterministic — no side effects, no network.
 */
class WordLookupUseCase {
    fun lookup(story: Story, token: String): WordLookupResult {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return WordLookupResult.NeedsAi("")

        // 1) Exact curated match wins outright.
        story.vocab.firstOrNull { it.word == trimmed }?.let { return it.toCurated(story.id) }

        // 2) Otherwise the most specific (longest) related curated word: a vocab word that
        //    contains the tapped token (single char within a word), or that the token
        //    contains (selected a span that includes a vocab word).
        val best = story.vocab
            .filter { vocab ->
                vocab.word.isNotBlank() &&
                    (vocab.word.contains(trimmed) || trimmed.contains(vocab.word))
            }
            .maxByOrNull { it.word.length }

        return best?.toCurated(story.id) ?: WordLookupResult.NeedsAi(trimmed)
    }

    private fun com.littlemandarin.classics.shared.story.Vocab.toCurated(
        storyId: String,
    ): WordLookupResult.Curated = WordLookupResult.Curated(
        word = word,
        pinyin = pinyin,
        meaning = meaning,
        example = example,
        sourceStoryId = storyId,
    )
}
