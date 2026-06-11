package com.littlemandarin.classics.shared.recording

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DefaultMaxRecordingsPerStory: Int = 12
private const val DefaultMaxRecordingsOverall: Int = 60

sealed interface RecordingState {
    val storyId: String?
    val paragraphIndex: Int?
    val activeRecordingId: String?

    data object Idle : RecordingState {
        override val storyId: String? = null
        override val paragraphIndex: Int? = null
        override val activeRecordingId: String? = null
    }

    data class Requesting(
        override val storyId: String,
        override val paragraphIndex: Int,
    ) : RecordingState {
        override val activeRecordingId: String? = null
    }

    data class Recording(
        override val storyId: String,
        override val paragraphIndex: Int,
    ) : RecordingState {
        override val activeRecordingId: String? = null
    }

    data class Stopped(
        override val storyId: String,
        override val paragraphIndex: Int,
        override val activeRecordingId: String?,
    ) : RecordingState

    data class Playing(
        override val storyId: String,
        override val paragraphIndex: Int,
        override val activeRecordingId: String,
    ) : RecordingState
}

sealed interface RecordingAction {
    data class Request(
        val storyId: String,
        val paragraphIndex: Int,
    ) : RecordingAction

    data object PermissionGranted : RecordingAction

    data object PermissionDenied : RecordingAction

    data class Stopped(
        val recordingId: String,
    ) : RecordingAction

    data class Playing(
        val recordingId: String,
    ) : RecordingAction

    data object Completed : RecordingAction

    data object Reset : RecordingAction
}

class RecordingStateMachine {
    fun reduce(
        state: RecordingState,
        action: RecordingAction,
    ): RecordingState = when (state) {
        is RecordingState.Idle -> when (action) {
            is RecordingAction.Request -> action.toRequestingState()
            else -> state
        }

        is RecordingState.Requesting -> when (action) {
            is RecordingAction.PermissionGranted -> RecordingState.Recording(
                storyId = state.storyId,
                paragraphIndex = state.paragraphIndex,
            )
            is RecordingAction.PermissionDenied, RecordingAction.Reset -> RecordingState.Idle
            is RecordingAction.Request -> action.toRequestingState()
            else -> state
        }

        is RecordingState.Recording -> when (action) {
            is RecordingAction.Stopped -> RecordingState.Stopped(
                storyId = state.storyId,
                paragraphIndex = state.paragraphIndex,
                activeRecordingId = action.recordingId,
            )
            is RecordingAction.Request -> action.toRequestingState()
            RecordingAction.Reset -> RecordingState.Idle
            else -> state
        }

        is RecordingState.Stopped -> when (action) {
            is RecordingAction.Playing -> RecordingState.Playing(
                storyId = state.storyId,
                paragraphIndex = state.paragraphIndex,
                activeRecordingId = action.recordingId,
            )
            is RecordingAction.Request -> action.toRequestingState()
            is RecordingAction.Reset -> RecordingState.Idle
            else -> state
        }

        is RecordingState.Playing -> when (action) {
            is RecordingAction.Completed -> RecordingState.Stopped(
                storyId = state.storyId,
                paragraphIndex = state.paragraphIndex,
                activeRecordingId = state.activeRecordingId,
            )
            is RecordingAction.Request -> action.toRequestingState()
            is RecordingAction.Reset -> RecordingState.Idle
            is RecordingAction.Playing -> state.copy(
                activeRecordingId = action.recordingId,
            )
            else -> state
        }
    }

    private fun RecordingAction.Request.toRequestingState(): RecordingState.Requesting =
        RecordingState.Requesting(
            storyId = storyId,
            paragraphIndex = paragraphIndex,
        )
}

@Serializable
data class VoiceRecording(
    val id: String,
    val storyId: String,
    @SerialName("paragraph_index")
    val paragraphIndex: Int,
    @SerialName("file_path")
    val filePath: String,
    @SerialName("duration_ms")
    val durationMs: Long,
    @SerialName("created_at_epoch_millis")
    val createdAtEpochMillis: Long,
)

data class RecordingRetentionPolicy(
    val maxPerStory: Int = DefaultMaxRecordingsPerStory,
    val maxOverall: Int = DefaultMaxRecordingsOverall,
) {
    init {
        require(maxPerStory > 0) { "maxPerStory must be greater than zero" }
        require(maxOverall > 0) { "maxOverall must be greater than zero" }
    }
}

data class RecordingStorageResult(
    val retained: List<VoiceRecording>,
    val removed: List<VoiceRecording>,
)

fun List<VoiceRecording>.newestFirst(): List<VoiceRecording> = sortedWith(
    compareByDescending<VoiceRecording> { it.createdAtEpochMillis }
        .thenByDescending { it.id },
)

