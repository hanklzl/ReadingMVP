package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Story

/** One self-check item for the retell: a key word/element from the story to mention. */
data class RetellCheckItem(
    val text: String,
    val meaning: String?,
)

/**
 * A story-grounded guide for the post-reading retell (competitive Top-12 #5). It is
 * NOT open AI chat and needs no ASR: it offers the retell prompt plus a low-pressure
 * "did you mention…?" self-check built from the story's own key words. The child
 * records a retell (reusing the voice-recording feature) and self-reports.
 */
data class RetellGuide(
    val storyId: String,
    val prompt: String,
    val checkItems: List<RetellCheckItem>,
)

/** Encouraging, non-punitive feedback tiers based on how many items the child self-checks. */
enum class RetellEncouragement {
    JustStarted,
    GoodProgress,
    GreatRetell,
}

class RetellGuideUseCases {
    fun buildGuide(story: Story, maxItems: Int = DefaultMaxItems): RetellGuide {
        val checkItems = story.vocab
            .filter { it.word.isNotBlank() }
            .distinctBy { it.word }
            .take(maxItems.coerceAtLeast(0))
            .map { RetellCheckItem(text = it.word, meaning = it.meaning.ifBlank { null }) }

        return RetellGuide(
            storyId = story.id,
            prompt = story.retellPrompt,
            checkItems = checkItems,
        )
    }

    /**
     * Map the count of self-checked items to an encouraging tier. Always positive —
     * even zero is "just getting started", never a failure.
     */
    fun encouragementFor(checkedCount: Int, totalItems: Int): RetellEncouragement {
        if (totalItems <= 0 || checkedCount <= 0) return RetellEncouragement.JustStarted
        val ratio = checkedCount.toDouble() / totalItems.toDouble()
        return when {
            ratio >= GreatThreshold -> RetellEncouragement.GreatRetell
            ratio >= GoodThreshold -> RetellEncouragement.GoodProgress
            else -> RetellEncouragement.JustStarted
        }
    }

    private companion object {
        const val DefaultMaxItems = 5
        const val GoodThreshold = 0.4
        const val GreatThreshold = 0.8
    }
}
