package com.littlemandarin.classics.shared.presentation

import platform.Foundation.NSUserDefaults

actual fun createPlatformReaderSettingsService(): ReaderSettingsService =
    StoredReaderSettingsService(
        store = IosReaderSettingsStore(),
        defaultAiBackendBaseUrl = iosDefaultAiBackendBaseUrl(),
    )

private class IosReaderSettingsStore(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : ReaderSettingsStore {
    override fun readLanguageTag(): String? =
        userDefaults.stringForKey(KeyLanguage)

    override fun writeLanguageTag(languageTag: String) {
        userDefaults.setObject(languageTag, forKey = KeyLanguage)
    }

    override fun readShowPinyinByDefault(defaultValue: Boolean): Boolean =
        if (userDefaults.objectForKey(KeyShowPinyinDefault) == null) {
            defaultValue
        } else {
            userDefaults.boolForKey(KeyShowPinyinDefault)
        }

    override fun writeShowPinyinByDefault(showPinyin: Boolean) {
        userDefaults.setBool(showPinyin, forKey = KeyShowPinyinDefault)
    }

    override fun readReadingTextSizeValue(): String? =
        userDefaults.stringForKey(KeyReadingTextSize)

    override fun writeReadingTextSizeValue(value: String) {
        userDefaults.setObject(value, forKey = KeyReadingTextSize)
    }

    override fun readAiBackendBaseUrl(defaultValue: String): String =
        userDefaults.stringForKey(KeyAiBackendBaseUrl) ?: defaultValue

    override fun writeAiBackendBaseUrl(baseUrl: String) {
        userDefaults.setObject(baseUrl, forKey = KeyAiBackendBaseUrl)
    }

    override fun readReadingParagraphIndex(storyId: String): Int =
        userDefaults.integerForKey(readingProgressKey(storyId)).toInt().let { stored ->
            if (userDefaults.objectForKey(readingProgressKey(storyId)) == null) -1 else stored
        }

    override fun writeReadingParagraphIndex(storyId: String, paragraphIndex: Int) {
        userDefaults.setInteger(paragraphIndex.toLong(), forKey = readingProgressKey(storyId))
    }

    private fun readingProgressKey(storyId: String): String = "$KeyReadingProgressPrefix$storyId"

    private companion object {
        const val KeyLanguage = "lmc_locale_identifier"
        const val KeyShowPinyinDefault = "lmc_show_pinyin"
        const val KeyReadingTextSize = "lmc_reading_size"
        const val KeyAiBackendBaseUrl = "lmc_ai_backend_base_url"
        const val KeyReadingProgressPrefix = "lmc_reading_progress_"
    }
}
