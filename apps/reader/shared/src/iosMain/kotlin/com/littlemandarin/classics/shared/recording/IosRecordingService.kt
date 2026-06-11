package com.littlemandarin.classics.shared.recording

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

internal actual fun createPlatformVoiceRecordingStore(): VoiceRecordingMetadataStore =
    IosVoiceRecordingMetadataStore()

actual fun createPlatformVoiceRecordingService(
    retentionPolicy: RecordingRetentionPolicy,
): VoiceRecordingService = StoredVoiceRecordingService(
    store = createPlatformVoiceRecordingStore(),
    retentionPolicy = retentionPolicy,
)

@OptIn(ExperimentalForeignApi::class)
private class IosVoiceRecordingMetadataStore : VoiceRecordingMetadataStore {
    override fun readRecordings(): List<VoiceRecording> {
        val path = metadataFilePath() ?: return emptyList()
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return emptyList()

        return VoiceRecordingJsonCodec.decode(
            NSString.stringWithContentsOfFile(
                path = path,
                encoding = NSUTF8StringEncoding,
                error = null,
            ),
        )
    }

    override fun writeRecordings(recordings: List<VoiceRecording>) {
        val path = metadataFilePath() ?: return
        NSString.create(string = VoiceRecordingJsonCodec.encode(recordings)).writeToFile(
            path = path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
    }

    override fun clearRecordings() {
        val path = metadataFilePath() ?: return
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }

    private fun metadataFilePath(): String? {
        val applicationSupportUrl = NSFileManager.defaultManager
            .URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask)
            .firstOrNull() as? NSURL ?: return null
        val recordingDirectoryUrl = applicationSupportUrl.URLByAppendingPathComponent(
            pathComponent = RecordingDirectory,
            isDirectory = true,
        ) ?: return null

        NSFileManager.defaultManager.createDirectoryAtURL(
            url = recordingDirectoryUrl,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )

        return recordingDirectoryUrl.URLByAppendingPathComponent(RecordingMetadataFileName)?.path
    }

    private companion object {
        const val RecordingDirectory: String = "recording"
        const val RecordingMetadataFileName: String = "recording_metadata.json"
    }
}
