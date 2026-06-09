package com.littlemandarin.classics.shared.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ProgressReportTest {
    @Test
    fun storedProgressServiceReadsLatestRecordsAcrossInstances() = runTest {
        val store = FakeCompletionRecordStore()
        val writer = StoredProgressService(store)
        val reader = StoredProgressService(store)
        val record = CompletionRecord(
            storyId = "peach-garden-oath",
            completedAtEpochMillis = 1_800_000_000_000L,
            vocabCount = 6,
            correctCount = 3,
            questionCount = 3,
        )

        writer.markCompleted(record)

        assertEquals(listOf(record), reader.getRecords())
        assertEquals(1, reader.completedCount())
    }

    @Test
    fun parentReportAggregatesThisWeekVocabAccuracyAndRecentCompletions() = runTest {
        val nowEpochMillis = 1_800_000_000_000L
        val oneDayMillis = 24L * 60L * 60L * 1_000L
        val progressService = InMemoryProgressService(
            initialRecords = listOf(
                CompletionRecord(
                    storyId = "peach-garden-oath",
                    completedAtEpochMillis = nowEpochMillis - oneDayMillis,
                    vocabCount = 6,
                    correctCount = 3,
                    questionCount = 3,
                ),
                CompletionRecord(
                    storyId = "borrow-arrows-boats",
                    completedAtEpochMillis = nowEpochMillis - (2L * oneDayMillis),
                    vocabCount = 5,
                    correctCount = 2,
                    questionCount = 3,
                ),
                CompletionRecord(
                    storyId = "three-visits-cottage",
                    completedAtEpochMillis = nowEpochMillis - (10L * oneDayMillis),
                    vocabCount = 8,
                    correctCount = 1,
                    questionCount = 3,
                ),
            ),
        )

        MarkStoryCompletedUseCase(progressService).invoke(
            CompletionRecord(
                storyId = "empty-fort",
                completedAtEpochMillis = nowEpochMillis,
                vocabCount = 5,
                correctCount = 2,
                questionCount = 3,
            ),
        )

        val report = BuildParentReportUseCase(progressService).invoke(
            nowEpochMillis = nowEpochMillis,
        )

        assertEquals(3, report.storiesCompletedThisWeek)
        assertEquals(16, report.vocabLearnedThisWeek)
        assertEquals(77.7777777778, report.averageCorrectPercent, 0.0001)
        assertEquals("empty-fort", report.recentCompletions.first().storyId)
        assertEquals(
            listOf("empty-fort", "peach-garden-oath", "borrow-arrows-boats", "three-visits-cottage"),
            report.recentCompletions.map { it.storyId },
        )
    }
}

private class FakeCompletionRecordStore : CompletionRecordStore {
    private var records: List<CompletionRecord> = emptyList()

    override fun readRecords(): List<CompletionRecord> = records

    override fun writeRecords(records: List<CompletionRecord>) {
        this.records = records
    }

    override fun clearRecords() {
        records = emptyList()
    }
}
