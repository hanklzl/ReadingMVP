package com.littlemandarin.classics.shared.quiz

import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MistakeRemediationTest {
    private val useCases = MistakeRemediationUseCases()
    private val scoreQuiz = ScoreQuizUseCase()

    private fun story(
        questions: List<Question>,
        vocab: List<Vocab>,
        paragraphs: List<Paragraph> = listOf(Paragraph("大家一起想办法。后来他们成功了。", "")),
    ): Story = Story(
        id = "s1",
        titleZh = "故事一",
        titleEn = "Story One",
        level = 1,
        ageRange = "5-8",
        sourceNote = "public domain",
        paragraphs = paragraphs,
        vocab = vocab,
        questions = questions,
        retellPrompt = "复述",
    )

    private val questions = listOf(
        Question("q1", "single_choice", "谁想办法？", listOf("大家", "没人"), "大家", "因为大家一起。"),
        Question("q2", "single_choice", "结果如何？", listOf("成功", "失败"), "成功", "正文说成功了。"),
        Question("q3", "single_choice", "心情怎样？", listOf("开心", "难过"), "开心", "他们很开心。"),
    )

    private val vocab = listOf(
        Vocab("办法", "bàn fǎ", "method"),
        Vocab("成功", "chéng gōng", "success"),
        Vocab("一起", "yì qǐ", "together"),
        Vocab("开心", "kāi xīn", "happy"),
    )

    @Test
    fun missedQuestionsCarryCorrectAnswerAndExplanation() {
        val s = story(questions, vocab)
        val score = scoreQuiz(s, mapOf("q1" to "大家", "q2" to "失败", "q3" to "开心"))

        val pack = useCases.buildReviewPack(s, score)

        assertEquals(1, pack.missedQuestions.size)
        val missed = pack.missedQuestions.first()
        assertEquals("q2", missed.questionId)
        assertEquals("成功", missed.correctAnswer)
        assertEquals("正文说成功了。", missed.explanation)
    }

    @Test
    fun reviewWordsPrioritiseDueThenCapAtThree() {
        val s = story(questions, vocab)
        val score = scoreQuiz(s, mapOf("q1" to "大家", "q2" to "成功", "q3" to "开心"))

        val pack = useCases.buildReviewPack(s, score, dueWords = setOf("开心"))

        assertEquals(3, pack.reviewWords.size)
        assertEquals("开心", pack.reviewWords.first().word) // due word first
        assertTrue(pack.reviewWords.all { it.word in setOf("开心", "办法", "成功", "一起") })
    }

    @Test
    fun parentTipReadTogetherWhenStruggled() {
        val s = story(questions, vocab)
        val score = scoreQuiz(s, mapOf("q1" to "没人", "q2" to "失败", "q3" to "难过")) // 0/3

        val pack = useCases.buildReviewPack(s, score)

        assertEquals(ReviewParentTip.ReadTogether, pack.parentTip)
    }

    @Test
    fun parentTipReviewDueWordsWhenDidWellButDuePending() {
        val s = story(questions, vocab)
        val score = scoreQuiz(s, mapOf("q1" to "大家", "q2" to "成功", "q3" to "开心")) // 3/3

        val pack = useCases.buildReviewPack(s, score, dueWords = setOf("成功"))

        assertEquals(ReviewParentTip.ReviewDueWords, pack.parentTip)
    }

    @Test
    fun parentTipPraiseWhenDidWellAndNothingDue() {
        val s = story(questions, vocab)
        val score = scoreQuiz(s, mapOf("q1" to "大家", "q2" to "成功", "q3" to "开心")) // 3/3

        val pack = useCases.buildReviewPack(s, score, dueWords = emptySet())

        assertEquals(ReviewParentTip.PraiseAndOneWord, pack.parentTip)
    }

    @Test
    fun rereadSentencePrefersMissedKeywordThenFallsBackToFirst() {
        val s = story(questions, vocab)
        // q2 (成功) wrong → sentence containing 成功 should be chosen.
        val struggled = scoreQuiz(s, mapOf("q1" to "大家", "q2" to "失败", "q3" to "开心"))
        val keywordPack = useCases.buildReviewPack(s, struggled)
        assertTrue(keywordPack.rereadSentence!!.contains("成功"))

        // All correct → no missed keyword → first sentence fallback.
        val perfect = scoreQuiz(s, mapOf("q1" to "大家", "q2" to "成功", "q3" to "开心"))
        val firstPack = useCases.buildReviewPack(s, perfect)
        assertEquals("大家一起想办法。", firstPack.rereadSentence)
    }

    @Test
    fun rereadSentenceNullWhenNoParagraphs() {
        val s = story(questions, vocab, paragraphs = emptyList())
        val score = scoreQuiz(s, mapOf("q1" to "大家", "q2" to "成功", "q3" to "开心"))

        val pack = useCases.buildReviewPack(s, score)

        assertNull(pack.rereadSentence)
    }
}
