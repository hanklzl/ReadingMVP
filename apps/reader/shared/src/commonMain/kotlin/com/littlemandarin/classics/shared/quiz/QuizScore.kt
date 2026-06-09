package com.littlemandarin.classics.shared.quiz

data class QuizScore(
    val correctCount: Int,
    val totalQuestions: Int,
    val scorePercent: Double,
    val results: List<QuestionResult>,
)

data class QuestionResult(
    val questionId: String,
    val selectedAnswer: String?,
    val correctAnswer: String,
    val isCorrect: Boolean,
)
