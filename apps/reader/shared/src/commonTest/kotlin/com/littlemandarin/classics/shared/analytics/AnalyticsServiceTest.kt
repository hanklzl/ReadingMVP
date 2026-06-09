package com.littlemandarin.classics.shared.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive

class AnalyticsServiceTest {
    @Test
    fun trackCreatesEventWithRequiredCommonFieldsAndSupportedProperties() = runTest {
        val analytics = InMemoryAnalyticsService(
            context = AnalyticsContext(
                anonymousInstallId = "install-1",
                sessionId = "session-1",
                platform = AnalyticsPlatform.Android,
                appVersion = "1.0.0",
                schemaVersion = 1,
                uiLocale = "en",
            ),
            runtime = FixedAnalyticsRuntime(),
        )

        val event = analytics.track(
            eventName = AnalyticsEventName.QuizComplete,
            properties = mapOf(
                "story_id" to AnalyticsProperties.string("peach-garden-oath"),
                "question_count" to AnalyticsProperties.int(3),
                "correct_count" to AnalyticsProperties.int(2),
                "attempt_number" to AnalyticsProperties.int(1),
            ),
        )

        assertEquals("event-1", event.eventId)
        assertEquals(AnalyticsEventName.QuizComplete, event.eventName)
        assertEquals("2026-06-10T00:00:00Z", event.eventTimestampUtc)
        assertEquals("install-1", event.anonymousInstallId)
        assertEquals("session-1", event.sessionId)
        assertEquals(AnalyticsPlatform.Android, event.platform)
        assertEquals("1.0.0", event.appVersion)
        assertEquals(1, event.schemaVersion)
        assertEquals("en", event.uiLocale)
        assertEquals(
            "peach-garden-oath",
            event.properties.getValue("story_id").jsonPrimitive.content,
        )

        val encoded = AnalyticsEventJsonCodec.encode(listOf(event))

        assertTrue(encoded.contains("\"event_name\":\"quiz_complete\""))
        assertTrue(encoded.contains("\"event_timestamp_utc\":\"2026-06-10T00:00:00Z\""))
        assertTrue(encoded.contains("\"story_id\":\"peach-garden-oath\""))
    }

    @Test
    fun trackRejectsRawPromptOrFreeTextProperties() = runTest {
        val analytics = InMemoryAnalyticsService(
            context = AnalyticsContext(
                anonymousInstallId = "install-1",
                sessionId = "session-1",
                platform = AnalyticsPlatform.Ios,
                appVersion = "1.0.0",
                schemaVersion = 1,
                uiLocale = "zh-Hans",
            ),
            runtime = FixedAnalyticsRuntime(),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            analytics.track(
                eventName = AnalyticsEventName.AiExplainRequest,
                properties = mapOf(
                    "story_id" to AnalyticsProperties.string("peach-garden-oath"),
                    "request_type" to AnalyticsProperties.string("explain_word"),
                    "safety_outcome" to AnalyticsProperties.string("allowed"),
                    "raw_prompt" to AnalyticsProperties.string("这个词是什么意思？"),
                ),
            )
        }

        assertTrue(error.message.orEmpty().contains("raw_prompt"))
    }

    @Test
    fun storedAnalyticsPersistsEventsAndAnonymousInstallId() = runTest {
        val store = FakeAnalyticsStore()
        val writer = StoredAnalyticsService(
            store = store,
            runtime = FixedAnalyticsRuntime(
                uuids = listOf("install-1", "session-1", "event-1"),
            ),
            appVersion = "1.0.0",
            uiLocale = "en",
        )

        writer.track(
            eventName = AnalyticsEventName.AppOpen,
            properties = mapOf(
                "open_type" to AnalyticsProperties.string("cold_start"),
                "is_first_open" to AnalyticsProperties.boolean(true),
            ),
        )

        val reader = StoredAnalyticsService(
            store = store,
            runtime = FixedAnalyticsRuntime(
                uuids = listOf("session-2"),
            ),
            appVersion = "1.0.0",
            uiLocale = "en",
        )

        val events = reader.getEvents()
        assertEquals(1, events.size)
        assertEquals("install-1", events.first().anonymousInstallId)
        assertEquals("session-1", events.first().sessionId)
        assertEquals("install-1", store.readAnonymousInstallId())
    }
}

private class FixedAnalyticsRuntime(
    private val uuids: List<String> = listOf("event-1"),
) : AnalyticsRuntime {
    private var index = 0

    override fun newUuid(): String {
        val value = uuids.getOrElse(index) { uuids.last() }
        index += 1
        return value
    }

    override fun nowUtcIsoString(): String = "2026-06-10T00:00:00Z"

    override fun platform(): AnalyticsPlatform = AnalyticsPlatform.Android
}

private class FakeAnalyticsStore : AnalyticsStore {
    private var events: List<AnalyticsEvent> = emptyList()
    private var anonymousInstallId: String? = null

    override fun readEvents(): List<AnalyticsEvent> = events

    override fun writeEvents(events: List<AnalyticsEvent>) {
        this.events = events
    }

    override fun clearEvents() {
        events = emptyList()
    }

    override fun readAnonymousInstallId(): String? = anonymousInstallId

    override fun writeAnonymousInstallId(anonymousInstallId: String) {
        this.anonymousInstallId = anonymousInstallId
    }
}
