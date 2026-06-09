package com.littlemandarin.classics.shared.feedback

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MaxSuggestionLength: Int = 1_000
private const val MaxParentContactLength: Int = 120

@Serializable
enum class FeedbackSatisfaction {
    @SerialName("very_satisfied")
    VerySatisfied,

    @SerialName("satisfied")
    Satisfied,

    @SerialName("neutral")
    Neutral,

    @SerialName("dissatisfied")
    Dissatisfied,
}

@Serializable
enum class FeedbackChildAgeBand {
    @SerialName("age_5_6")
    Age5To6,

    @SerialName("age_7_8")
    Age7To8,

    @SerialName("prefer_not_to_say")
    PreferNotToSay,
}

@Serializable
enum class FeedbackIssueType {
    @SerialName("content_too_hard")
    ContentTooHard,

    @SerialName("audio_issue")
    AudioIssue,

    @SerialName("ai_explain_issue")
    AiExplainIssue,

    @SerialName("bug")
    Bug,

    @SerialName("other")
    Other,
}

@Serializable
data class FeedbackSubmission(
    val satisfaction: FeedbackSatisfaction,
    @SerialName("child_age_band")
    val childAgeBand: FeedbackChildAgeBand,
    @SerialName("issue_type")
    val issueType: FeedbackIssueType,
    val suggestion: String,
    @SerialName("parent_contact")
    val parentContact: String? = null,
)

@Serializable
data class FeedbackEntry(
    @SerialName("feedback_id")
    val feedbackId: String,
    @SerialName("created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    val satisfaction: FeedbackSatisfaction,
    @SerialName("child_age_band")
    val childAgeBand: FeedbackChildAgeBand,
    @SerialName("issue_type")
    val issueType: FeedbackIssueType,
    val suggestion: String,
    @SerialName("parent_contact")
    val parentContact: String? = null,
)

interface FeedbackService {
    val feedback: Flow<List<FeedbackEntry>>

    suspend fun submit(submission: FeedbackSubmission): FeedbackEntry

    suspend fun getFeedback(): List<FeedbackEntry>

    suspend fun clear()
}

interface FeedbackRuntime {
    fun newUuid(): String

    fun nowEpochMillis(): Long
}

class InMemoryFeedbackService(
    private val runtime: FeedbackRuntime = PlatformFeedbackRuntime,
    initialFeedback: List<FeedbackEntry> = emptyList(),
) : FeedbackService {
    private val state = MutableStateFlow(initialFeedback)

    override val feedback: Flow<List<FeedbackEntry>> = state

    override suspend fun submit(submission: FeedbackSubmission): FeedbackEntry {
        val entry = submission.toEntry(runtime)
        state.value = state.value + entry
        return entry
    }

    override suspend fun getFeedback(): List<FeedbackEntry> = state.value

    override suspend fun clear() {
        state.value = emptyList()
    }
}

expect object PlatformFeedbackRuntime : FeedbackRuntime

expect fun createPlatformFeedbackService(): FeedbackService

internal interface FeedbackStore {
    fun readFeedback(): List<FeedbackEntry>

    fun writeFeedback(feedback: List<FeedbackEntry>)

    fun clearFeedback()
}

internal class StoredFeedbackService(
    private val store: FeedbackStore,
    private val runtime: FeedbackRuntime = PlatformFeedbackRuntime,
) : FeedbackService {
    private val state = MutableStateFlow(store.readFeedback())

    override val feedback: Flow<List<FeedbackEntry>> = state

    override suspend fun submit(submission: FeedbackSubmission): FeedbackEntry {
        val entry = submission.toEntry(runtime)
        val updatedFeedback = store.readFeedback() + entry
        store.writeFeedback(updatedFeedback)
        state.value = updatedFeedback
        return entry
    }

    override suspend fun getFeedback(): List<FeedbackEntry> {
        val latestFeedback = store.readFeedback()
        state.value = latestFeedback
        return latestFeedback
    }

    override suspend fun clear() {
        store.clearFeedback()
        state.value = emptyList()
    }
}

internal object FeedbackJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(feedback: List<FeedbackEntry>): String = json.encodeToString(feedback)

    fun decode(encodedFeedback: String?): List<FeedbackEntry> {
        if (encodedFeedback.isNullOrBlank()) return emptyList()

        return runCatching {
            json.decodeFromString<List<FeedbackEntry>>(encodedFeedback)
        }.getOrElse {
            emptyList()
        }
    }
}

private fun FeedbackSubmission.toEntry(runtime: FeedbackRuntime): FeedbackEntry {
    val normalizedSuggestion = suggestion.trim()
    require(normalizedSuggestion.isNotBlank()) {
        "feedback suggestion is required"
    }
    require(normalizedSuggestion.length <= MaxSuggestionLength) {
        "feedback suggestion must be $MaxSuggestionLength characters or fewer"
    }

    val normalizedParentContact = parentContact
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    require(normalizedParentContact == null || normalizedParentContact.length <= MaxParentContactLength) {
        "parent contact must be $MaxParentContactLength characters or fewer"
    }

    return FeedbackEntry(
        feedbackId = runtime.newUuid(),
        createdAtEpochMillis = runtime.nowEpochMillis(),
        satisfaction = satisfaction,
        childAgeBand = childAgeBand,
        issueType = issueType,
        suggestion = normalizedSuggestion,
        parentContact = normalizedParentContact,
    )
}
