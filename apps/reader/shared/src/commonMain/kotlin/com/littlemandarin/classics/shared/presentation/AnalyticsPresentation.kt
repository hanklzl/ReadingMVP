package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.analytics.AnalyticsEventName
import com.littlemandarin.classics.shared.analytics.AnalyticsProperties
import com.littlemandarin.classics.shared.quiz.QuizScore
import com.littlemandarin.classics.shared.story.Story
import kotlinx.serialization.json.JsonElement

data class AnalyticsEventPayload(
    val eventName: AnalyticsEventName,
    val properties: Map<String, JsonElement>,
)

object ReaderAnalyticsEvents {
    fun appOpen(
        openType: String,
        isFirstOpen: Boolean? = null,
        daysSinceFirstOpen: Int? = null,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.AppOpen,
        properties = buildMap {
            put("open_type", AnalyticsProperties.string(openType))
            isFirstOpen?.let { put("is_first_open", AnalyticsProperties.boolean(it)) }
            daysSinceFirstOpen?.let { put("days_since_first_open", AnalyticsProperties.int(it)) }
        },
    )

    fun storyOpen(
        story: Story,
        storyOrder: Int,
        openSource: String,
        previousStoryStatus: StoryProgressStatus? = null,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.StoryOpen,
        properties = buildMap {
            put("story_id", AnalyticsProperties.string(story.id))
            put("story_order", AnalyticsProperties.int(storyOrder))
            put("content_level", AnalyticsProperties.int(story.level))
            put("open_source", AnalyticsProperties.string(openSource))
            previousStoryStatus?.let {
                put("previous_story_status", AnalyticsProperties.string(it.analyticsValue))
            }
        },
    )

    fun paragraphAudioPlay(
        storyId: String,
        paragraphIndex: Int,
        audioSource: String = "tts",
        sentenceIndex: Int? = null,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.ParagraphAudioPlay,
        properties = buildMap {
            put("story_id", AnalyticsProperties.string(storyId))
            put("paragraph_index", AnalyticsProperties.int(paragraphIndex))
            put("audio_source", AnalyticsProperties.string(audioSource))
            sentenceIndex?.let { put("sentence_index", AnalyticsProperties.int(it)) }
        },
    )

    fun pinyinToggle(
        storyId: String,
        enabled: Boolean,
        surface: String,
        paragraphIndex: Int? = null,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.PinyinToggle,
        properties = buildMap {
            put("story_id", AnalyticsProperties.string(storyId))
            put("enabled", AnalyticsProperties.boolean(enabled))
            put("surface", AnalyticsProperties.string(surface))
            paragraphIndex?.let { put("paragraph_index", AnalyticsProperties.int(it)) }
        },
    )

    fun vocabOpen(
        story: Story,
        vocabIndex: Int,
        openSource: String,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.VocabOpen,
        properties = mapOf(
            "story_id" to AnalyticsProperties.string(story.id),
            "vocab_id" to AnalyticsProperties.string("${story.id}:${vocabIndex + 1}"),
            "open_source" to AnalyticsProperties.string(openSource),
            "content_level" to AnalyticsProperties.int(story.level),
        ),
    )

    fun wordBookOpen(
        entryPoint: String,
        learnedCount: Int,
        dueCount: Int,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.WordBookOpen,
        properties = mapOf(
            "entry_point" to AnalyticsProperties.string(entryPoint),
            "learned_count" to AnalyticsProperties.int(learnedCount),
            "due_count" to AnalyticsProperties.int(dueCount),
        ),
    )

    fun wordReviewAnswer(
        storyId: String,
        vocabId: String,
        rating: String,
        reviewIndex: Int,
        nextIntervalDays: Int,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.WordReviewAnswer,
        properties = mapOf(
            "story_id" to AnalyticsProperties.string(storyId),
            "vocab_id" to AnalyticsProperties.string(vocabId),
            "rating" to AnalyticsProperties.string(rating),
            "review_index" to AnalyticsProperties.int(reviewIndex),
            "next_interval_days" to AnalyticsProperties.int(nextIntervalDays),
        ),
    )

    fun wordReviewComplete(
        sessionSize: Int,
        reviewedCount: Int,
        knownCount: Int,
        needsPracticeCount: Int,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.WordReviewComplete,
        properties = mapOf(
            "session_size" to AnalyticsProperties.int(sessionSize),
            "reviewed_count" to AnalyticsProperties.int(reviewedCount),
            "known_count" to AnalyticsProperties.int(knownCount),
            "needs_practice_count" to AnalyticsProperties.int(needsPracticeCount),
        ),
    )

    fun quizStart(story: Story): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.QuizStart,
        properties = mapOf(
            "story_id" to AnalyticsProperties.string(story.id),
            "question_count" to AnalyticsProperties.int(story.questions.size),
            "attempt_number" to AnalyticsProperties.int(1),
        ),
    )

    fun quizComplete(
        story: Story,
        score: QuizScore,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.QuizComplete,
        properties = mapOf(
            "story_id" to AnalyticsProperties.string(story.id),
            "question_count" to AnalyticsProperties.int(score.totalQuestions),
            "correct_count" to AnalyticsProperties.int(score.correctCount),
            "attempt_number" to AnalyticsProperties.int(1),
        ),
    )

    fun aiExplainRequest(
        storyId: String,
        requestType: String,
        safetyOutcome: String,
        targetType: String,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.AiExplainRequest,
        properties = mapOf(
            "story_id" to AnalyticsProperties.string(storyId),
            "request_type" to AnalyticsProperties.string(requestType),
            "safety_outcome" to AnalyticsProperties.string(safetyOutcome),
            "target_type" to AnalyticsProperties.string(targetType),
        ),
    )

    fun storyComplete(
        story: Story,
        storyOrder: Int,
        quizCompleted: Boolean,
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.StoryComplete,
        properties = mapOf(
            "story_id" to AnalyticsProperties.string(story.id),
            "story_order" to AnalyticsProperties.int(storyOrder),
            "content_level" to AnalyticsProperties.int(story.level),
            "quiz_completed" to AnalyticsProperties.boolean(quizCompleted),
        ),
    )

    fun parentReportOpen(
        entryPoint: String,
        reportPeriod: String = "week",
    ): AnalyticsEventPayload = payload(
        eventName = AnalyticsEventName.ParentReportOpen,
        properties = mapOf(
            "entry_point" to AnalyticsProperties.string(entryPoint),
            "report_period" to AnalyticsProperties.string(reportPeriod),
        ),
    )

    private fun payload(
        eventName: AnalyticsEventName,
        properties: Map<String, JsonElement>,
    ): AnalyticsEventPayload = AnalyticsEventPayload(
        eventName = eventName,
        properties = properties,
    )

    private val StoryProgressStatus.analyticsValue: String
        get() = when (this) {
            StoryProgressStatus.NotStarted -> "not_started"
            StoryProgressStatus.InProgress -> "in_progress"
            StoryProgressStatus.Completed -> "completed"
        }
}
