package com.littlemandarin.classics.shared.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class ChildAgeBand(val recommendedLevel: Int) {
    Age5To8(1),
    Age5To6(1),
    Age7To8(2),
}

object OnboardingDefaults {
    const val DailyGoalStories: Int = 1
    val DefaultChildAgeBand: ChildAgeBand = ChildAgeBand.Age5To8

    fun skippedPreferences(language: ReaderLanguage = ReaderLanguage.English): OnboardingPreferences =
        OnboardingPreferences(
            completed = true,
            skipped = true,
            childAgeBand = DefaultChildAgeBand,
            language = language,
            dailyGoalStories = DailyGoalStories,
        )
}

@Serializable
data class OnboardingPreferences(
    val completed: Boolean = false,
    val skipped: Boolean = false,
    val childAgeBand: ChildAgeBand? = null,
    val language: ReaderLanguage = ReaderLanguage.English,
    val dailyGoalStories: Int = 1,
    // Reading-level preference from the optional placement check (1..3). NOT child PII.
    // When present it takes precedence over the age-band default for recommendations.
    val assessedReadingLevel: Int? = null,
) {
    val recommendedLevel: Int
        get() = assessedReadingLevel?.takeIf { it in 1..3 }
            ?: childAgeBand?.recommendedLevel
            ?: 1
}

interface OnboardingService {
    val preferences: Flow<OnboardingPreferences>

    suspend fun read(): OnboardingPreferences

    suspend fun complete(preferences: OnboardingPreferences)

    suspend fun skip(preferences: OnboardingPreferences = OnboardingDefaults.skippedPreferences())

    suspend fun clear()
}

class InMemoryOnboardingService(
    initialPreferences: OnboardingPreferences = OnboardingPreferences(),
) : OnboardingService {
    private val state = MutableStateFlow(initialPreferences.normalized())

    override val preferences: Flow<OnboardingPreferences> = state

    override suspend fun read(): OnboardingPreferences = state.value

    override suspend fun complete(preferences: OnboardingPreferences) {
        state.value = preferences.copy(completed = true, skipped = false).normalized()
    }

    override suspend fun skip(preferences: OnboardingPreferences) {
        state.value = preferences.toSkippedDefaults().normalized()
    }

    override suspend fun clear() {
        state.value = OnboardingPreferences()
    }
}

expect fun createPlatformOnboardingService(): OnboardingService

internal interface OnboardingPreferencesStore {
    fun readPreferences(): OnboardingPreferences

    fun writePreferences(preferences: OnboardingPreferences)

    fun clearPreferences()
}

internal class StoredOnboardingService(
    private val store: OnboardingPreferencesStore,
) : OnboardingService {
    private val state = MutableStateFlow(store.readPreferences().normalized())

    override val preferences: Flow<OnboardingPreferences> = state

    override suspend fun read(): OnboardingPreferences {
        val latest = store.readPreferences().normalized()
        state.value = latest
        return latest
    }

    override suspend fun complete(preferences: OnboardingPreferences) {
        val updated = preferences.copy(completed = true, skipped = false).normalized()
        store.writePreferences(updated)
        state.value = updated
    }

    override suspend fun skip(preferences: OnboardingPreferences) {
        val updated = preferences.toSkippedDefaults().normalized()
        store.writePreferences(updated)
        state.value = updated
    }

    override suspend fun clear() {
        store.clearPreferences()
        state.value = OnboardingPreferences()
    }
}

@Serializable
data class DailyCompletionCount(
    val epochDay: Int,
    val completedStories: Int,
)

@Serializable
data class StreakState(
    val dailyGoalStories: Int = 1,
    val completions: List<DailyCompletionCount> = emptyList(),
    val awardedMilestones: Set<Int> = emptySet(),
)

data class StreakSummary(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val todayCompletedStories: Int,
    val dailyGoalStories: Int,
    val todayProgressFraction: Double,
    val todayGoalMet: Boolean,
    val newMilestoneDays: Int? = null,
)

interface StreakService {
    val state: Flow<StreakState>

    suspend fun read(): StreakState

    suspend fun write(state: StreakState)

    suspend fun clear()
}

class InMemoryStreakService(
    initialState: StreakState = StreakState(),
) : StreakService {
    private val stateFlow = MutableStateFlow(initialState.normalized())

    override val state: Flow<StreakState> = stateFlow

    override suspend fun read(): StreakState = stateFlow.value

    override suspend fun write(state: StreakState) {
        stateFlow.value = state.normalized()
    }

    override suspend fun clear() {
        stateFlow.value = StreakState()
    }
}

expect fun createPlatformStreakService(): StreakService

internal interface StreakStateStore {
    fun readState(): StreakState

    fun writeState(state: StreakState)

    fun clearState()
}

internal class StoredStreakService(
    private val store: StreakStateStore,
) : StreakService {
    private val stateFlow = MutableStateFlow(store.readState().normalized())

    override val state: Flow<StreakState> = stateFlow

    override suspend fun read(): StreakState {
        val latest = store.readState().normalized()
        stateFlow.value = latest
        return latest
    }

    override suspend fun write(state: StreakState) {
        val normalized = state.normalized()
        store.writeState(normalized)
        stateFlow.value = normalized
    }

    override suspend fun clear() {
        store.clearState()
        stateFlow.value = StreakState()
    }
}

