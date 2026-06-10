package com.littlemandarin.classics.shared.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class StreakPresentationTest {
    @Test
    fun summaryCountsCurrentAndLongestStreaksFromCompletedGoalDays() = runTest {
        val today = 40L * StreakDayMillis
        val service = InMemoryStreakService(
            initialState = StreakState(
                dailyGoalStories = 1,
                completions = listOf(
                    DailyCompletionCount(epochDay = 38, completedStories = 1),
                    DailyCompletionCount(epochDay = 39, completedStories = 1),
                    DailyCompletionCount(epochDay = 40, completedStories = 1),
                ),
            ),
        )
        val useCase = StreakUseCase(service)

        val summary = useCase.summary(todayEpochMillis = today)

        assertEquals(3, summary.currentStreakDays)
        assertEquals(3, summary.longestStreakDays)
        assertEquals(1, summary.todayCompletedStories)
        assertEquals(1, summary.dailyGoalStories)
        assertTrue(summary.todayGoalMet)
    }

    @Test
    fun summaryResetsCurrentStreakAfterMissingDayButKeepsLongest() = runTest {
        val today = 40L * StreakDayMillis
        val service = InMemoryStreakService(
            initialState = StreakState(
                dailyGoalStories = 1,
                completions = listOf(
                    DailyCompletionCount(epochDay = 36, completedStories = 1),
                    DailyCompletionCount(epochDay = 37, completedStories = 1),
                    DailyCompletionCount(epochDay = 40, completedStories = 1),
                ),
            ),
        )
        val useCase = StreakUseCase(service)

        val summary = useCase.summary(todayEpochMillis = today)

        assertEquals(1, summary.currentStreakDays)
        assertEquals(2, summary.longestStreakDays)
        assertTrue(summary.todayGoalMet)
    }

    @Test
    fun recordCompletionTracksDailyGoalProgressAndFirstMilestoneOnce() = runTest {
        val today = 40L * StreakDayMillis
        val service = InMemoryStreakService(
            initialState = StreakState(
                dailyGoalStories = 2,
                completions = listOf(
                    DailyCompletionCount(epochDay = 38, completedStories = 2),
                    DailyCompletionCount(epochDay = 39, completedStories = 2),
                ),
            ),
        )
        val useCase = StreakUseCase(service)

        val first = useCase.recordStoryCompleted(nowEpochMillis = today)
        val second = useCase.recordStoryCompleted(nowEpochMillis = today)
        val repeated = useCase.summary(todayEpochMillis = today)

        assertEquals(1, first.todayCompletedStories)
        assertFalse(first.todayGoalMet)
        assertEquals(2, second.todayCompletedStories)
        assertTrue(second.todayGoalMet)
        assertEquals(3, second.currentStreakDays)
        assertEquals(3, second.newMilestoneDays)
        assertEquals(null, repeated.newMilestoneDays)
    }

    @Test
    fun onboardingPreferencesPersistAgeLanguageAndGoalWithoutPersonalInformation() = runTest {
        val service = InMemoryOnboardingService()

        service.complete(
            preferences = OnboardingPreferences(
                completed = true,
                skipped = false,
                childAgeBand = ChildAgeBand.Age7To8,
                language = ReaderLanguage.ChineseSimplified,
                dailyGoalStories = 2,
            ),
        )

        val saved = service.read()
        assertTrue(saved.completed)
        assertFalse(saved.skipped)
        assertEquals(ChildAgeBand.Age7To8, saved.childAgeBand)
        assertEquals(ReaderLanguage.ChineseSimplified, saved.language)
        assertEquals(2, saved.dailyGoalStories)
        assertEquals(2, saved.recommendedLevel)
    }

    @Test
    fun onboardingSkipPersistsSafeDefaultsWithoutPersonalInformation() = runTest {
        val service = InMemoryOnboardingService()

        service.skip()

        val saved = service.read()
        assertTrue(saved.completed)
        assertTrue(saved.skipped)
        assertEquals(ChildAgeBand.Age5To8, saved.childAgeBand)
        assertEquals(ReaderLanguage.English, saved.language)
        assertEquals(1, saved.dailyGoalStories)
        assertEquals(1, saved.recommendedLevel)
    }

    @Test
    fun onboardingSkipKeepsSystemLanguageButForcesSafeDefaults() = runTest {
        val service = InMemoryOnboardingService()

        service.skip(
            OnboardingPreferences(
                childAgeBand = ChildAgeBand.Age7To8,
                language = ReaderLanguage.ChineseSimplified,
                dailyGoalStories = 3,
            ),
        )

        val saved = service.read()
        assertTrue(saved.completed)
        assertTrue(saved.skipped)
        assertEquals(ChildAgeBand.Age5To8, saved.childAgeBand)
        assertEquals(ReaderLanguage.ChineseSimplified, saved.language)
        assertEquals(1, saved.dailyGoalStories)
    }
}

private const val StreakDayMillis: Long = 24L * 60L * 60L * 1_000L
