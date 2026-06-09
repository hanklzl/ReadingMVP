package com.littlemandarin.classics.shared.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object AiFallbackAnswers {
    const val OutOfScope: String = "这个问题和今天的故事关系不大，我们先回到故事里吧。"
}

object AiMessageKeys {
    const val ExplanationStub: String = "ai_explanation_stub"
    const val OutOfScope: String = "ai_out_of_scope"
}

object AiQuestionTypes {
    const val ExplainWord: String = "explain_word"
    const val ExplainSentence: String = "explain_sentence"
    const val AnswerQuestion: String = "answer_question"
}

@Serializable
data class AiExplanationRequest(
    @SerialName("story_id")
    val storyId: String,
    @SerialName("selected_text")
    val selectedText: String,
    @SerialName("question_type")
    val questionType: String,
    @SerialName("child_age")
    val childAge: Int = 6,
)

@Serializable
data class AiExplanationResponse(
    val answer: String,
    @SerialName("message_key")
    val messageKey: String? = null,
) {
    fun toDisplayText(
        stubText: String,
        outOfScopeText: String,
    ): String = when (messageKey) {
        AiMessageKeys.ExplanationStub -> stubText
        AiMessageKeys.OutOfScope -> outOfScopeText
        else -> answer
    }

    fun safetyOutcome(
        displayedAnswer: String,
        outOfScopeText: String,
    ): String = if (
        messageKey == AiMessageKeys.OutOfScope ||
        displayedAnswer == outOfScopeText ||
        answer == AiFallbackAnswers.OutOfScope
    ) {
        "out_of_scope"
    } else {
        "allowed"
    }
}

@Serializable
data class AiServiceConfig(
    @SerialName("base_url")
    val baseUrl: String? = null,
    @SerialName("api_key")
    val apiKey: String? = null,
    @SerialName("max_selected_text_length")
    val maxSelectedTextLength: Int = 120,
    @SerialName("max_answer_length")
    val maxAnswerLength: Int = 100,
)

interface AiService {
    suspend fun explain(request: AiExplanationRequest): AiExplanationResponse
}

interface AiExplainBackendClient {
    suspend fun postExplain(
        baseUrl: String,
        apiKey: String?,
        request: AiExplanationRequest,
    ): AiExplanationResponse
}

fun createAiService(
    config: AiServiceConfig = AiServiceConfig(),
    backendClient: AiExplainBackendClient? = null,
): AiService {
    val baseUrl = config.normalizedBaseUrl()
    return if (baseUrl != null && backendClient != null) {
        BackendAiService(
            config = config,
            baseUrl = baseUrl,
            backendClient = backendClient,
        )
    } else {
        MockAiService(config)
    }
}

expect fun createPlatformAiService(
    config: AiServiceConfig = AiServiceConfig(),
): AiService

class MockAiService(
    private val config: AiServiceConfig = AiServiceConfig(),
) : AiService {
    override suspend fun explain(request: AiExplanationRequest): AiExplanationResponse {
        val normalizedRequest = request.normalizedOrNull(config)
            ?: return AiExplanationResponse(
                answer = AiFallbackAnswers.OutOfScope.limitedAnswer(config),
                messageKey = AiMessageKeys.OutOfScope,
            )

        val answer = when (normalizedRequest.questionType) {
            AiQuestionTypes.ExplainWord ->
                "这个词可以放回故事里理解。先看它旁边的人和事，再想它帮助我们明白什么。"
            AiQuestionTypes.ExplainSentence ->
                "这句话是在说故事里的想法或行动。先找谁在做什么，再想他为什么这样做。"
            AiQuestionTypes.AnswerQuestion ->
                "可以从故事中找线索回答。先回到这段文字，再用自己的话说出原因。"
            else -> AiFallbackAnswers.OutOfScope
        }

        return AiExplanationResponse(
            answer = answer.limitedAnswer(config),
            messageKey = AiMessageKeys.ExplanationStub,
        )
    }
}

class BackendAiService internal constructor(
    private val config: AiServiceConfig,
    private val baseUrl: String,
    private val backendClient: AiExplainBackendClient,
) : AiService {
    override suspend fun explain(request: AiExplanationRequest): AiExplanationResponse {
        val normalizedRequest = request.normalizedOrNull(config)
            ?: return AiExplanationResponse(
                answer = AiFallbackAnswers.OutOfScope.limitedAnswer(config),
                messageKey = AiMessageKeys.OutOfScope,
            )

        val response = backendClient.postExplain(
            baseUrl = baseUrl,
            apiKey = config.apiKey?.trim()?.takeIf { it.isNotBlank() },
            request = normalizedRequest,
        )

        return AiExplanationResponse(response.answer.limitedAnswer(config))
    }
}

@Deprecated(
    message = "Use MockAiService or createAiService().",
    replaceWith = ReplaceWith("MockAiService()"),
)
class StubAiService : AiService {
    private val delegate = MockAiService()

    override suspend fun explain(request: AiExplanationRequest): AiExplanationResponse =
        delegate.explain(request)
}

private fun AiExplanationRequest.normalizedOrNull(config: AiServiceConfig): AiExplanationRequest? {
    val normalizedStoryId = storyId.trim()
    val normalizedSelectedText = selectedText.trim()
    val normalizedQuestionType = questionType.trim()
    val maxSelectedTextLength = config.maxSelectedTextLength.coerceIn(1, 240)

    if (normalizedStoryId.isBlank()) return null
    if (normalizedSelectedText.isBlank()) return null
    if (normalizedSelectedText.length > maxSelectedTextLength) return null
    if (normalizedQuestionType !in SupportedQuestionTypes) return null
    if (childAge !in 5..8) return null

    return copy(
        storyId = normalizedStoryId,
        selectedText = normalizedSelectedText,
        questionType = normalizedQuestionType,
    )
}

private fun AiServiceConfig.normalizedBaseUrl(): String? =
    baseUrl
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf { it.isNotBlank() }

private fun String.limitedAnswer(config: AiServiceConfig): String {
    val maxLength = config.maxAnswerLength.coerceIn(1, 100)
    val normalized = trim().ifBlank { AiFallbackAnswers.OutOfScope }
    return normalized.take(maxLength)
}

private val SupportedQuestionTypes = setOf(
    AiQuestionTypes.ExplainWord,
    AiQuestionTypes.ExplainSentence,
    AiQuestionTypes.AnswerQuestion,
)
