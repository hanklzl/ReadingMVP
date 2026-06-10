package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.quiz.QuestionResult
import com.littlemandarin.classics.shared.quiz.QuizScore
import com.littlemandarin.classics.shared.quiz.ScoreQuizUseCase
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story

data class QuizSessionState(
    val questionIndex: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val submittedQuestionIds: Set<String> = emptySet(),
    val isComplete: Boolean = false,
)

data class QuizQuestionState(
    val question: Question?,
    val questionIndex: Int,
    val questionCount: Int,
    val selectedAnswer: String?,
    val submitted: Boolean,
    val progressFraction: Double,
    val canSubmitOrAdvance: Boolean,
    val isLastQuestion: Boolean,
    val result: QuestionResult?,
)

class QuizSessionReducer(
    private val scoreQuizUseCase: ScoreQuizUseCase = ScoreQuizUseCase(),
) {
    fun initialState(story: Story): QuizSessionState =
        QuizSessionState(
            questionIndex = 0,
            isComplete = story.questions.isEmpty(),
        )

    fun questionState(
        story: Story,
        state: QuizSessionState,
    ): QuizQuestionState {
        val questionCount = story.questions.size.coerceAtLeast(1)
        val questionIndex = state.questionIndex.coerceIn(0, questionCount - 1)
        val question = story.questions.getOrNull(questionIndex)
        val selectedAnswer = question?.let { state.answers[it.id] }
        val submitted = question?.let { state.submittedQuestionIds.contains(it.id) } ?: false
        val result = question?.let {
            QuestionResult(
                questionId = it.id,
                selectedAnswer = selectedAnswer,
                correctAnswer = it.answer,
                isCorrect = selectedAnswer == it.answer,
            )
        }

        return QuizQuestionState(
            question = question,
            questionIndex = questionIndex,
            questionCount = questionCount,
            selectedAnswer = selectedAnswer,
            submitted = submitted,
            progressFraction = if (story.questions.isEmpty()) {
                0.0
            } else {
                (questionIndex + 1).toDouble() / questionCount.toDouble()
            },
            canSubmitOrAdvance = submitted || selectedAnswer != null,
            isLastQuestion = questionIndex >= questionCount - 1,
            result = result,
        )
    }

    fun selectAnswer(
        story: Story,
        state: QuizSessionState,
        answer: String,
    ): QuizSessionState {
        val question = story.questions.getOrNull(state.questionIndex) ?: return state
        if (state.submittedQuestionIds.contains(question.id)) return state

        return state.copy(
            answers = state.answers + (question.id to answer),
        )
    }

    fun submitOrAdvance(
        story: Story,
        state: QuizSessionState,
    ): QuizSessionState {
        val question = story.questions.getOrNull(state.questionIndex)
            ?: return state.copy(isComplete = true)
        val submitted = state.submittedQuestionIds.contains(question.id)

        return when {
            !submitted && state.answers[question.id] != null -> state.copy(
                submittedQuestionIds = state.submittedQuestionIds + question.id,
            )
            submitted && state.questionIndex < story.questions.lastIndex -> state.copy(
                questionIndex = state.questionIndex + 1,
            )
            submitted -> state.copy(isComplete = true)
            else -> state
        }
    }

    fun currentSelectedAnswer(
        story: Story,
        state: QuizSessionState,
    ): String? = story.questions.getOrNull(state.questionIndex)
        ?.let { state.answers[it.id] }

    fun isCurrentQuestionSubmitted(
        story: Story,
        state: QuizSessionState,
    ): Boolean = story.questions.getOrNull(state.questionIndex)
        ?.let { state.submittedQuestionIds.contains(it.id) }
        ?: false

    fun score(
        story: Story,
        state: QuizSessionState,
    ): QuizScore = scoreQuizUseCase(story, state.answers)

    fun completionRecord(
        story: Story,
        state: QuizSessionState,
        nowEpochMillis: Long,
    ): CompletionRecord {
        val score = score(story, state)
        return CompletionRecord(
            storyId = story.id,
            completedAtEpochMillis = nowEpochMillis,
            vocabCount = story.vocab.size,
            correctCount = score.correctCount,
            questionCount = score.totalQuestions,
        )
    }
}
