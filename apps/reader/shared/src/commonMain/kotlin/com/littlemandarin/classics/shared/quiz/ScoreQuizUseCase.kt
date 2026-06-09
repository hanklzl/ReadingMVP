package com.littlemandarin.classics.shared.quiz

import com.littlemandarin.classics.shared.story.Story

class ScoreQuizUseCase {
    operator fun invoke(
        story: Story,
        answers: Map<String, String>,
    ): QuizScore {
        val results = story.questions.map { question ->
            val selectedAnswer = answers[question.id]
            QuestionResult(
                questionId = question.id,
                selectedAnswer = selectedAnswer,
                correctAnswer = question.answer,
                isCorrect = selectedAnswer == question.answer,
            )
        }
        val correctCount = results.count { it.isCorrect }
        val totalQuestions = results.size
        val scorePercent = if (totalQuestions == 0) {
            0.0
        } else {
            correctCount.toDouble() / totalQuestions.toDouble() * 100.0
        }

        return QuizScore(
            correctCount = correctCount,
            totalQuestions = totalQuestions,
            scorePercent = scorePercent,
            results = results,
        )
    }
}
