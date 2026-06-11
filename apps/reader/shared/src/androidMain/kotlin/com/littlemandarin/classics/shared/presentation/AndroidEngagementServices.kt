package com.littlemandarin.classics.shared.presentation

import android.content.Context
import android.content.SharedPreferences

object AndroidEngagementServiceProvider {
    private const val PreferencesName: String = "little_mandarin_engagement"

    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun sharedPreferences(): SharedPreferences {
        val context = applicationContext
            ?: error(
                "AndroidEngagementServiceProvider.initialize(context) must be called before " +
                    "createPlatformOnboardingService() or createPlatformStreakService().",
            )

        return context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    }
}

actual fun createPlatformOnboardingService(): OnboardingService =
    StoredOnboardingService(AndroidOnboardingPreferencesStore(AndroidEngagementServiceProvider.sharedPreferences()))

actual fun createPlatformStreakService(): StreakService =
    StoredStreakService(AndroidStreakStateStore(AndroidEngagementServiceProvider.sharedPreferences()))

actual fun createPlatformReviewPackService(): ReviewPackService =
    StoredReviewPackService(AndroidReviewPackStore(AndroidEngagementServiceProvider.sharedPreferences()))

private class AndroidReviewPackStore(
    private val sharedPreferences: SharedPreferences,
) : ReviewPackStore {
    override fun readPack(): StoredReviewPack? =
        ReviewPackJsonCodec.decode(sharedPreferences.getString(ReviewPackKey, null))

    override fun writePack(pack: StoredReviewPack) {
        sharedPreferences.edit()
            .putString(ReviewPackKey, ReviewPackJsonCodec.encode(pack))
            .apply()
    }

    override fun clearPack() {
        sharedPreferences.edit()
            .remove(ReviewPackKey)
            .apply()
    }

    private companion object {
        const val ReviewPackKey: String = "review_pack"
    }
}

private class AndroidOnboardingPreferencesStore(
    private val sharedPreferences: SharedPreferences,
) : OnboardingPreferencesStore {
    override fun readPreferences(): OnboardingPreferences =
        OnboardingPreferencesJsonCodec.decode(sharedPreferences.getString(OnboardingKey, null))

    override fun writePreferences(preferences: OnboardingPreferences) {
        sharedPreferences.edit()
            .putString(OnboardingKey, OnboardingPreferencesJsonCodec.encode(preferences))
            .apply()
    }

    override fun clearPreferences() {
        sharedPreferences.edit()
            .remove(OnboardingKey)
            .apply()
    }

    private companion object {
        const val OnboardingKey: String = "onboarding_preferences"
    }
}

private class AndroidStreakStateStore(
    private val sharedPreferences: SharedPreferences,
) : StreakStateStore {
    override fun readState(): StreakState =
        StreakStateJsonCodec.decode(sharedPreferences.getString(StreakKey, null))

    override fun writeState(state: StreakState) {
        sharedPreferences.edit()
            .putString(StreakKey, StreakStateJsonCodec.encode(state))
            .apply()
    }

    override fun clearState() {
        sharedPreferences.edit()
            .remove(StreakKey)
            .apply()
    }

    private companion object {
        const val StreakKey: String = "streak_state"
    }
}
