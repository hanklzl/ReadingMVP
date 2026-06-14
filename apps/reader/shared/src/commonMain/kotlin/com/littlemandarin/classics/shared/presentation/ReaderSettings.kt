package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.sfx.SfxSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

private const val DefaultShowPinyinByDefault: Boolean = true
private const val AndroidDefaultAiBackendBaseUrl: String = "mock"
private const val IosDefaultAiBackendBaseUrl: String = "local/mock"

@Serializable
data class ReaderSettings(
    val language: ReaderLanguage = ReaderLanguage.English,
    val showPinyinByDefault: Boolean = DefaultShowPinyinByDefault,
    val readingTextSize: ReadingTextSize = ReadingTextSize.Medium,
    val aiBackendBaseUrl: String = AndroidDefaultAiBackendBaseUrl,
    val sfxSettings: SfxSettings = SfxSettings(),
) {
    val isMockAiBackend: Boolean
        get() = aiBackendBaseUrl.isMockAiBackend()
}

@Serializable
enum class ReaderLanguage(val tag: String) {
    English("en"),
    ChineseSimplified("zh-Hans"),
    ;

    companion object {
        fun fromTag(tag: String?): ReaderLanguage =
            entries.firstOrNull { it.tag == tag } ?: English
    }
}

@Serializable
enum class ReadingTextSize(val prefValue: String) {
    Small("small"),
    Medium("medium"),
    Large("large"),
    ;

    companion object {
        fun fromPrefValue(value: String?): ReadingTextSize =
            entries.firstOrNull { it.prefValue == value } ?: Medium
    }
}

class ReaderSettingsReducer(
    private val defaultAiBackendBaseUrl: String = AndroidDefaultAiBackendBaseUrl,
) {
    fun defaultState(): ReaderSettings = ReaderSettings(
        aiBackendBaseUrl = defaultAiBackendBaseUrl,
    )

    fun languageChanged(
        state: ReaderSettings,
        language: ReaderLanguage,
    ): ReaderSettings = state.copy(language = language)

    fun showPinyinByDefaultChanged(
        state: ReaderSettings,
        showPinyinByDefault: Boolean,
    ): ReaderSettings = state.copy(showPinyinByDefault = showPinyinByDefault)

    fun readingTextSizeChanged(
        state: ReaderSettings,
        readingTextSize: ReadingTextSize,
    ): ReaderSettings = state.copy(readingTextSize = readingTextSize)

    fun aiBackendBaseUrlChanged(
        state: ReaderSettings,
        aiBackendBaseUrl: String,
    ): ReaderSettings = state.copy(
        aiBackendBaseUrl = normalizeAiBackendBaseUrl(aiBackendBaseUrl, defaultAiBackendBaseUrl),
    )
}

interface ReaderSettingsService {
    val settings: Flow<ReaderSettings>

    suspend fun read(): ReaderSettings

    suspend fun setLanguage(language: ReaderLanguage)

    suspend fun setShowPinyinByDefault(showPinyin: Boolean)

    suspend fun setReadingTextSize(textSize: ReadingTextSize)

    suspend fun setAiBackendBaseUrl(baseUrl: String)

    suspend fun setSfxEnabled(enabled: Boolean)

    suspend fun setSfxVolume(volume: Float)

    suspend fun readReadingParagraphIndex(storyId: String): Int

    suspend fun setReadingParagraphIndex(storyId: String, paragraphIndex: Int)
}

class InMemoryReaderSettingsService(
    initialSettings: ReaderSettings = ReaderSettings(),
    initialReadingPositions: Map<String, Int> = emptyMap(),
    private val defaultAiBackendBaseUrl: String = initialSettings.aiBackendBaseUrl,
) : ReaderSettingsService {
    private val state = MutableStateFlow(initialSettings)
    private val readingPositions = initialReadingPositions.toMutableMap()

    override val settings: Flow<ReaderSettings> = state

    override suspend fun read(): ReaderSettings = state.value

    override suspend fun setLanguage(language: ReaderLanguage) {
        state.value = state.value.copy(language = language)
    }

    override suspend fun setShowPinyinByDefault(showPinyin: Boolean) {
        state.value = state.value.copy(showPinyinByDefault = showPinyin)
    }

    override suspend fun setReadingTextSize(textSize: ReadingTextSize) {
        state.value = state.value.copy(readingTextSize = textSize)
    }

    override suspend fun setAiBackendBaseUrl(baseUrl: String) {
        state.value = state.value.copy(
            aiBackendBaseUrl = normalizeAiBackendBaseUrl(
                baseUrl = baseUrl,
                defaultBaseUrl = defaultAiBackendBaseUrl,
            ),
        )
    }

    override suspend fun setSfxEnabled(enabled: Boolean) {
        state.value = state.value.copy(
            sfxSettings = state.value.sfxSettings.withEnabled(enabled),
        )
    }

    override suspend fun setSfxVolume(volume: Float) {
        state.value = state.value.copy(
            sfxSettings = state.value.sfxSettings.withVolume(volume),
        )
    }

    override suspend fun readReadingParagraphIndex(storyId: String): Int =
        readingPositions[storyId].orMissingReadingPosition()

    override suspend fun setReadingParagraphIndex(storyId: String, paragraphIndex: Int) {
        if (storyId.isBlank()) return
        readingPositions[storyId] = paragraphIndex
    }
}

