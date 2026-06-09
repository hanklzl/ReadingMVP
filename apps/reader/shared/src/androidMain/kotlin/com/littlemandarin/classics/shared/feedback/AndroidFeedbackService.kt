package com.littlemandarin.classics.shared.feedback

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object AndroidFeedbackServiceProvider {
    private const val PreferencesName: String = "little_mandarin_feedback"

    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun sharedPreferences(): SharedPreferences {
        val context = applicationContext
            ?: error(
                "AndroidFeedbackServiceProvider.initialize(context) must be called before " +
                    "createPlatformFeedbackService().",
            )

        return context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    }
}

actual object PlatformFeedbackRuntime : FeedbackRuntime {
    override fun newUuid(): String = UUID.randomUUID().toString()

    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}

actual fun createPlatformFeedbackService(): FeedbackService =
    StoredFeedbackService(AndroidFeedbackStore(AndroidFeedbackServiceProvider.sharedPreferences()))

private class AndroidFeedbackStore(
    private val sharedPreferences: SharedPreferences,
) : FeedbackStore {
    override fun readFeedback(): List<FeedbackEntry> =
        FeedbackJsonCodec.decode(sharedPreferences.getString(FeedbackKey, null))

    override fun writeFeedback(feedback: List<FeedbackEntry>) {
        sharedPreferences.edit()
            .putString(FeedbackKey, FeedbackJsonCodec.encode(feedback))
            .apply()
    }

    override fun clearFeedback() {
        sharedPreferences.edit()
            .remove(FeedbackKey)
            .apply()
    }

    private companion object {
        const val FeedbackKey: String = "feedback_entries"
    }
}
