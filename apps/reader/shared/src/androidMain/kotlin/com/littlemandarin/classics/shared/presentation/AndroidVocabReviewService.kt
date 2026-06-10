package com.littlemandarin.classics.shared.presentation

import android.content.Context
import android.content.SharedPreferences

object AndroidVocabReviewServiceProvider {
    private const val PreferencesName: String = "little_mandarin_vocab_review"

    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun sharedPreferences(): SharedPreferences {
        val context = applicationContext
            ?: error(
                "AndroidVocabReviewServiceProvider.initialize(context) must be called before " +
                    "createPlatformVocabReviewService().",
            )

        return context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    }
}

actual fun createPlatformVocabReviewService(): VocabReviewService =
    StoredVocabReviewService(AndroidVocabReviewRecordStore(AndroidVocabReviewServiceProvider.sharedPreferences()))

private class AndroidVocabReviewRecordStore(
    private val sharedPreferences: SharedPreferences,
) : VocabReviewRecordStore {
    override fun readRecords(): List<VocabSrsRecord> =
        VocabReviewRecordJsonCodec.decode(sharedPreferences.getString(RecordsKey, null))

    override fun writeRecords(records: List<VocabSrsRecord>) {
        sharedPreferences.edit()
            .putString(RecordsKey, VocabReviewRecordJsonCodec.encode(records))
            .apply()
    }

    override fun clearRecords() {
        sharedPreferences.edit()
            .remove(RecordsKey)
            .apply()
    }

    private companion object {
        const val RecordsKey: String = "vocab_srs_records"
    }
}
