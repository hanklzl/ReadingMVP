package com.littlemandarin.classics.shared.recording

import android.content.Context
import java.io.File

object AndroidRecordingStoreProvider {
    private const val RecordingDirectory: String = "recording"
    private const val RecordingMetadataFileName: String = "recording_metadata.json"

    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun metadataFile(): File {
        val context = applicationContext
            ?: error(
                "AndroidRecordingStoreProvider.initialize(context) must be called before " +
                    "createPlatformVoiceRecordingStore().",
            )

        return File(context.filesDir, "$RecordingDirectory/$RecordingMetadataFileName").also {
            it.parentFile?.mkdirs()
        }
    }
}

internal actual fun createPlatformVoiceRecordingStore(): VoiceRecordingMetadataStore =
    AndroidVoiceRecordingMetadataStore(
        file = AndroidRecordingStoreProvider.metadataFile(),
    )

actual fun createPlatformVoiceRecordingService(
    retentionPolicy: RecordingRetentionPolicy,
): VoiceRecordingService = StoredVoiceRecordingService(
    store = createPlatformVoiceRecordingStore(),
    retentionPolicy = retentionPolicy,
)

private class AndroidVoiceRecordingMetadataStore(
    private val file: File,
) : VoiceRecordingMetadataStore {
    override fun readRecordings(): List<VoiceRecording> {
        if (!file.exists()) return emptyList()

        return runCatching {
            VoiceRecordingJsonCodec.decode(file.readText())
        }.getOrElse {
            emptyList()
        }
    }

    override fun writeRecordings(recordings: List<VoiceRecording>) {
        file.parentFile?.mkdirs()
        file.writeText(VoiceRecordingJsonCodec.encode(recordings))
    }

    override fun clearRecordings() {
        file.delete()
    }
}
