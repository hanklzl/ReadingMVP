package com.littlemandarin.classics.shared.presentation

import platform.Foundation.NSUserDefaults

actual fun createPlatformOnboardingService(): OnboardingService =
    StoredOnboardingService(IosOnboardingPreferencesStore())

actual fun createPlatformStreakService(): StreakService =
    StoredStreakService(IosStreakStateStore())

private class IosOnboardingPreferencesStore(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : OnboardingPreferencesStore {
    override fun readPreferences(): OnboardingPreferences =
        OnboardingPreferencesJsonCodec.decode(userDefaults.stringForKey(OnboardingKey))

    override fun writePreferences(preferences: OnboardingPreferences) {
        userDefaults.setObject(
            OnboardingPreferencesJsonCodec.encode(preferences),
            forKey = OnboardingKey,
        )
    }

    override fun clearPreferences() {
        userDefaults.removeObjectForKey(OnboardingKey)
    }

    private companion object {
        const val OnboardingKey: String = "lmc_onboarding_preferences"
    }
}

private class IosStreakStateStore(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : StreakStateStore {
    override fun readState(): StreakState =
        StreakStateJsonCodec.decode(userDefaults.stringForKey(StreakKey))

    override fun writeState(state: StreakState) {
        userDefaults.setObject(
            StreakStateJsonCodec.encode(state),
            forKey = StreakKey,
        )
    }

    override fun clearState() {
        userDefaults.removeObjectForKey(StreakKey)
    }

    private companion object {
        const val StreakKey: String = "lmc_streak_state"
    }
}
