package com.littlemandarin.classics.shared.progress

import android.content.Context
import android.content.SharedPreferences

object AndroidProgressServiceProvider {
    private const val PreferencesName: String = "little_mandarin_progress"

    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun sharedPreferences(): SharedPreferences {
        val context = applicationContext
            ?: error(
                "AndroidProgressServiceProvider.initialize(context) must be called before " +
                    "createPlatformProgressService().",
            )

        return context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    }
}

actual fun createPlatformProgressService(): ProgressService =
    StoredProgressService(AndroidCompletionRecordStore(AndroidProgressServiceProvider.sharedPreferences()))

private class AndroidCompletionRecordStore(
    private val sharedPreferences: SharedPreferences,
) : CompletionRecordStore {
    override fun readRecords(): List<CompletionRecord> =
        CompletionRecordJsonCodec.decode(
            sharedPreferences.getString(CompletionRecordsKey, null),
        )

    override fun writeRecords(records: List<CompletionRecord>) {
        sharedPreferences.edit()
            .putString(CompletionRecordsKey, CompletionRecordJsonCodec.encode(records))
            .apply()
    }

    override fun clearRecords() {
        sharedPreferences.edit()
            .remove(CompletionRecordsKey)
            .apply()
    }

    private companion object {
        const val CompletionRecordsKey: String = "completion_records"
    }
}