expect fun createPlatformReaderSettingsService(): ReaderSettingsService

internal interface ReaderSettingsStore {
    fun readLanguageTag(): String?

    fun writeLanguageTag(languageTag: String)

    fun readShowPinyinByDefault(defaultValue: Boolean): Boolean

    fun writeShowPinyinByDefault(showPinyin: Boolean)

    fun readReadingTextSizeValue(): String?

    fun writeReadingTextSizeValue(value: String)

    fun readAiBackendBaseUrl(defaultValue: String): String

    fun writeAiBackendBaseUrl(baseUrl: String)

    fun readSfxEnabled(defaultValue: Boolean): Boolean

    fun writeSfxEnabled(enabled: Boolean)

    fun readSfxVolume(defaultValue: Float): Float

    fun writeSfxVolume(volume: Float)

    fun readReadingParagraphIndex(storyId: String): Int

    fun writeReadingParagraphIndex(storyId: String, paragraphIndex: Int)
}

internal class StoredReaderSettingsService(
    private val store: ReaderSettingsStore,
    private val defaultAiBackendBaseUrl: String,
    // Language to use when the user has NOT explicitly chosen one yet (no stored tag).
    // Platforms pass the current system locale so the app follows the device language
    // by default; an explicit choice (onboarding / Settings) persists and wins.
    private val defaultLanguage: ReaderLanguage = ReaderLanguage.English,
) : ReaderSettingsService {
    private val state = MutableStateFlow(readFromStore())

    override val settings: Flow<ReaderSettings> = state

    override suspend fun read(): ReaderSettings {
        val latest = readFromStore()
        state.value = latest
        return latest
    }

    override suspend fun setLanguage(language: ReaderLanguage) {
        store.writeLanguageTag(language.tag)
        state.value = state.value.copy(language = language)
    }

    override suspend fun setShowPinyinByDefault(showPinyin: Boolean) {
        store.writeShowPinyinByDefault(showPinyin)
        state.value = state.value.copy(showPinyinByDefault = showPinyin)
    }

    override suspend fun setReadingTextSize(textSize: ReadingTextSize) {
        store.writeReadingTextSizeValue(textSize.prefValue)
        state.value = state.value.copy(readingTextSize = textSize)
    }

    override suspend fun setAiBackendBaseUrl(baseUrl: String) {
        val normalized = normalizeAiBackendBaseUrl(baseUrl, defaultAiBackendBaseUrl)
        store.writeAiBackendBaseUrl(normalized)
        state.value = state.value.copy(aiBackendBaseUrl = normalized)
    }

    override suspend fun setSfxEnabled(enabled: Boolean) {
        val sfxSettings = state.value.sfxSettings.withEnabled(enabled)
        store.writeSfxEnabled(sfxSettings.enabled)
        state.value = state.value.copy(sfxSettings = sfxSettings)
    }

    override suspend fun setSfxVolume(volume: Float) {
        val sfxSettings = state.value.sfxSettings.withVolume(volume)
        store.writeSfxVolume(sfxSettings.volume)
        state.value = state.value.copy(sfxSettings = sfxSettings)
    }

    override suspend fun readReadingParagraphIndex(storyId: String): Int =
        store.readReadingParagraphIndex(storyId).orMissingReadingPosition()

    override suspend fun setReadingParagraphIndex(storyId: String, paragraphIndex: Int) {
        if (storyId.isBlank()) return
        store.writeReadingParagraphIndex(storyId, paragraphIndex)
    }

    private fun readFromStore(): ReaderSettings = ReaderSettings(
        language = store.readLanguageTag()?.let { ReaderLanguage.fromTag(it) } ?: defaultLanguage,
        showPinyinByDefault = store.readShowPinyinByDefault(DefaultShowPinyinByDefault),
        readingTextSize = ReadingTextSize.fromPrefValue(store.readReadingTextSizeValue()),
        aiBackendBaseUrl = normalizeAiBackendBaseUrl(
            baseUrl = store.readAiBackendBaseUrl(defaultAiBackendBaseUrl),
            defaultBaseUrl = defaultAiBackendBaseUrl,
        ),
        sfxSettings = SfxSettings(
            enabled = store.readSfxEnabled(SfxSettings.DefaultEnabled),
            volume = store.readSfxVolume(SfxSettings.DefaultVolume),
        ).sanitized(),
    )
}

internal fun androidDefaultAiBackendBaseUrl(): String = AndroidDefaultAiBackendBaseUrl

internal fun iosDefaultAiBackendBaseUrl(): String = IosDefaultAiBackendBaseUrl

fun String.isMockAiBackend(): Boolean =
    trim().let { value ->
        value.isBlank() ||
            value.equals(AndroidDefaultAiBackendBaseUrl, ignoreCase = true) ||
            value.equals(IosDefaultAiBackendBaseUrl, ignoreCase = true)
    }

private fun normalizeAiBackendBaseUrl(
    baseUrl: String,
    defaultBaseUrl: String,
): String {
    val trimmed = baseUrl.trim()
    return if (trimmed.isBlank()) defaultBaseUrl else trimmed
}

private fun Int?.orMissingReadingPosition(): Int =
    this?.takeIf { it >= 0 } ?: -1
