package com.littlemandarin.classics.shared.progress

import platform.Foundation.NSUserDefaults

actual fun createPlatformProgressService(): ProgressService =
    StoredProgressService(IosCompletionRecordStore())

private class IosCompletionRecordStore(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : CompletionRecordStore {
    override fun readRecords(): List<CompletionRecord> =
        CompletionRecordJsonCodec.decode(userDefaults.stringForKey(CompletionRecordsKey))

    override fun writeRecords(records: List<CompletionRecord>) {
        userDefaults.setObject(
            CompletionRecordJsonCodec.encode(records),
            forKey = CompletionRecordsKey,
        )
    }

    override fun clearRecords() {
        userDefaults.removeObjectForKey(CompletionRecordsKey)
    }

    private companion object {
        const val CompletionRecordsKey: String = "completion_records"
    }
}
