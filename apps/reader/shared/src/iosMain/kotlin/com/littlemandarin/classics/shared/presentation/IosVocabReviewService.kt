package com.littlemandarin.classics.shared.presentation

import platform.Foundation.NSUserDefaults

actual fun createPlatformVocabReviewService(): VocabReviewService =
    StoredVocabReviewService(IosVocabReviewRecordStore())

private class IosVocabReviewRecordStore(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : VocabReviewRecordStore {
    override fun readRecords(): List<VocabSrsRecord> =
        VocabReviewRecordJsonCodec.decode(userDefaults.stringForKey(RecordsKey))

    override fun writeRecords(records: List<VocabSrsRecord>) {
        userDefaults.setObject(
            VocabReviewRecordJsonCodec.encode(records),
            forKey = RecordsKey,
        )
    }

    override fun clearRecords() {
        userDefaults.removeObjectForKey(RecordsKey)
    }

    private companion object {
        const val RecordsKey: String = "lmc_vocab_srs_records"
    }
}
