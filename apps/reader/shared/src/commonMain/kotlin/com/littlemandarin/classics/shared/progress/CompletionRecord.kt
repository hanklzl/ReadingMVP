package com.littlemandarin.classics.shared.progress

import kotlinx.serialization.Serializable

@Serializable
data class CompletionRecord(
    val storyId: String,
    val completedAtEpochMillis: Long,
    val vocabCount: Int,
    val correctCount: Int,
    val questionCount: Int,
)

@Serializable
data class ProgressStats(
    val completedCount: Int,
    val vocabLearnedCount: Int,
    val correctCount: Int,
    val questionCount: Int,
    val averageCorrectPercent: Double,
)

@Serializable
data class ParentProgressReport(
    val storiesCompletedThisWeek: Int,
    val vocabLearnedThisWeek: Int,
    val averageCorrectPercent: Double,
    val recentCompletions: List<CompletionRecord>,
)