fun List<VoiceRecording>.applyRetention(
    policy: RecordingRetentionPolicy,
): RecordingStorageResult {
    val sorted = this.newestFirst()
    val selectedByStory = HashMap<String, Int>()
    val retained = ArrayList<VoiceRecording>()
    val removed = ArrayList<VoiceRecording>()

    for (recording in sorted) {
        val currentCount = selectedByStory[recording.storyId] ?: 0
        if (currentCount >= policy.maxPerStory) {
            removed.add(recording)
            continue
        }
        retained.add(recording)
        selectedByStory[recording.storyId] = currentCount + 1
    }

    val overflow = retained.drop(policy.maxOverall)
    if (overflow.isNotEmpty()) {
        removed.addAll(overflow)
    }

    return RecordingStorageResult(
        retained = retained.take(policy.maxOverall),
        removed = removed,
    )
}

interface VoiceRecordingService {
    val recordings: Flow<List<VoiceRecording>>

    suspend fun getRecordings(storyId: String? = null): List<VoiceRecording>

    suspend fun add(recording: VoiceRecording): RecordingStorageResult

    suspend fun delete(recordingId: String): RecordingStorageResult

    suspend fun clearAll(): RecordingStorageResult
}

class InMemoryVoiceRecordingService(
    initialRecordings: List<VoiceRecording> = emptyList(),
    private val retentionPolicy: RecordingRetentionPolicy = RecordingRetentionPolicy(),
) : VoiceRecordingService {
    private val state = MutableStateFlow(
        initialRecordings.applyRetention(retentionPolicy).retained,
    )

    override val recordings: Flow<List<VoiceRecording>> = state

    override suspend fun getRecordings(storyId: String?): List<VoiceRecording> {
        val current = state.value
        return if (storyId == null) {
            current
        } else {
            current.filter { it.storyId == storyId }
        }
    }

    override suspend fun add(recording: VoiceRecording): RecordingStorageResult {
        val updated = (state.value.filterNot { it.id == recording.id } + recording)
            .applyRetention(retentionPolicy)

        state.value = updated.retained
        return updated
    }

    override suspend fun delete(recordingId: String): RecordingStorageResult {
        val current = state.value
        val removed = current.filter { it.id == recordingId }
        val retained = current.filterNot { it.id == recordingId }
        state.value = retained
        return RecordingStorageResult(
            retained = retained,
            removed = removed,
        )
    }

    override suspend fun clearAll(): RecordingStorageResult {
        val removed = state.value
        state.value = emptyList()
        return RecordingStorageResult(
            retained = emptyList(),
            removed = removed,
        )
    }
}

internal expect fun createPlatformVoiceRecordingStore(): VoiceRecordingMetadataStore

expect fun createPlatformVoiceRecordingService(
    retentionPolicy: RecordingRetentionPolicy = RecordingRetentionPolicy(),
): VoiceRecordingService

interface VoiceRecordingMetadataStore {
    fun readRecordings(): List<VoiceRecording>

    fun writeRecordings(recordings: List<VoiceRecording>)

    fun clearRecordings()
}

internal class StoredVoiceRecordingService(
    private val store: VoiceRecordingMetadataStore,
    private val retentionPolicy: RecordingRetentionPolicy = RecordingRetentionPolicy(),
) : VoiceRecordingService {
    private val state = MutableStateFlow(
        store.readRecordings().applyRetention(retentionPolicy).retained,
    )

    override val recordings: Flow<List<VoiceRecording>> = state

    override suspend fun getRecordings(storyId: String?): List<VoiceRecording> {
        val current = state.value
        return if (storyId == null) {
            current
        } else {
            current.filter { it.storyId == storyId }
        }
    }

    override suspend fun add(recording: VoiceRecording): RecordingStorageResult {
        val updated = (state.value.filterNot { it.id == recording.id } + recording)
            .applyRetention(retentionPolicy)

        state.value = updated.retained
        store.writeRecordings(updated.retained)
        return updated
    }

    override suspend fun delete(recordingId: String): RecordingStorageResult {
        val current = state.value
        val removed = current.filter { it.id == recordingId }
        val retained = current.filterNot { it.id == recordingId }

        state.value = retained
        if (current != retained) {
            store.writeRecordings(retained)
        }

        return RecordingStorageResult(
            retained = retained,
            removed = removed,
        )
    }

    override suspend fun clearAll(): RecordingStorageResult {
        val removed = state.value
        state.value = emptyList()
        store.clearRecordings()

        return RecordingStorageResult(
            retained = emptyList(),
            removed = removed,
        )
    }
}

fun createDefaultRecordingService(
    retentionPolicy: RecordingRetentionPolicy = RecordingRetentionPolicy(),
): VoiceRecordingService = StoredVoiceRecordingService(
    store = createPlatformVoiceRecordingStore(),
    retentionPolicy = retentionPolicy,
)

object VoiceRecordingJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(recordings: List<VoiceRecording>): String =
        json.encodeToString(recordings.newestFirst())

    fun decode(encodedRecordings: String?): List<VoiceRecording> {
        if (encodedRecordings.isNullOrBlank()) return emptyList()

        return runCatching {
            json.decodeFromString<List<VoiceRecording>>(encodedRecordings)
        }.getOrElse {
            emptyList()
        }.newestFirst()
    }
}
