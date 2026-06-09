package com.littlemandarin.classics.shared.analytics

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

actual object PlatformAnalyticsRuntime : AnalyticsRuntime {
    override fun newUuid(): String = NSUUID().UUIDString

    override fun nowUtcIsoString(): String {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale(localeIdentifier = "en_US_POSIX")
        formatter.dateFormat = UtcDateFormat
        return formatter.stringFromDate(NSDate())
    }

    override fun platform(): AnalyticsPlatform = AnalyticsPlatform.Ios

    private const val UtcDateFormat: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
}

actual fun createPlatformAnalytics(
    appVersion: String,
    uiLocale: String,
): Analytics = StoredAnalyticsService(
    store = IosAnalyticsStore(),
    runtime = PlatformAnalyticsRuntime,
    appVersion = appVersion,
    uiLocale = uiLocale,
)

private class IosAnalyticsStore(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : AnalyticsStore {
    override fun readEvents(): List<AnalyticsEvent> =
        AnalyticsEventJsonCodec.decode(userDefaults.stringForKey(EventsKey))

    override fun writeEvents(events: List<AnalyticsEvent>) {
        userDefaults.setObject(
            AnalyticsEventJsonCodec.encode(events),
            forKey = EventsKey,
        )
    }

    override fun clearEvents() {
        userDefaults.removeObjectForKey(EventsKey)
    }

    override fun readAnonymousInstallId(): String? =
        userDefaults.stringForKey(AnonymousInstallIdKey)

    override fun writeAnonymousInstallId(anonymousInstallId: String) {
        userDefaults.setObject(
            anonymousInstallId,
            forKey = AnonymousInstallIdKey,
        )
    }

    private companion object {
        const val EventsKey: String = "analytics_events"
        const val AnonymousInstallIdKey: String = "anonymous_install_id"
    }
}
