package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.feedback.FeedbackChildAgeBand
import com.littlemandarin.classics.shared.feedback.FeedbackIssueType
import com.littlemandarin.classics.shared.feedback.FeedbackSatisfaction
import com.littlemandarin.classics.shared.feedback.FeedbackSubmission

data class FeedbackOption<T>(
    val id: String,
    val value: T,
)

object FeedbackPresentationOptions {
    val satisfaction: List<FeedbackOption<FeedbackSatisfaction>> = listOf(
        FeedbackOption(id = "satisfied", value = FeedbackSatisfaction.Satisfied),
        FeedbackOption(id = "okay", value = FeedbackSatisfaction.Neutral),
        FeedbackOption(id = "not_satisfied", value = FeedbackSatisfaction.Dissatisfied),
    )

    val childAgeBands: List<FeedbackOption<FeedbackChildAgeBand>> = listOf(
        FeedbackOption(id = "5_6", value = FeedbackChildAgeBand.Age5To6),
        FeedbackOption(id = "7_8", value = FeedbackChildAgeBand.Age7To8),
        FeedbackOption(id = "other", value = FeedbackChildAgeBand.PreferNotToSay),
    )

    val issueTypes: List<FeedbackOption<FeedbackIssueType>> = listOf(
        FeedbackOption(id = "content", value = FeedbackIssueType.ContentTooHard),
        FeedbackOption(id = "audio", value = FeedbackIssueType.AudioIssue),
        FeedbackOption(id = "ai", value = FeedbackIssueType.AiExplainIssue),
        FeedbackOption(id = "progress", value = FeedbackIssueType.Other),
        FeedbackOption(id = "other", value = FeedbackIssueType.Other),
    )
}

class BuildFeedbackSubmissionUseCase {
    fun canSubmit(suggestion: String): Boolean =
        suggestion.trim().isNotBlank()

    operator fun invoke(
        satisfaction: FeedbackSatisfaction,
        childAgeBand: FeedbackChildAgeBand,
        issueType: FeedbackIssueType,
        suggestion: String,
        parentContact: String?,
    ): FeedbackSubmission {
        val trimmedContact = parentContact
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return FeedbackSubmission(
            satisfaction = satisfaction,
            childAgeBand = childAgeBand,
            issueType = issueType,
            suggestion = suggestion.trim(),
            parentContact = trimmedContact,
        )
    }
}
