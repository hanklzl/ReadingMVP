package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.story.Story
import kotlin.random.Random

/**
 * A single placement-check item. Drawn from existing story vocabulary so no new
 * authored content is needed. This is a *reading-level preference* signal only —
 * never child PII (see AGENTS.md §7). [targetLevel] mirrors the source story level.
 */
data class AssessmentItem(
    val id: String,
    val storyId: String,
    val targetLevel: Int,
    val word: String,
    val pinyin: String,
    val correctAnswer: String,
    val options: List<String>,
)

/** Result of scoring a placement check: a reading level in 1..3 plus raw correct count. */
data class AssessedLevel(
    val level: Int,
    val correctCount: Int,
)

/**
 * Builds and scores the onboarding placement check. Pure and seedable so selection is
 * deterministic in tests and reproducible on a device for a given seed.
 */
class ReadingLevelAssessmentUseCases {

    /**
     * Pick a balanced, deterministic set of [itemCount] items spanning every available
     * story level. Same [seed] → identical result; different seed → different ordering.
     */
    fun selectAssessmentItems(
        stories: List<Story>,
        seed: Int,
        itemCount: Int = DefaultItemCount,
    ): List<AssessmentItem> {
        val candidates = stories.flatMap { story ->
            story.vocab
                .filter { it.word.isNotBlank() && it.meaning.isNotBlank() }
                .map { vocab -> Candidate(story, vocab.word, vocab.pinyin, vocab.meaning) }
        }
        if (candidates.isEmpty() || itemCount <= 0) return emptyList()

        val random = Random(seed)
        val allMeanings = candidates.map { it.meaning }.distinct()

        // One candidate per available level first (balance), then fill from the rest.
        val byLevel = candidates.groupBy { it.story.level }
        val chosen = LinkedHashSet<Candidate>()
        byLevel.keys.sorted().forEach { level ->
            byLevel.getValue(level).shuffled(random).firstOrNull()?.let { chosen.add(it) }
        }
        candidates.shuffled(random).forEach { candidate ->
            if (chosen.size >= itemCount) return@forEach
            chosen.add(candidate)
        }

        return chosen
            .take(itemCount)
            .shuffled(random)
            .map { candidate -> candidate.toItem(allMeanings, random) }
    }

    /**
     * Map answers to a reading level. Weighted by accuracy ratio so it scales with item
     * count: high ratio → 3, mid → 2, low → 1.
     */
    fun scoreAssessment(
        items: List<AssessmentItem>,
        answersByItemId: Map<String, String>,
    ): AssessedLevel {
        if (items.isEmpty()) return AssessedLevel(level = 1, correctCount = 0)

        val correctCount = items.count { item -> answersByItemId[item.id] == item.correctAnswer }
        val ratio = correctCount.toDouble() / items.size.toDouble()
        val level = when {
            ratio >= HighAccuracyThreshold -> 3
            ratio >= MidAccuracyThreshold -> 2
            else -> 1
        }
        return AssessedLevel(level = level, correctCount = correctCount)
    }

    private data class Candidate(
        val story: Story,
        val word: String,
        val pinyin: String,
        val meaning: String,
    ) {
        fun toItem(allMeanings: List<String>, random: Random): AssessmentItem {
            val distractors = allMeanings
                .filter { it != meaning }
                .shuffled(random)
                .take(2)
            val options = (listOf(meaning) + distractors).shuffled(random)
            return AssessmentItem(
                id = "$word@${story.id}",
                storyId = story.id,
                targetLevel = story.level,
                word = word,
                pinyin = pinyin,
                correctAnswer = meaning,
                options = options,
            )
        }
    }

    private companion object {
        const val DefaultItemCount = 5
        const val HighAccuracyThreshold = 0.8
        const val MidAccuracyThreshold = 0.4
    }
}

/** Recommendation for the Today screen: which story to read next and how much to review. */
data class ReadingPathRecommendation(
    val nextStory: Story?,
    val readingLevel: Int,
    val reviewWordCount: Int,
)

/**
 * Recommends the next story and a gentle review amount from the user's reading level,
 * completion history, in-progress positions and due-vocab count. Pure and unit-tested.
 *
 * Rules:
 *  - An in-progress (started, not completed) story is always resumed first.
 *  - Recent low quiz accuracy steps the effective difficulty down one level.
 *  - Otherwise pick the first incomplete story at or below the effective level.
 *  - Review amount is capped so it never overwhelms a young reader.
 */
class AdaptiveReadingPathRecommender {
    fun recommend(
        stories: List<Story>,
        readingLevel: Int,
        completionRecords: List<CompletionRecord>,
        readingPositions: Map<String, Int>,
        dueVocabWordCount: Int,
    ): ReadingPathRecommendation {
        val reviewWordCount = dueVocabWordCount.coerceIn(0, MaxReviewWords)
        val completedIds = completionRecords.mapTo(mutableSetOf()) { it.storyId }

        // 1) Resume an in-progress story before anything else.
        val inProgress = stories.firstOrNull { story ->
            story.id !in completedIds &&
                (readingPositions[story.id] ?: -1) >= 0
        }
        if (inProgress != null) {
            return ReadingPathRecommendation(inProgress, readingLevel, reviewWordCount)
        }

        // 2) Step difficulty down after recent low accuracy.
        val effectiveLevel = if (recentlyStruggled(completionRecords)) {
            (readingLevel - 1).coerceAtLeast(1)
        } else {
            readingLevel
        }

        // 3) First incomplete at or below the effective level; else first incomplete overall.
        val incomplete = stories.filter { it.id !in completedIds }
        val nextStory = incomplete.firstOrNull { it.level <= effectiveLevel }
            ?: incomplete.firstOrNull()

        return ReadingPathRecommendation(nextStory, readingLevel, reviewWordCount)
    }

    private fun recentlyStruggled(records: List<CompletionRecord>): Boolean {
        val mostRecent = records.maxByOrNull { it.completedAtEpochMillis } ?: return false
        if (mostRecent.questionCount <= 0) return false
        val accuracy = mostRecent.correctCount.toDouble() / mostRecent.questionCount.toDouble()
        return accuracy < StruggleAccuracyThreshold
    }

    private companion object {
        const val MaxReviewWords = 5
        const val StruggleAccuracyThreshold = 0.5
    }
}
