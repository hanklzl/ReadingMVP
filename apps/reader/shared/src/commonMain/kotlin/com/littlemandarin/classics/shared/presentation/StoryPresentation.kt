package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.progress.ParentProgressReport
import com.littlemandarin.classics.shared.progress.ProgressStats
import com.littlemandarin.classics.shared.story.Story

enum class TodayStorySelectionPolicy {
    CatalogFirst,
    FirstIncomplete,
}

enum class StoryProgressStatus {
    NotStarted,
    InProgress,
    Completed,
}

data class StoryProgress(
    val storyId: String,
    val fraction: Double,
    val status: StoryProgressStatus,
)

data class TodayStories(
    val todayStory: Story?,
    val upNextStory: Story?,
)

data class ParentReportSummary(
    val storiesCompletedThisWeek: Int,
    val readingDaysThisWeek: Int,
    val vocabLearnedThisWeek: Int,
    val averageCorrectPercent: Double,
    val correctCount: Int,
    val questionCount: Int,
)

class StoryPresentationUseCases {
    fun selectTodayStories(
        stories: List<Story>,
        completedStoryIds: Set<String>,
        policy: TodayStorySelectionPolicy = TodayStorySelectionPolicy.FirstIncomplete,
    ): TodayStories {
        if (stories.isEmpty()) return TodayStories(null, null)

        val today = when (policy) {
            TodayStorySelectionPolicy.CatalogFirst -> stories.first()
            TodayStorySelectionPolicy.FirstIncomplete ->
                stories.firstOrNull { it.id !in completedStoryIds } ?: stories.first()
        }
        val todayIndex = stories.indexOfFirst { it.id == today.id }.coerceAtLeast(0)
        val upNext = stories.drop(todayIndex + 1).firstOrNull()

        return TodayStories(
            todayStory = today,
            upNextStory = upNext,
        )
    }

    fun storyProgress(
        story: Story,
        completedStoryIds: Set<String>,
        savedParagraphIndex: Int,
    ): StoryProgress {
        if (story.id in completedStoryIds) {
            return StoryProgress(
                storyId = story.id,
                fraction = 1.0,
                status = StoryProgressStatus.Completed,
            )
        }
        if (story.paragraphs.isEmpty() || savedParagraphIndex < 0) {
            return StoryProgress(
                storyId = story.id,
                fraction = 0.0,
                status = StoryProgressStatus.NotStarted,
            )
        }

        val fraction = ((savedParagraphIndex + 1).toDouble() / story.paragraphs.size.toDouble())
            .coerceIn(0.0, 0.99)
        return StoryProgress(
            storyId = story.id,
            fraction = fraction,
            status = if (fraction > 0.0) StoryProgressStatus.InProgress else StoryProgressStatus.NotStarted,
        )
    }

    fun completedStoryIds(records: List<CompletionRecord>): Set<String> =
        records.mapTo(mutableSetOf()) { it.storyId }

    fun filterStoriesByLevel(
        stories: List<Story>,
        selectedLevel: Int?,
    ): List<Story> =
        selectedLevel?.let { level -> stories.filter { it.level == level } } ?: stories

    fun canOpenQuiz(
        story: Story,
        completedStoryIds: Set<String>,
    ): Boolean = story.id in completedStoryIds
}

class BuildParentReportSummaryUseCase {
    operator fun invoke(
        report: ParentProgressReport,
        stats: ProgressStats,
        nowEpochMillis: Long,
        weekWindowMillis: Long = SevenDaysMillis,
    ): ParentReportSummary {
        val weekStartEpochMillis = nowEpochMillis - weekWindowMillis
        val recordsThisWeek = report.recentCompletions.filter {
            it.completedAtEpochMillis in weekStartEpochMillis..nowEpochMillis
        }
        val readingDaysThisWeek = recordsThisWeek
            .map { it.completedAtEpochMillis.floorEpochDay() }
            .distinct()
            .size

        return ParentReportSummary(
            storiesCompletedThisWeek = report.storiesCompletedThisWeek,
            readingDaysThisWeek = readingDaysThisWeek,
            vocabLearnedThisWeek = report.vocabLearnedThisWeek,
            averageCorrectPercent = report.averageCorrectPercent,
            correctCount = stats.correctCount,
            questionCount = stats.questionCount,
        )
    }

    private fun Long.floorEpochDay(): Long = this / DayMillis

    private companion object {
        const val DayMillis: Long = 24L * 60L * 60L * 1_000L
        const val SevenDaysMillis: Long = 7L * DayMillis
    }
}
