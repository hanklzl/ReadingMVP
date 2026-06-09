package com.littlemandarin.classics.shared.feedback

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
actual object PlatformFeedbackRuntime : FeedbackRuntime {
    override fun newUuid(): String = NSUUID().UUIDString

    override fun nowEpochMillis(): Long = time(null) * 1_000L
}

actual fun createPlatformFeedbackService(): FeedbackService =
    StoredFeedbackService(IosFeedbackStore())

private class IosFeedbackStore(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : FeedbackStore {
    override fun readFeedback(): List<FeedbackEntry> =
        FeedbackJsonCodec.decode(userDefaults.stringForKey(FeedbackKey))

    override fun writeFeedback(feedback: List<FeedbackEntry>) {
        userDefaults.setObject(
            FeedbackJsonCodec.encode(feedback),
            forKey = FeedbackKey,
        )
    }

    override fun clearFeedback() {
        userDefaults.removeObjectForKey(FeedbackKey)
    }

    private companion object {
        const val FeedbackKey: String = "feedback_entries"
    }
}
