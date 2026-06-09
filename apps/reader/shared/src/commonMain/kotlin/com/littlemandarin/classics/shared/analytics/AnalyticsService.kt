package com.littlemandarin.classics.shared.analytics

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val DefaultAnalyticsSchemaVersion: Int = 1

@Serializable
enum class AnalyticsEventName(val wireName: String) {
    @SerialName("app_open")
    AppOpen("app_open"),

    @SerialName("story_open")
    StoryOpen("story_open"),

    @SerialName("paragraph_audio_play")
    ParagraphAudioPlay("paragraph_audio_play"),

    @SerialName("pinyin_toggle")
    PinyinToggle("pinyin_toggle"),

    @SerialName("vocab_open")
    VocabOpen("vocab_open"),

    @SerialName("quiz_start")
    QuizStart("quiz_start"),

    @SerialName("quiz_complete")
    QuizComplete("quiz_complete"),

    @SerialName("ai_explain_request")
    AiExplainRequest("ai_explain_request"),

    @SerialName("story_complete")
    StoryComplete("story_complete"),

    @SerialName("parent_report_open")
    ParentReportOpen("parent_report_open"),
}

@Serializable
enum class AnalyticsPlatform {
    @SerialName("android")
    Android,

    @SerialName("ios")
    Ios,
}

@Serializable
data class AnalyticsContext(
    @SerialName("anonymous_install_id")
    val anonymousInstallId: String,
    @SerialName("session_id")
    val sessionId: String,
    val platform: AnalyticsPlatform,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("schema_version")
    val schemaVersion: Int = DefaultAnalyticsSchemaVersion,
    @SerialName("ui_locale")
    val uiLocale: String,
)

@Serializable
data class AnalyticsEvent(
    @SerialName("event_id")
    val eventId: String,
    @SerialName("event_name")
    val eventName: AnalyticsEventName,
    @SerialName("event_timestamp_utc")
    val eventTimestampUtc: String,
    @SerialName("anonymous_install_id")
    val anonymousInstallId: String,
    @SerialName("session_id")
    val sessionId: String,
    val platform: AnalyticsPlatform,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("schema_version")
    val schemaVersion: Int,
    @SerialName("ui_locale")
    val uiLocale: String,
    val properties: Map<String, JsonElement> = emptyMap(),
)

interface Analytics {
    val events: Flow<List<AnalyticsEvent>>

    suspend fun track(
        eventName: AnalyticsEventName,
        properties: Map<String, JsonElement> = emptyMap(),
    ): AnalyticsEvent

    suspend fun getEvents(): List<AnalyticsEvent>

    suspend fun clear()
}

interface AnalyticsRuntime {
    fun newUuid(): String

    fun nowUtcIsoString(): String

    fun platform(): AnalyticsPlatform
}

object AnalyticsProperties {
    fun string(value: String): JsonElement = JsonPrimitive(value)

    fun int(value: Int): JsonElement = JsonPrimitive(value)

    fun long(value: Long): JsonElement = JsonPrimitive(value)

    fun double(value: Double): JsonElement = JsonPrimitive(value)

    fun boolean(value: Boolean): JsonElement = JsonPrimitive(value)
}

class InMemoryAnalyticsService(
    private val context: AnalyticsContext,
    private val runtime: AnalyticsRuntime = PlatformAnalyticsRuntime,
    initialEvents: List<AnalyticsEvent> = emptyList(),
) : Analytics {
    private val state = MutableStateFlow(initialEvents)

    override val events: Flow<List<AnalyticsEvent>> = state

    init {
        AnalyticsEventValidator.validateContext(context)
    }

    override suspend fun track(
        eventName: AnalyticsEventName,
        properties: Map<String, JsonElement>,
    ): AnalyticsEvent {
        AnalyticsEventValidator.validateProperties(eventName, properties)

        val event = buildAnalyticsEvent(
            eventName = eventName,
            properties = properties,
            context = context,
            runtime = runtime,
        )
        state.value = state.value + event
        return event
    }

    override suspend fun getEvents(): List<AnalyticsEvent> = state.value

    override suspend fun clear() {
        state.value = emptyList()
    }
}

expect object PlatformAnalyticsRuntime : AnalyticsRuntime

expect fun createPlatformAnalytics(
    appVersion: String = "unknown",
    uiLocale: String = "en",
): Analytics

internal interface AnalyticsStore {
    fun readEvents(): List<AnalyticsEvent>

    fun writeEvents(events: List<AnalyticsEvent>)

    fun clearEvents()

    fun readAnonymousInstallId(): String?

    fun writeAnonymousInstallId(anonymousInstallId: String)
}

