package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.story.Story

/** A story referenced in the weekly plan (bilingual titles for the parent view). */
data class WeeklyPlanStory(
    val storyId: String,
    val titleZh: String,
    val titleEn: String,
)

/** A vocabulary word referenced in the weekly plan. */
data class WeeklyPlanWord(
    val word: String,
    val pinyin: String,
    val meaning: String,
)

/** A shareable, PII-free progress snapshot the parent can show off. */
data class WeeklyShareCard(
    val storiesThisWeek: Int,
    val wordsInNotebook: Int,
    val masteredWords: Int,
    val streakDays: Int,
)

/**
 * The parent weekly report upgraded from numbers to an action plan (competitive
 * Top-12 #9): what was read, which words are mastered vs weak, one sentence to
 * re-read, a weekend retell question, a shareable card, and one top suggestion.
 * Pure/deterministic; no child PII.
 */
data class ParentWeeklyPlan(
    val storiesReadThisWeek: List<WeeklyPlanStory>,
    val masteredWords: List<WeeklyPlanWord>,
    val weakWords: List<WeeklyPlanWord>,
    val rereadStoryId: String?,
    val rereadSentence: String?,
    val weekendRetellPrompt: String?,
    val shareCard: WeeklyShareCard,
    val topAdvice: ParentAdviceType,
)

/**
 * A minimal SRS view the weekly plan needs, decoupled from the persisted record so this
 * stays a pure presentation use case. Map `VocabSrsRecord` → this at the call site.
 */
data class WeeklyPlanWordState(
    val word: String,
    val pinyin: String,
    val meaning: String,
    val intervalDays: Int,
    val dueEpochDay: Int,
    val lapses: Int,
)

class ParentWeeklyPlanUseCases {
    fun buildWeeklyPlan(
        stories: List<Story>,
        completionRecords: List<CompletionRecord>,
        wordStates: List<WeeklyPlanWordState>,
        todayEpochDay: Int,
        nowEpochMillis: Long,
        streakDays: Int,
    ): ParentWeeklyPlan {
        val storiesById = stories.associateBy { it.id }

        val recordsThisWeek = completionRecords
            .filter { nowEpochMillis - it.completedAtEpochMillis in 0..WeekWindowMillis }
            .sortedByDescending { it.completedAtEpochMillis }

        val storiesReadThisWeek = recordsThisWeek
            .map { it.storyId }
            .distinct()
            .mapNotNull { id -> storiesById[id]?.let { WeeklyPlanStory(it.id, it.titleZh, it.titleEn) } }

        val mastered = wordStates
            .filter { it.intervalDays >= MasteredIntervalDays }
            .take(MaxListedWords)
            .map { WeeklyPlanWord(it.word, it.pinyin, it.meaning) }

        val masteredWordSet = wordStates.filter { it.intervalDays >= MasteredIntervalDays }.mapTo(mutableSetOf()) { it.word }
        val weak = wordStates
            .filter { it.word !in masteredWordSet && (it.dueEpochDay <= todayEpochDay || it.lapses > 0) }
            .sortedBy { it.dueEpochDay }
            .take(MaxListedWords)
            .map { WeeklyPlanWord(it.word, it.pinyin, it.meaning) }

        // Re-read the story the child found hardest this week (lowest quiz accuracy < 100%).
        val rereadRecord = recordsThisWeek
            .filter { it.questionCount > 0 && it.correctCount < it.questionCount }
            .minByOrNull { it.correctCount.toDouble() / it.questionCount.toDouble() }
        val rereadStory = rereadRecord?.let { storiesById[it.storyId] }
        val rereadSentence = rereadStory?.let { firstSentence(it) }

        val weekendRetellPrompt = storiesReadThisWeek
            .firstNotNullOfOrNull { storiesById[it.storyId]?.retellPrompt?.takeIf { p -> p.isNotBlank() } }

        val shareCard = WeeklyShareCard(
            storiesThisWeek = storiesReadThisWeek.size,
            wordsInNotebook = wordStates.size,
            masteredWords = masteredWordSet.size,
            streakDays = streakDays.coerceAtLeast(0),
        )

        val topAdvice = when {
            weak.isNotEmpty() -> ParentAdviceType.ReviewDueWords
            storiesReadThisWeek.isEmpty() -> ParentAdviceType.ReadTogetherToday
            rereadStory != null -> ParentAdviceType.RevisitRecentStory
            else -> ParentAdviceType.CelebrateStreak
        }

        return ParentWeeklyPlan(
            storiesReadThisWeek = storiesReadThisWeek,
            masteredWords = mastered,
            weakWords = weak,
            rereadStoryId = rereadStory?.id,
            rereadSentence = rereadSentence,
            weekendRetellPrompt = weekendRetellPrompt,
            shareCard = shareCard,
            topAdvice = topAdvice,
        )
    }

    private fun firstSentence(story: Story): String? {
        for (paragraph in story.paragraphs) {
            val builder = StringBuilder()
            for (char in paragraph.text) {
                builder.append(char)
                if (char == '。' || char == '！' || char == '？') break
            }
            val sentence = builder.toString()
            if (sentence.isNotBlank()) return sentence
        }
        return null
    }

    private companion object {
        const val WeekWindowMillis: Long = 7L * 24L * 60L * 60L * 1_000L
        const val MasteredIntervalDays = 7
        const val MaxListedWords = 5
    }
}
