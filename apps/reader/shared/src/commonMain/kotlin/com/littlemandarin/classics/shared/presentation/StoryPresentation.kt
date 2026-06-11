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
    val advice: ParentAdvice = ParentAdvice(ParentAdviceType.ReadTogetherToday),
)

enum class LibraryStoryStatus {
    New,
    Continue,
    Done,
}

enum class LibraryStoryAction {
    Start,
    Continue,
    ReadAgain,
}

data class LibraryStoryCard(
    val story: Story,
    val sequenceNumber: Int,
    val seriesKey: String,
    val status: LibraryStoryStatus,
    val action: LibraryStoryAction,
    val progress: StoryProgress,
)

enum class ParentAdviceType {
    ReadTogetherToday,
    ReviewDueWords,
    RevisitRecentStory,
    TryNextStory,
    CelebrateStreak,
}

data class ParentAdvice(
    val type: ParentAdviceType,
    val storyId: String? = null,
    val dueWordCount: Int = 0,
)

class StoryPresentationUseCases {
    fun selectTodayStories(
        stories: List<Story>,
        completedStoryIds: Set<String>,
        policy: TodayStorySelectionPolicy = TodayStorySelectionPolicy.FirstIncomplete,
        recommendedStoryId: String? = null,
    ): TodayStories {
        if (stories.isEmpty()) return TodayStories(null, null)

        // An adaptive recommendation (when it names an existing, incomplete story) wins
        // over the catalog policy and is presented as the single focus for today.
        recommendedStoryId?.let { id ->
            stories.firstOrNull { it.id == id && it.id !in completedStoryIds }?.let { recommended ->
                return TodayStories(todayStory = recommended, upNextStory = null)
            }
        }

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

    fun libraryStoryCards(
        stories: List<Story>,
        completedStoryIds: Set<String>,
        readingPositions: Map<String, Int>,
        seriesKey: String = DefaultSeriesKey,
    ): List<LibraryStoryCard> =
        stories.mapIndexed { index, story ->
            val progress = storyProgress(
                story = story,
                completedStoryIds = completedStoryIds,
                savedParagraphIndex = readingPositions[story.id] ?: -1,
            )
            val status = when (progress.status) {
                StoryProgressStatus.Completed -> LibraryStoryStatus.Done
                StoryProgressStatus.InProgress -> LibraryStoryStatus.Continue
                StoryProgressStatus.NotStarted -> LibraryStoryStatus.New
            }
            LibraryStoryCard(
                story = story,
                sequenceNumber = index + 1,
                seriesKey = seriesKey,
                status = status,
                action = when (status) {
                    LibraryStoryStatus.Done -> LibraryStoryAction.ReadAgain
                    LibraryStoryStatus.Continue -> LibraryStoryAction.Continue
                    LibraryStoryStatus.New -> LibraryStoryAction.Start
                },
                progress = progress,
            )
        }

    fun canOpenQuiz(
        story: Story,
        completedStoryIds: Set<String>,
    ): Boolean = story.id in completedStoryIds

    private companion object {
        const val DefaultSeriesKey = "series_three_kingdoms"
    }
}

class BuildParentReportSummaryUseCase {
    operator fun invoke(
        report: ParentProgressReport,
        stats: ProgressStats,
        nowEpochMillis: Long,
        weekWindowMillis: Long = SevenDaysMillis,
        dueWordCount: Int = 0,
        nextStoryId: String? = null,
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
            advice = parentAdvice(
                report = report,
                stats = stats,
                dueWordCount = dueWordCount,
                nextStoryId = nextStoryId,
            ),
        )
    }

    private fun Long.floorEpochDay(): Long = this / DayMillis

    private fun parentAdvice(
        report: ParentProgressReport,
        stats: ProgressStats,
        dueWordCount: Int,
        nextStoryId: String?,
    ): ParentAdvice {
        if (dueWordCount > 0) {
            return ParentAdvice(
                type = ParentAdviceType.ReviewDueWords,
                dueWordCount = dueWordCount,
            )
        }
        if (report.storiesCompletedThisWeek == 0) {
            return ParentAdvice(ParentAdviceType.ReadTogetherToday)
        }
        if (stats.questionCount > 0 && stats.averageCorrectPercent < LowQuizAccuracyPercent) {
            return ParentAdvice(
                type = ParentAdviceType.RevisitRecentStory,
                storyId = report.recentCompletions.firstOrNull()?.storyId,
            )
        }
        if (nextStoryId != null) {
            return ParentAdvice(
                type = ParentAdviceType.TryNextStory,
                storyId = nextStoryId,
            )
        }
        if (report.storiesCompletedThisWeek >= StreakCelebrationStoryCount) {
            return ParentAdvice(ParentAdviceType.CelebrateStreak)
        }
        return ParentAdvice(ParentAdviceType.ReadTogetherToday)
    }

    private companion object {
        const val DayMillis: Long = 24L * 60L * 60L * 1_000L
        const val SevenDaysMillis: Long = 7L * DayMillis
        const val LowQuizAccuracyPercent: Double = 70.0
        const val StreakCelebrationStoryCount: Int = 3
    }
}
