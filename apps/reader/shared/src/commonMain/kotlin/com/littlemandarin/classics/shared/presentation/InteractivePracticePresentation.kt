package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Story
import kotlin.random.Random

/** A matched pair for the word↔meaning game. */
data class PracticeMatchPair(
    val word: String,
    val meaning: String,
)

/**
 * Low-pressure interactive practice generated from existing story content (paragraphs +
 * vocab) — no story-schema or content change. Used for an optional "play more" round
 * after the quiz. All items are deterministic for a given seed.
 */
sealed interface InteractivePracticeItem {
    val id: String

    /** Put the shuffled sentences back into the order they appear in the story. */
    data class SentenceOrdering(
        override val id: String,
        val shuffled: List<String>,
        val correctOrder: List<String>,
    ) : InteractivePracticeItem

    /** Match each word to its English meaning. */
    data class WordMatching(
        override val id: String,
        val pairs: List<PracticeMatchPair>,
    ) : InteractivePracticeItem

    /** Choose the word that fills the blank in a sentence from the story. */
    data class Cloze(
        override val id: String,
        val sentenceBefore: String,
        val sentenceAfter: String,
        val options: List<String>,
        val answer: String,
    ) : InteractivePracticeItem
}

class InteractivePracticeUseCases {

    /** Generate up to one of each available practice type for the story, deterministically. */
    fun generate(
        story: Story,
        seed: Int,
        maxItems: Int = DefaultMaxItems,
    ): List<InteractivePracticeItem> {
        if (maxItems <= 0) return emptyList()
        val random = Random(seed)
        val items = mutableListOf<InteractivePracticeItem>()

        buildSentenceOrdering(story, random)?.let { items.add(it) }
        buildWordMatching(story, random)?.let { items.add(it) }
        buildCloze(story, random)?.let { items.add(it) }

        return items.take(maxItems)
    }

    fun scoreOrdering(item: InteractivePracticeItem.SentenceOrdering, submitted: List<String>): Boolean =
        submitted == item.correctOrder

    fun scoreMatching(item: InteractivePracticeItem.WordMatching, submitted: Map<String, String>): Boolean =
        item.pairs.isNotEmpty() && item.pairs.all { submitted[it.word] == it.meaning }

    fun scoreCloze(item: InteractivePracticeItem.Cloze, submitted: String): Boolean =
        submitted == item.answer

    private fun buildSentenceOrdering(
        story: Story,
        random: Random,
    ): InteractivePracticeItem.SentenceOrdering? {
        val paragraph = story.paragraphs
            .map { splitSentences(it.text) }
            .filter { it.size in 2..MaxOrderingSentences }
            .maxByOrNull { it.size } ?: return null

        var shuffled = paragraph.shuffled(random)
        // Avoid handing back the already-correct order.
        var attempts = 0
        while (shuffled == paragraph && attempts < 5) {
            shuffled = paragraph.shuffled(random)
            attempts++
        }
        if (shuffled == paragraph) return null

        return InteractivePracticeItem.SentenceOrdering(
            id = "ordering@${story.id}",
            shuffled = shuffled,
            correctOrder = paragraph,
        )
    }

    private fun buildWordMatching(
        story: Story,
        random: Random,
    ): InteractivePracticeItem.WordMatching? {
        val usable = story.vocab.filter { it.word.isNotBlank() && it.meaning.isNotBlank() }
        if (usable.size < 2) return null
        val pairs = usable.shuffled(random)
            .take(MaxMatchPairs)
            .map { PracticeMatchPair(it.word, it.meaning) }
        return InteractivePracticeItem.WordMatching(id = "matching@${story.id}", pairs = pairs)
    }

    private fun buildCloze(
        story: Story,
        random: Random,
    ): InteractivePracticeItem.Cloze? {
        val usable = story.vocab.map { it.word }.filter { it.isNotBlank() }.distinct()
        if (usable.size < 3) return null

        val fullText = story.paragraphs.joinToString("") { it.text }
        val target = usable.shuffled(random).firstOrNull { fullText.contains(it) } ?: return null

        val sentence = splitSentences(fullText).firstOrNull { it.contains(target) } ?: return null
        val cut = sentence.indexOf(target)
        val before = sentence.substring(0, cut)
        val after = sentence.substring(cut + target.length)

        val distractors = usable.filter { it != target }.shuffled(random).take(2)
        val options = (listOf(target) + distractors).shuffled(random)

        return InteractivePracticeItem.Cloze(
            id = "cloze@${story.id}",
            sentenceBefore = before,
            sentenceAfter = after,
            options = options,
            answer = target,
        )
    }

    private fun splitSentences(text: String): List<String> {
        val sentences = mutableListOf<String>()
        val builder = StringBuilder()
        for (char in text) {
            builder.append(char)
            if (char == '。' || char == '！' || char == '？') {
                sentences.add(builder.toString())
                builder.clear()
            }
        }
        if (builder.isNotBlank()) sentences.add(builder.toString())
        return sentences.filter { it.isNotBlank() }
    }

    private companion object {
        const val DefaultMaxItems = 3
        const val MaxOrderingSentences = 5
        const val MaxMatchPairs = 4
    }
}