class StreakUseCase(
    private val streakService: StreakService,
) {
    suspend fun setDailyGoal(dailyGoalStories: Int) {
        val current = streakService.read()
        streakService.write(
            current.copy(dailyGoalStories = dailyGoalStories.coerceAtLeast(1)),
        )
    }

    suspend fun recordStoryCompleted(nowEpochMillis: Long): StreakSummary {
        val today = epochDayFromMillis(nowEpochMillis)
        val current = streakService.read().normalized()
        val updatedCompletions = current.completions
            .associateBy { it.epochDay }
            .toMutableMap()
        val existing = updatedCompletions[today]
        updatedCompletions[today] = DailyCompletionCount(
            epochDay = today,
            completedStories = ((existing?.completedStories ?: 0) + 1).coerceAtLeast(0),
        )

        val updated = current.copy(
            completions = updatedCompletions.values.toList(),
        ).normalized()
        val milestone = firstNewMilestone(updated, today)
        val stateToStore = if (milestone == null) {
            updated
        } else {
            updated.copy(awardedMilestones = updated.awardedMilestones + milestone)
        }.normalized()

        streakService.write(stateToStore)
        return buildSummary(
            state = stateToStore,
            today = today,
            newMilestoneDays = milestone,
        )
    }

    suspend fun summary(todayEpochMillis: Long): StreakSummary =
        buildSummary(
            state = streakService.read().normalized(),
            today = epochDayFromMillis(todayEpochMillis),
            newMilestoneDays = null,
        )

    private fun firstNewMilestone(
        state: StreakState,
        today: Int,
    ): Int? {
        val current = buildSummary(state, today, null).currentStreakDays
        return Milestones.firstOrNull { milestone ->
            current >= milestone && milestone !in state.awardedMilestones
        }
    }

    private fun buildSummary(
        state: StreakState,
        today: Int,
        newMilestoneDays: Int?,
    ): StreakSummary {
        val dailyGoal = state.dailyGoalStories.coerceAtLeast(1)
        val completionsByDay = state.completions.associateBy { it.epochDay }
        val completedGoalDays = completionsByDay
            .filterValues { it.completedStories >= dailyGoal }
            .keys
        val todayCompleted = completionsByDay[today]?.completedStories ?: 0
        val currentStartDay = if (today in completedGoalDays) today else today - 1
        val currentStreak = countBackwardsStreak(completedGoalDays, currentStartDay)

        return StreakSummary(
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak(completedGoalDays),
            todayCompletedStories = todayCompleted,
            dailyGoalStories = dailyGoal,
            todayProgressFraction = (todayCompleted.toDouble() / dailyGoal.toDouble()).coerceIn(0.0, 1.0),
            todayGoalMet = todayCompleted >= dailyGoal,
            newMilestoneDays = newMilestoneDays,
        )
    }

    private fun countBackwardsStreak(
        completedGoalDays: Set<Int>,
        startDay: Int,
    ): Int {
        var count = 0
        var day = startDay
        while (day in completedGoalDays) {
            count += 1
            day -= 1
        }
        return count
    }

    private fun longestStreak(completedGoalDays: Set<Int>): Int {
        var longest = 0
        var current = 0
        var previous: Int? = null
        for (day in completedGoalDays.sorted()) {
            current = if (previous != null && day == previous + 1) current + 1 else 1
            if (current > longest) longest = current
            previous = day
        }
        return longest
    }

    private companion object {
        val Milestones = listOf(3, 7, 14)
    }
}

internal object OnboardingPreferencesJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(preferences: OnboardingPreferences): String =
        json.encodeToString(preferences.normalized())

    fun decode(encodedPreferences: String?): OnboardingPreferences {
        if (encodedPreferences.isNullOrBlank()) return OnboardingPreferences()

        return runCatching {
            json.decodeFromString<OnboardingPreferences>(encodedPreferences)
                .normalized()
        }.getOrElse {
            OnboardingPreferences()
        }
    }
}

internal object StreakStateJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: StreakState): String =
        json.encodeToString(state.normalized())

    fun decode(encodedState: String?): StreakState {
        if (encodedState.isNullOrBlank()) return StreakState()

        return runCatching {
            json.decodeFromString<StreakState>(encodedState)
                .normalized()
        }.getOrElse {
            StreakState()
        }
    }
}

private fun OnboardingPreferences.normalized(): OnboardingPreferences {
    val normalizedGoal = dailyGoalStories.coerceAtLeast(OnboardingDefaults.DailyGoalStories)
    val normalizedLevel = assessedReadingLevel?.takeIf { it in 1..3 }
    return if (completed && skipped) {
        copy(
            childAgeBand = childAgeBand ?: OnboardingDefaults.DefaultChildAgeBand,
            dailyGoalStories = normalizedGoal,
            assessedReadingLevel = normalizedLevel,
        )
    } else {
        copy(
            dailyGoalStories = normalizedGoal,
            assessedReadingLevel = normalizedLevel,
        )
    }
}

private fun OnboardingPreferences.toSkippedDefaults(): OnboardingPreferences =
    OnboardingDefaults.skippedPreferences(language)

private fun StreakState.normalized(): StreakState =
    copy(
        dailyGoalStories = dailyGoalStories.coerceAtLeast(1),
        completions = completions
            .filter { it.epochDay >= 0 && it.completedStories > 0 }
            .groupBy { it.epochDay }
            .map { (epochDay, dayCompletions) ->
                DailyCompletionCount(
                    epochDay = epochDay,
                    completedStories = dayCompletions.sumOf { it.completedStories }.coerceAtLeast(0),
                )
            }
            .sortedBy { it.epochDay },
        awardedMilestones = awardedMilestones.filter { it > 0 }.toSet(),
    )
