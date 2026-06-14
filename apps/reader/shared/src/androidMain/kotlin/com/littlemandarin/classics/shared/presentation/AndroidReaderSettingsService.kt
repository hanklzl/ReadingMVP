package com.littlemandarin.classics.shared.presentation

import android.content.Context
import android.content.SharedPreferences

object AndroidReaderSettingsServiceProvider {
    private const val PreferencesName: String = "little_mandarin_reader_settings"

    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun sharedPreferences(): SharedPreferences {
        val context = applicationContext
            ?: error(
                "AndroidReaderSettingsServiceProvider.initialize(context) must be called before " +
                    "createPlatformReaderSettingsService().",
            )

        return context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    }
}

actual fun createPlatformReaderSettingsService(): ReaderSettingsService =
    StoredReaderSettingsService(
        store = AndroidReaderSettingsStore(AndroidReaderSettingsServiceProvider.sharedPreferences()),
        defaultAiBackendBaseUrl = androidDefaultAiBackendBaseUrl(),
        defaultLanguage = androidSystemDefaultLanguage(),
    )

// Follow the device language by default when the user has not chosen one yet.
// Read the true SYSTEM locale (immune to the app's own Locale.setDefault() when it
// forces a UI locale), so re-reads stay correct.
private fun androidSystemDefaultLanguage(): ReaderLanguage {
    val systemLanguage = android.content.res.Resources.getSystem()
        .configuration.locales[0]?.language
        ?: java.util.Locale.getDefault().language
    return if (systemLanguage.equals("zh", ignoreCase = true)) {
        ReaderLanguage.ChineseSimplified
    } else {
        ReaderLanguage.English
    }
}

private class AndroidReaderSettingsStore(
    private val sharedPreferences: SharedPreferences,
) : ReaderSettingsStore {
    override fun readLanguageTag(): String? =
        sharedPreferences.getString(KeyLanguage, null)

    override fun writeLanguageTag(languageTag: String) {
        sharedPreferences.edit()
            .putString(KeyLanguage, languageTag)
            .apply()
    }

    override fun readShowPinyinByDefault(defaultValue: Boolean): Boolean =
        sharedPreferences.getBoolean(KeyShowPinyinDefault, defaultValue)

    override fun writeShowPinyinByDefault(showPinyin: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KeyShowPinyinDefault, showPinyin)
            .apply()
    }

    override fun readReadingTextSizeValue(): String? =
        sharedPreferences.getString(KeyReadingTextSize, null)

    override fun writeReadingTextSizeValue(value: String) {
        sharedPreferences.edit()
            .putString(KeyReadingTextSize, value)
            .apply()
    }

    override fun readAiBackendBaseUrl(defaultValue: String): String =
        sharedPreferences.getString(KeyAiBackendBaseUrl, defaultValue) ?: defaultValue

    override fun writeAiBackendBaseUrl(baseUrl: String) {
        sharedPreferences.edit()
            .putString(KeyAiBackendBaseUrl, baseUrl)
            .apply()
    }

    override fun readSfxEnabled(defaultValue: Boolean): Boolean =
        sharedPreferences.getBoolean(KeySfxEnabled, defaultValue)

    override fun writeSfxEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KeySfxEnabled, enabled)
            .apply()
    }

    override fun readSfxVolume(defaultValue: Float): Float =
        sharedPreferences.getFloat(KeySfxVolume, defaultValue)

    override fun writeSfxVolume(volume: Float) {
        sharedPreferences.edit()
            .putFloat(KeySfxVolume, volume)
            .apply()
    }

    override fun readReadingParagraphIndex(storyId: String): Int =
        sharedPreferences.getInt(readingProgressKey(storyId), -1)

    override fun writeReadingParagraphIndex(storyId: String, paragraphIndex: Int) {
        sharedPreferences.edit()
            .putInt(readingProgressKey(storyId), paragraphIndex)
            .apply()
    }

    private fun readingProgressKey(storyId: String): String = "$KeyReadingProgressPrefix$storyId"

    private companion object {
        const val KeyLanguage = "language"
        const val KeyShowPinyinDefault = "show_pinyin_default"
        const val KeyReadingTextSize = "reading_text_size"
        const val KeyAiBackendBaseUrl = "ai_backend_base_url"
        const val KeySfxEnabled = "sfx_enabled"
        const val KeySfxVolume = "sfx_volume"
        const val KeyReadingProgressPrefix = "reading_progress_"
    }
}
