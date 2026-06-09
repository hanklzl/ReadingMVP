package com.littlemandarin.classics.shared.analytics

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object AndroidAnalyticsServiceProvider {
    private const val PreferencesName: String = "little_mandarin_analytics"

    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun sharedPreferences(): SharedPreferences {
        val context = applicationContext
            ?: error(
                "AndroidAnalyticsServiceProvider.initialize(context) must be called before " +
                    "createPlatformAnalytics().",
            )

        return context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    }
}

actual object PlatformAnalyticsRuntime : AnalyticsRuntime {
    override fun newUuid(): String = UUID.randomUUID().toString()

    override fun nowUtcIsoString(): String {
        val formatter = SimpleDateFormat(UtcDateFormat, Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    override fun platform(): AnalyticsPlatform = AnalyticsPlatform.Android

    private const val UtcDateFormat: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
}

actual fun createPlatformAnalytics(
    appVersion: String,
    uiLocale: String,
): Analytics = StoredAnalyticsService(
    store = AndroidAnalyticsStore(AndroidAnalyticsServiceProvider.sharedPreferences()),
    runtime = PlatformAnalyticsRuntime,
    appVersion = appVersion,
    uiLocale = uiLocale,
)

private class AndroidAnalyticsStore(
    private val sharedPreferences: SharedPreferences,
) : AnalyticsStore {
    override fun readEvents(): List<AnalyticsEvent> =
        AnalyticsEventJsonCodec.decode(sharedPreferences.getString(EventsKey, null))

    override fun writeEvents(events: List<AnalyticsEvent>) {
        sharedPreferences.edit()
            .putString(EventsKey, AnalyticsEventJsonCodec.encode(events))
            .apply()
    }

    override fun clearEvents() {
        sharedPreferences.edit()
            .remove(EventsKey)
            .apply()
    }

    override fun readAnonymousInstallId(): String? =
        sharedPreferences.getString(AnonymousInstallIdKey, null)

    override fun writeAnonymousInstallId(anonymousInstallId: String) {
        sharedPreferences.edit()
            .putString(AnonymousInstallIdKey, anonymousInstallId)
            .apply()
    }

    private companion object {
        const val EventsKey: String = "analytics_events"
        const val AnonymousInstallIdKey: String = "anonymous_install_id"
    }
}
