package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.story.Story

/**
 * The Chinese-ability dimensions a classic story exercises. Derived from existing
 * story.json content (no new authored data) so the child sees structured progress and
 * the parent sees "what was practised today".
 */
enum class AbilityDimension {
    CharacterRecognition, // 识字
    WordMeaning,          // 词义
    SentenceReading,      // 句读
    Listening,            // 听读
    Comprehension,        // 理解
    Retelling,            // 复述
    Culture,              // 文化
}

/** Per-dimension progress: how many exercising stories are completed, and a mastery 0..1. */
data class AbilityProgress(
    val dimension: AbilityDimension,
    val practicedStories: Int,
    val totalStories: Int,
    val masteryFraction: Double,
)

/**
 * Aggregated ability map plus the dimensions practised in the most recent session
 * (for a parent "today you practised …" line).
 */
data class AbilityMap(
    val dimensions: List<AbilityProgress>,
    val recentlyPracticed: Set<AbilityDimension>,
    val comprehensionAccuracy: Double?,
)

class AbilityMapUseCases {

    /** Which ability dimensions a single story exercises, from its content shape. */
    fun storyAbilities(story: Story): Set<AbilityDimension> {
        val dimensions = mutableSetOf<AbilityDimension>()
        val hasText = story.paragraphs.any { it.text.isNotBlank() }
        if (hasText) {
            dimensions.add(AbilityDimension.CharacterRecognition)
            dimensions.add(AbilityDimension.SentenceReading)
            dimensions.add(AbilityDimension.Listening) // per-sentence narration accompanies the text
        }
        if (story.vocab.isNotEmpty()) {
            dimensions.add(AbilityDimension.WordMeaning)
            // Recognition is anchored by the curated vocabulary too.
            dimensions.add(AbilityDimension.CharacterRecognition)
        }
        if (story.questions.isNotEmpty()) dimensions.add(AbilityDimension.Comprehension)
        if (story.retellPrompt.isNotBlank()) dimensions.add(AbilityDimension.Retelling)
        if (story.sourceNote.isNotBlank()) dimensions.add(AbilityDimension.Culture)
        return dimensions
    }

    /**
     * Build the ability map across [stories] given [completionRecords]. Mastery per
     * dimension is coverage: completed exercising stories / all exercising stories.
     * [recentSessionStoryId] (the last story finished) drives "recently practised".
     */
    fun buildAbilityMap(
        stories: List<Story>,
        completionRecords: List<CompletionRecord>,
        recentSessionStoryId: String? = null,
    ): AbilityMap {
        val completedIds = completionRecords.mapTo(mutableSetOf()) { it.storyId }
        val abilitiesByStory = stories.associate { it.id to storyAbilities(it) }

        val dimensions = AbilityDimension.entries.map { dimension ->
            val exercising = stories.filter { dimension in abilitiesByStory.getValue(it.id) }
            val practiced = exercising.count { it.id in completedIds }
            val total = exercising.size
            AbilityProgress(
                dimension = dimension,
                practicedStories = practiced,
                totalStories = total,
                masteryFraction = if (total == 0) 0.0 else practiced.toDouble() / total.toDouble(),
            )
        }

        val recentlyPracticed = recentSessionStoryId
            ?.let { id -> abilitiesByStory[id] }
            ?: emptySet()

        val comprehensionRecords = completionRecords.filter { it.questionCount > 0 }
        val comprehensionAccuracy = if (comprehensionRecords.isEmpty()) {
            null
        } else {
            val correct = comprehensionRecords.sumOf { it.correctCount }
            val asked = comprehensionRecords.sumOf { it.questionCount }
            if (asked == 0) null else correct.toDouble() / asked.toDouble()
        }

        return AbilityMap(
            dimensions = dimensions,
            recentlyPracticed = recentlyPracticed,
            comprehensionAccuracy = comprehensionAccuracy,
        )
    }
}
