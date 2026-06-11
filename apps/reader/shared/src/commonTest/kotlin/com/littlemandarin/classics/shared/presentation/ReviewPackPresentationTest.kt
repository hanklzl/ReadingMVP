package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.quiz.MissedQuestion
import com.littlemandarin.classics.shared.quiz.ReviewPack
import com.littlemandarin.classics.shared.quiz.ReviewParentTip
import com.littlemandarin.classics.shared.quiz.ReviewWord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private const val DayMillis: Long = 24L * 60L * 60L * 1_000L

class ReviewPackPresentationTest {
    private fun samplePack(): ReviewPack = ReviewPack(
        storyId = "story-1",
        titleZh = "桃园结义",
        titleEn = "Oath in the Garden",
        missedQuestions = listOf(
            MissedQuestion("q1", "谁先提议结义？", "刘备", "刘备最先提出大家一起做兄弟。"),
        ),
        reviewWords = listOf(ReviewWord("结义", "jié yì", "to become sworn brothers")),
        rereadSentence = "三人在桃园结为兄弟。",
        parentTip = ReviewParentTip.ReadTogether,
    )

    @Test
    fun savedPackRoundTripsThroughStoredForm() = runTest {
        val service = InMemoryReviewPackService()
        val useCase = ReviewPackUseCase(service)

        useCase.savePack(samplePack(), todayEpochMillis = 40L * DayMillis)

        val pending = useCase.pendingReview(todayEpochMillis = 40L * DayMillis)
        assertEquals(samplePack(), pending?.pack)
        assertFalse(pending!!.isFromPreviousDay)
    }

    @Test
    fun pendingReviewIsFlaggedFromPreviousDay() = runTest {
        val service = InMemoryReviewPackService()
        val useCase = ReviewPackUseCase(service)
        useCase.savePack(samplePack(), todayEpochMillis = 40L * DayMillis)

        val pending = useCase.pendingReview(todayEpochMillis = 41L * DayMillis)
        assertTrue(pending!!.isFromPreviousDay)
    }

    @Test
    fun completedPackIsNoLongerPending() = runTest {
        val service = InMemoryReviewPackService()
        val useCase = ReviewPackUseCase(service)
        useCase.savePack(samplePack(), todayEpochMillis = 40L * DayMillis)

        useCase.markCompleted(todayEpochMillis = 41L * DayMillis)

        assertNull(useCase.pendingReview(todayEpochMillis = 41L * DayMillis))
        assertEquals(41, service.read()?.completedEpochDay)
    }

    @Test
    fun jsonCodecRoundTrip() {
        val stored = samplePack().toStored(savedEpochDay = 40)
        val encoded = ReviewPackJsonCodec.encode(stored)
        assertEquals(stored, ReviewPackJsonCodec.decode(encoded))
        assertNull(ReviewPackJsonCodec.decode(null))
        assertNull(ReviewPackJsonCodec.decode("not-json"))
    }
}
