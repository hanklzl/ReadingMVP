package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.service.AiExplanationRequest
import com.littlemandarin.classics.shared.service.AiExplanationResponse
import com.littlemandarin.classics.shared.service.AiQuestionTypes
import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Story

const val AiAnswerMaxChars: Int = 100
const val AiSelectedTextMaxChars: Int = 120

class BuildAiExplanationRequestUseCase {
    fun forParagraph(
        story: Story,
        paragraphIndex: Int,
        childAge: Int = 6,
    ): AiExplanationRequest? {
        val paragraph = story.paragraphs.getOrNull(
            paragraphIndex.coerceIn(0, (story.paragraphs.size - 1).coerceAtLeast(0)),
        ) ?: return null

        return forParagraph(
            storyId = story.id,
            paragraph = paragraph,
            childAge = childAge,
        )
    }

    fun forParagraph(
        storyId: String,
        paragraph: Paragraph,
        childAge: Int = 6,
    ): AiExplanationRequest? {
        return forSelectedText(
            storyId = storyId,
            selectedText = paragraph.text,
            questionType = AiQuestionTypes.ExplainSentence,
            childAge = childAge,
        )
    }

    fun forSelectedText(
        storyId: String,
        selectedText: String,
        questionType: String = AiQuestionTypes.ExplainSentence,
        childAge: Int = 6,
    ): AiExplanationRequest? {
        val normalizedText = selectedText.trim().take(AiSelectedTextMaxChars)
        if (storyId.isBlank() || normalizedText.isBlank()) return null

        return AiExplanationRequest(
            storyId = storyId,
            selectedText = normalizedText,
            questionType = questionType,
            childAge = childAge,
        )
    }
}

fun AiExplanationResponse.toLimitedDisplayText(
    stubText: String,
    outOfScopeText: String,
): String = toDisplayText(
    stubText = stubText,
    outOfScopeText = outOfScopeText,
).trim().take(AiAnswerMaxChars)
