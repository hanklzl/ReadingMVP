package com.littlemandarin.classics.shared.feedback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FeedbackServiceTest {
    @Test
    fun codecRoundTripsParentFeedbackWithoutTreatingContactAsChildData() {
        val feedback = listOf(
            FeedbackEntry(
                feedbackId = "feedback-1",
                createdAtEpochMillis = 1_800_000_000_000L,
                satisfaction = FeedbackSatisfaction.Satisfied,
                childAgeBand = FeedbackChildAgeBand.Age5To6,
                issueType = FeedbackIssueType.AudioIssue,
                suggestion = "Audio stopped after the second paragraph.",
                parentContact = "parent@example.com",
            ),
        )

        val encoded = FeedbackJsonCodec.encode(feedback)
        val decoded = FeedbackJsonCodec.decode(encoded)

        assertTrue(encoded.contains("\"child_age_band\":\"age_5_6\""))
        assertTrue(encoded.contains("\"parent_contact\":\"parent@example.com\""))
        assertEquals(feedback, decoded)
    }

    @Test
    fun storedFeedbackServicePersistsValidatedEntriesAcrossInstances() = runTest {
        val store = FakeFeedbackStore()
        val writer = StoredFeedbackService(
            store = store,
            runtime = FixedFeedbackRuntime(),
        )

        val entry = writer.submit(
            FeedbackSubmission(
                satisfaction = FeedbackSatisfaction.Neutral,
                childAgeBand = FeedbackChildAgeBand.Age7To8,
                issueType = FeedbackIssueType.ContentTooHard,
                suggestion = "  Please add a slower audio speed.  ",
                parentContact = "  parent@example.com  ",
            ),
        )

        val reader = StoredFeedbackService(
            store = store,
            runtime = FixedFeedbackRuntime(),
        )

        assertEquals("feedback-1", entry.feedbackId)
        assertEquals(1_800_000_000_000L, entry.createdAtEpochMillis)
        assertEquals("Please add a slower audio speed.", entry.suggestion)
        assertEquals("parent@example.com", entry.parentContact)
        assertEquals(listOf(entry), reader.getFeedback())
    }

    @Test
    fun feedbackValidationRejectsBlankOrOverlongSuggestionAndNormalizesBlankContact() = runTest {
        val service = InMemoryFeedbackService(runtime = FixedFeedbackRuntime())

        assertFailsWith<IllegalArgumentException> {
            service.submit(
                FeedbackSubmission(
                    satisfaction = FeedbackSatisfaction.Dissatisfied,
                    childAgeBand = FeedbackChildAgeBand.Age5To6,
                    issueType = FeedbackIssueType.Bug,
                    suggestion = " ",
                    parentContact = null,
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            service.submit(
                FeedbackSubmission(
                    satisfaction = FeedbackSatisfaction.Dissatisfied,
                    childAgeBand = FeedbackChildAgeBand.Age5To6,
                    issueType = FeedbackIssueType.Bug,
                    suggestion = "a".repeat(1_001),
                    parentContact = null,
                ),
            )
        }

        val entry = service.submit(
            FeedbackSubmission(
                satisfaction = FeedbackSatisfaction.Satisfied,
                childAgeBand = FeedbackChildAgeBand.PreferNotToSay,
                issueType = FeedbackIssueType.Other,
                suggestion = "Looks good.",
                parentContact = " ",
            ),
        )

        assertNull(entry.parentContact)
    }
}

private class FixedFeedbackRuntime : FeedbackRuntime {
    override fun newUuid(): String = "feedback-1"

    override fun nowEpochMillis(): Long = 1_800_000_000_000L
}

private class FakeFeedbackStore : FeedbackStore {
    private var feedback: List<FeedbackEntry> = emptyList()

    override fun readFeedback(): List<FeedbackEntry> = feedback

    override fun writeFeedback(feedback: List<FeedbackEntry>) {
        this.feedback = feedback
    }

    override fun clearFeedback() {
        feedback = emptyList()
    }
}
