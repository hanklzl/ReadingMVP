package com.littlemandarin.classics.shared.sfx

import com.littlemandarin.classics.shared.presentation.ReaderSettingsStore
import com.littlemandarin.classics.shared.presentation.StoredReaderSettingsService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SfxPresentationTest {
    @Test
    fun sfxSettingsDefaultToEnabledAndSanitizeVolume() {
        val defaults = SfxSettings()

        assertTrue(defaults.enabled)
        assertEquals(SfxSettings.DefaultVolume, defaults.volume, FloatTolerance)
        assertEquals(0f, SfxSettings(volume = -0.25f).sanitized().volume, FloatTolerance)
        assertEquals(1f, SfxSettings(volume = 1.25f).sanitized().volume, FloatTolerance)
        assertEquals(SfxSettings.DefaultVolume, SfxSettings(volume = Float.NaN).sanitized().volume, FloatTolerance)
    }

    @Test
    fun storedReaderSettingsServicePersistsSanitizedSfxSettings() = runTest {
        val store = FakeReaderSettingsStore()
        val service = StoredReaderSettingsService(
            store = store,
            defaultAiBackendBaseUrl = "mock",
        )

        service.setSfxEnabled(false)
        service.setSfxVolume(1.25f)

        val readSettings = service.read().sfxSettings
        assertFalse(readSettings.enabled)
        assertEquals(1f, readSettings.volume, FloatTolerance)

        val freshService = StoredReaderSettingsService(
            store = store,
            defaultAiBackendBaseUrl = "mock",
        )
        val persistedSettings = freshService.read().sfxSettings
        assertFalse(persistedSettings.enabled)
        assertEquals(1f, persistedSettings.volume, FloatTolerance)

        freshService.setSfxVolume(Float.NaN)
        assertEquals(SfxSettings.DefaultVolume, freshService.read().sfxSettings.volume, FloatTolerance)
    }

    @Test
    fun sfxReducerMapsQuizAnswerFeedbackAndSuppressesWhenDisabled() {
        val reducer = SfxEventReducer()
        val enabledSettings = SfxSettings(volume = 0.4f)
        val disabledSettings = SfxSettings(enabled = false)

        val correctCue = reducer.quizAnswerSubmitted(isCorrect = true, settings = enabledSettings)
        val wrongCue = reducer.quizAnswerSubmitted(isCorrect = false, settings = enabledSettings)

        assertNotNull(correctCue)
        assertEquals(SfxEvent.QuizCorrect, correctCue.event)
        assertEquals(0.4f, correctCue.volume, FloatTolerance)
        assertEquals(SfxEvent.QuizWrong, wrongCue?.event)
        assertNull(reducer.quizAnswerSubmitted(isCorrect = true, settings = disabledSettings))
        assertNull(reducer.quizAnswerSubmitted(isCorrect = false, settings = disabledSettings))
    }

    @Test
    fun sfxReducerPrefersSupportedStreakMilestonesOverGenericCompletion() {
        val reducer = SfxEventReducer()
        val settings = SfxSettings()

        val genericCompletion = reducer.storyCompleted(newMilestoneDays = null, settings = settings)
        val milestone3 = reducer.storyCompleted(newMilestoneDays = 3, settings = settings)
        val milestone7 = reducer.storyCompleted(newMilestoneDays = 7, settings = settings)
        val milestone14 = reducer.storyCompleted(newMilestoneDays = 14, settings = settings)
        val unsupportedMilestone = reducer.storyCompleted(newMilestoneDays = 5, settings = settings)

        assertEquals(SfxEvent.CompletionCelebration, genericCompletion?.event)
        assertEquals(SfxEvent.StreakMilestone, milestone3?.event)
        assertEquals(3, milestone3?.milestoneDays)
        assertEquals(SfxEvent.StreakMilestone, milestone7?.event)
        assertEquals(7, milestone7?.milestoneDays)
        assertEquals(SfxEvent.StreakMilestone, milestone14?.event)
        assertEquals(14, milestone14?.milestoneDays)
        assertEquals(SfxEvent.CompletionCelebration, unsupportedMilestone?.event)
        assertNull(reducer.storyCompleted(newMilestoneDays = 7, settings = settings.copy(enabled = false)))
    }
}

private class FakeReaderSettingsStore : ReaderSettingsStore {
    private var languageTag: String? = null
    private var showPinyinByDefault: Boolean? = null
    private var readingTextSizeValue: String? = null
    private var aiBackendBaseUrl: String? = null
    private var sfxEnabled: Boolean? = null
    private var sfxVolume: Float? = null
    private val readingPositions = mutableMapOf<String, Int>()

    override fun readLanguageTag(): String? = languageTag

    override fun writeLanguageTag(languageTag: String) {
        this.languageTag = languageTag
    }

    override fun readShowPinyinByDefault(defaultValue: Boolean): Boolean =
        showPinyinByDefault ?: defaultValue

    override fun writeShowPinyinByDefault(showPinyin: Boolean) {
        showPinyinByDefault = showPinyin
    }

    override fun readReadingTextSizeValue(): String? = readingTextSizeValue

    override fun writeReadingTextSizeValue(value: String) {
        readingTextSizeValue = value
    }

    override fun readAiBackendBaseUrl(defaultValue: String): String =
        aiBackendBaseUrl ?: defaultValue

    override fun writeAiBackendBaseUrl(baseUrl: String) {
        aiBackendBaseUrl = baseUrl
    }

    override fun readSfxEnabled(defaultValue: Boolean): Boolean =
        sfxEnabled ?: defaultValue

    override fun writeSfxEnabled(enabled: Boolean) {
        sfxEnabled = enabled
    }

    override fun readSfxVolume(defaultValue: Float): Float =
        sfxVolume ?: defaultValue

    override fun writeSfxVolume(volume: Float) {
        sfxVolume = volume
    }

    override fun readReadingParagraphIndex(storyId: String): Int =
        readingPositions[storyId] ?: -1

    override fun writeReadingParagraphIndex(storyId: String, paragraphIndex: Int) {
        readingPositions[storyId] = paragraphIndex
    }
}

private const val FloatTolerance: Float = 0.0001f