internal class StoredAnalyticsService(
    private val store: AnalyticsStore,
    private val runtime: AnalyticsRuntime = PlatformAnalyticsRuntime,
    appVersion: String,
    uiLocale: String,
    schemaVersion: Int = DefaultAnalyticsSchemaVersion,
) : Analytics {
    private val context = AnalyticsContext(
        anonymousInstallId = readOrCreateAnonymousInstallId(),
        sessionId = runtime.newUuid(),
        platform = runtime.platform(),
        appVersion = appVersion,
        schemaVersion = schemaVersion,
        uiLocale = uiLocale,
    )
    private val state = MutableStateFlow(store.readEvents())

    override val events: Flow<List<AnalyticsEvent>> = state

    init {
        AnalyticsEventValidator.validateContext(context)
    }

    override suspend fun track(
        eventName: AnalyticsEventName,
        properties: Map<String, JsonElement>,
    ): AnalyticsEvent {
        AnalyticsEventValidator.validateProperties(eventName, properties)

        val event = buildAnalyticsEvent(
            eventName = eventName,
            properties = properties,
            context = context,
            runtime = runtime,
        )
        val updatedEvents = store.readEvents() + event
        store.writeEvents(updatedEvents)
        state.value = updatedEvents
        return event
    }

    override suspend fun getEvents(): List<AnalyticsEvent> {
        val latestEvents = store.readEvents()
        state.value = latestEvents
        return latestEvents
    }

    override suspend fun clear() {
        store.clearEvents()
        state.value = emptyList()
    }

    private fun readOrCreateAnonymousInstallId(): String {
        val existing = store.readAnonymousInstallId()
        if (!existing.isNullOrBlank()) return existing

        val created = runtime.newUuid()
        store.writeAnonymousInstallId(created)
        return created
    }
}

private fun buildAnalyticsEvent(
    eventName: AnalyticsEventName,
    properties: Map<String, JsonElement>,
    context: AnalyticsContext,
    runtime: AnalyticsRuntime,
): AnalyticsEvent = AnalyticsEvent(
    eventId = runtime.newUuid(),
    eventName = eventName,
    eventTimestampUtc = runtime.nowUtcIsoString(),
    anonymousInstallId = context.anonymousInstallId,
    sessionId = context.sessionId,
    platform = context.platform,
    appVersion = context.appVersion,
    schemaVersion = context.schemaVersion,
    uiLocale = context.uiLocale,
    properties = properties,
)

internal object AnalyticsEventJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(events: List<AnalyticsEvent>): String = json.encodeToString(events)

    fun decode(encodedEvents: String?): List<AnalyticsEvent> {
        if (encodedEvents.isNullOrBlank()) return emptyList()

        return runCatching {
            json.decodeFromString<List<AnalyticsEvent>>(encodedEvents)
        }.getOrElse {
            emptyList()
        }
    }
}

internal object AnalyticsEventValidator {
    fun validateContext(context: AnalyticsContext) {
        require(context.anonymousInstallId.isNotBlank()) {
            "anonymous_install_id is required"
        }
        require(context.sessionId.isNotBlank()) {
            "session_id is required"
        }
        require(context.appVersion.isNotBlank()) {
            "app_version is required"
        }
        require(context.schemaVersion > 0) {
            "schema_version must be positive"
        }
        require(context.uiLocale.isNotBlank()) {
            "ui_locale is required"
        }
    }

    fun validateProperties(
        eventName: AnalyticsEventName,
        properties: Map<String, JsonElement>,
    ) {
        val unsafeKey = properties.keys.firstOrNull(::isUnsafePropertyKey)
        require(unsafeKey == null) {
            "Analytics property '$unsafeKey' may contain raw prompt, free text, or PII"
        }

        val schema = EventSchemas.getValue(eventName)
        val allowedKeys = schema.required.keys + schema.optional.keys
        val unsupportedKeys = properties.keys - allowedKeys
        require(unsupportedKeys.isEmpty()) {
            "Unsupported analytics properties for ${eventName.wireName}: ${unsupportedKeys.sorted()}"
        }

        val missingKeys = schema.required.keys - properties.keys
        require(missingKeys.isEmpty()) {
            "Missing analytics properties for ${eventName.wireName}: ${missingKeys.sorted()}"
        }

        for ((key, value) in properties) {
            val expectedType = schema.required[key] ?: schema.optional.getValue(key)
            require(value.matches(expectedType)) {
                "Analytics property '$key' must be ${expectedType.description}"
            }
        }
    }

    private fun isUnsafePropertyKey(key: String): Boolean {
        val normalized = key.lowercase()
        return normalized in UnsafePropertyKeys ||
            UnsafePropertyFragments.any { fragment -> normalized.contains(fragment) }
    }

    private val UnsafePropertyKeys = setOf(
        "prompt",
        "raw_prompt",
        "raw_text",
        "free_text",
        "selected_text",
        "answer_text",
        "suggestion",
        "parent_contact",
        "contact",
        "email",
        "phone",
        "child_name",
        "name",
        "birthday",
        "school",
        "grade",
        "photo",
        "exact_location",
        "advertising_id",
    )

    private val UnsafePropertyFragments = setOf(
        "raw_prompt",
        "raw_text",
        "free_text",
        "selected_text",
    )
}

private enum class AnalyticsPropertyType(val description: String) {
    StringValue("a string"),
    IntValue("an integer"),
    LongValue("a long"),
    DoubleValue("a number"),
    BooleanValue("a boolean"),
}

private data class AnalyticsEventSchema(
    val required: Map<String, AnalyticsPropertyType>,
    val optional: Map<String, AnalyticsPropertyType> = emptyMap(),
)

