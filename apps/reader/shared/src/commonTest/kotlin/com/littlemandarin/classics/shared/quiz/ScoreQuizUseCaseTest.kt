package com.littlemandarin.classics.shared.quiz

import com.littlemandarin.classics.shared.story.DefaultStoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class ScoreQuizUseCaseTest {
    @Test
    fun scoresTwoCorrectAnswersOutOfThreeQuestions() = runTest {
        val story = assertNotNull(DefaultStoryRepository().getStory("peach-garden-oath"))
        val answers = story.questions.associate { question ->
            question.id to question.answer
        }.toMutableMap()
        val missedQuestion = story.questions.first()
        answers[missedQuestion.id] = missedQuestion.options.first { option ->
            option != missedQuestion.answer
        }

        val result = ScoreQuizUseCase().invoke(story, answers)

        assertEquals(2, result.correctCount)
        assertEquals(3, result.totalQuestions)
        assertEquals(66.6666666667, result.scorePercent, 0.0001)
    }
}
