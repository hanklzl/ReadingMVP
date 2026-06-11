package com.littlemandarin.classics.shared.sfx

import kotlinx.serialization.Serializable

@Serializable
enum class SfxEvent(val semanticKey: String) {
    CompletionCelebration("story_complete_chime"),
    QuizCorrect("quiz_correct"),
    QuizWrong("quiz_try_again"),
    StreakMilestone("streak_milestone"),
}

@Serializable
data class SfxSettings(
    val enabled: Boolean = DefaultEnabled,
    val volume: Float = DefaultVolume,
) {
    fun sanitized(): SfxSettings {
        val sanitizedVolume = sanitizeSfxVolume(volume)
        return if (sanitizedVolume == volume) this else copy(volume = sanitizedVolume)
    }

    fun withEnabled(enabled: Boolean): SfxSettings =
        copy(enabled = enabled).sanitized()

    fun withVolume(volume: Float): SfxSettings =
        copy(volume = volume).sanitized()

    companion object {
        const val DefaultEnabled: Boolean = true
        const val DefaultVolume: Float = 0.5f
    }
}

@Serializable
data class SfxCue(
    val event: SfxEvent,
    val volume: Float,
    val milestoneDays: Int? = null,
) {
    val semanticKey: String
        get() = if (event == SfxEvent.StreakMilestone && milestoneDays != null) {
            "${event.semanticKey}_$milestoneDays"
        } else {
            event.semanticKey
        }
}

class SfxEventReducer {
    fun quizAnswerSubmitted(
        isCorrect: Boolean,
        settings: SfxSettings = SfxSettings(),
    ): SfxCue? =
        cue(
            event = if (isCorrect) SfxEvent.QuizCorrect else SfxEvent.QuizWrong,
            settings = settings,
        )

    fun storyCompleted(
        newMilestoneDays: Int?,
        settings: SfxSettings = SfxSettings(),
    ): SfxCue? {
        val sanitizedSettings = settings.sanitized()
        if (!sanitizedSettings.enabled) return null

        return if (newMilestoneDays != null && newMilestoneDays in SupportedStreakMilestones) {
            SfxCue(
                event = SfxEvent.StreakMilestone,
                volume = sanitizedSettings.volume,
                milestoneDays = newMilestoneDays,
            )
        } else {
            SfxCue(
                event = SfxEvent.CompletionCelebration,
                volume = sanitizedSettings.volume,
            )
        }
    }

    private fun cue(
        event: SfxEvent,
        settings: SfxSettings,
    ): SfxCue? {
        val sanitizedSettings = settings.sanitized()
        if (!sanitizedSettings.enabled) return null
        return SfxCue(event = event, volume = sanitizedSettings.volume)
    }

    private companion object {
        val SupportedStreakMilestones: Set<Int> = setOf(3, 7, 14)
    }
}

interface SfxPlayer {
    suspend fun play(cue: SfxCue)
}

internal fun sanitizeSfxVolume(volume: Float): Float =
    when {
        volume.isNaN() -> SfxSettings.DefaultVolume
        volume < 0f -> 0f
        volume > 1f -> 1f
        else -> volume
    }