private val EventSchemas: Map<AnalyticsEventName, AnalyticsEventSchema> = mapOf(
    AnalyticsEventName.AppOpen to AnalyticsEventSchema(
        required = mapOf(
            "open_type" to AnalyticsPropertyType.StringValue,
        ),
        optional = mapOf(
            "is_first_open" to AnalyticsPropertyType.BooleanValue,
            "days_since_first_open" to AnalyticsPropertyType.IntValue,
        ),
    ),
    AnalyticsEventName.StoryOpen to AnalyticsEventSchema(
        required = mapOf(
            "story_id" to AnalyticsPropertyType.StringValue,
            "story_order" to AnalyticsPropertyType.IntValue,
            "content_level" to AnalyticsPropertyType.IntValue,
            "open_source" to AnalyticsPropertyType.StringValue,
        ),
        optional = mapOf(
            "previous_story_status" to AnalyticsPropertyType.StringValue,
        ),
    ),
    AnalyticsEventName.ParagraphAudioPlay to AnalyticsEventSchema(
        required = mapOf(
            "story_id" to AnalyticsPropertyType.StringValue,
            "paragraph_index" to AnalyticsPropertyType.IntValue,
            "audio_source" to AnalyticsPropertyType.StringValue,
        ),
        optional = mapOf(
            "playback_speed_bucket" to AnalyticsPropertyType.StringValue,
        ),
    ),
    AnalyticsEventName.PinyinToggle to AnalyticsEventSchema(
        required = mapOf(
            "story_id" to AnalyticsPropertyType.StringValue,
            "enabled" to AnalyticsPropertyType.BooleanValue,
            "surface" to AnalyticsPropertyType.StringValue,
        ),
        optional = mapOf(
            "paragraph_index" to AnalyticsPropertyType.IntValue,
        ),
    ),
    AnalyticsEventName.VocabOpen to AnalyticsEventSchema(
        required = mapOf(
            "story_id" to AnalyticsPropertyType.StringValue,
            "vocab_id" to AnalyticsPropertyType.StringValue,
            "open_source" to AnalyticsPropertyType.StringValue,
        ),
        optional = mapOf(
            "content_level" to AnalyticsPropertyType.IntValue,
        ),
    ),
    AnalyticsEventName.QuizStart to AnalyticsEventSchema(
        required = mapOf(
            "story_id" to AnalyticsPropertyType.StringValue,
            "question_count" to AnalyticsPropertyType.IntValue,
        ),
        optional = mapOf(
            "attempt_number" to AnalyticsPropertyType.IntValue,
        ),
    ),
    AnalyticsEventName.QuizComplete to AnalyticsEventSchema(
        required = mapOf(
            "story_id" to AnalyticsPropertyType.StringValue,
            "question_count" to AnalyticsPropertyType.IntValue,
            "correct_count" to AnalyticsPropertyType.IntValue,
        ),
        optional = mapOf(
            "attempt_number" to AnalyticsPropertyType.IntValue,
            "duration_seconds_bucket" to AnalyticsPropertyType.StringValue,
        ),
    ),
    AnalyticsEventName.AiExplainRequest to AnalyticsEventSchema(
        required = mapOf(
            "story_id" to AnalyticsPropertyType.StringValue,
            "request_type" to AnalyticsPropertyType.StringValue,
            "safety_outcome" to AnalyticsPropertyType.StringValue,
        ),
        optional = mapOf(
            "target_type" to AnalyticsPropertyType.StringValue,
        ),
    ),
    AnalyticsEventName.StoryComplete to AnalyticsEventSchema(
        required = mapOf(
            "story_id" to AnalyticsPropertyType.StringValue,
            "story_order" to AnalyticsPropertyType.IntValue,
            "content_level" to AnalyticsPropertyType.IntValue,
        ),
        optional = mapOf(
            "active_reading_seconds_bucket" to AnalyticsPropertyType.StringValue,
            "quiz_completed" to AnalyticsPropertyType.BooleanValue,
        ),
    ),
    AnalyticsEventName.ParentReportOpen to AnalyticsEventSchema(
        required = mapOf(
            "entry_point" to AnalyticsPropertyType.StringValue,
            "report_period" to AnalyticsPropertyType.StringValue,
        ),
        optional = mapOf(
            "days_since_first_open" to AnalyticsPropertyType.IntValue,
        ),
    ),
)

private fun JsonElement.matches(expectedType: AnalyticsPropertyType): Boolean {
    val primitive = runCatching { jsonPrimitive }.getOrNull() ?: return false
    return when (expectedType) {
        AnalyticsPropertyType.StringValue -> primitive.isString
        AnalyticsPropertyType.IntValue -> !primitive.isString &&
            primitive.content.toIntOrNull() != null
        AnalyticsPropertyType.LongValue -> !primitive.isString &&
            primitive.content.toLongOrNull() != null
        AnalyticsPropertyType.DoubleValue -> !primitive.isString &&
            primitive.content.toDoubleOrNull() != null
        AnalyticsPropertyType.BooleanValue -> !primitive.isString &&
            primitive.booleanOrNull != null
    }
}
