package com.littlemandarin.classics.shared.service

import kotlinx.serialization.Serializable

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
    val storyId: String,
    val selectedText: String,
    val questionType: String,
)

@Serializable
data class AiExplanationResponse(
    val messageKey: String,
)

interface AiService {
    suspend fun explain(request: AiExplanationRequest): AiExplanationResponse
}

class StubAiService : AiService {
    override suspend fun explain(request: AiExplanationRequest): AiExplanationResponse {
        val messageKey = if (request.isInControlledStoryScope()) {
            AiMessageKeys.ExplanationStub
        } else {
            AiMessageKeys.OutOfScope
        }

        return AiExplanationResponse(messageKey = messageKey)
    }

    private fun AiExplanationRequest.isInControlledStoryScope(): Boolean =
        storyId.isNotBlank() &&
            selectedText.isNotBlank() &&
            questionType in SupportedQuestionTypes

    private companion object {
        val SupportedQuestionTypes = setOf(
            AiQuestionTypes.ExplainWord,
            AiQuestionTypes.ExplainSentence,
            AiQuestionTypes.AnswerQuestion,
        )
    }
}
