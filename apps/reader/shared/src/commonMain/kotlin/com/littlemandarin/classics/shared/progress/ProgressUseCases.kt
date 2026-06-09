package com.littlemandarin.classics.shared.progress

class MarkStoryCompletedUseCase(
    private val progressService: ProgressService,
) {
    suspend operator fun invoke(record: CompletionRecord) {
        progressService.markCompleted(record)
    }
}

class GetProgressStatsUseCase(
    private val progressService: ProgressService,
) {
    suspend operator fun invoke(): ProgressStats {
        val records = progressService.getRecords()
        val correctCount = records.sumOf { it.correctCount }
        val questionCount = records.sumOf { it.questionCount }

        return ProgressStats(
            completedCount = records.size,
            vocabLearnedCount = records.sumOf { it.vocabCount },
            correctCount = correctCount,
            questionCount = questionCount,
            averageCorrectPercent = correctPercent(
                correctCount = correctCount,
                questionCount = questionCount,
            ),
        )
    }
}

class BuildParentReportUseCase(
    private val progressService: ProgressService,
) {
    suspend operator fun invoke(
        nowEpochMillis: Long,
        weekWindowMillis: Long = SevenDaysMillis,
    ): ParentProgressReport {
        val records = progressService.getRecords()
        val weekStartEpochMillis = nowEpochMillis - weekWindowMillis
        val recordsThisWeek = records.filter {
            it.completedAtEpochMillis in weekStartEpochMillis..nowEpochMillis
        }
        val questionCountThisWeek = recordsThisWeek.sumOf { it.questionCount }
        val correctCountThisWeek = recordsThisWeek.sumOf { it.correctCount }

        return ParentProgressReport(
            storiesCompletedThisWeek = recordsThisWeek.size,
            vocabLearnedThisWeek = recordsThisWeek.sumOf { it.vocabCount },
            averageCorrectPercent = correctPercent(
                correctCount = correctCountThisWeek,
                questionCount = questionCountThisWeek,
            ),
            recentCompletions = records.sortedByDescending { it.completedAtEpochMillis },
        )
    }

    private companion object {
        const val SevenDaysMillis: Long = 7L * 24L * 60L * 60L * 1_000L
    }
}

private fun correctPercent(
    correctCount: Int,
    questionCount: Int,
): Double {
    if (questionCount == 0) return 0.0

    return correctCount.toDouble() / questionCount.toDouble() * 100.0
}
