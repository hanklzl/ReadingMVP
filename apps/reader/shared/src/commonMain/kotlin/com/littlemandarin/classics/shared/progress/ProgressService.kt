package com.littlemandarin.classics.shared.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ProgressService {
    val records: Flow<List<CompletionRecord>>

    suspend fun getRecords(): List<CompletionRecord>

    suspend fun markCompleted(record: CompletionRecord)

    suspend fun completedCount(): Int

    suspend fun clear()
}

class InMemoryProgressService(
    initialRecords: List<CompletionRecord> = emptyList(),
) : ProgressService {
    private val state = MutableStateFlow(initialRecords.latestCompletionRecordsByStory())

    override val records: Flow<List<CompletionRecord>> = state

    override suspend fun getRecords(): List<CompletionRecord> = state.value

    override suspend fun markCompleted(record: CompletionRecord) {
        state.value = state.value.withLatestCompletionRecord(record)
    }

    override suspend fun completedCount(): Int = state.value.size

    override suspend fun clear() {
        state.value = emptyList()
    }
}

expect fun createPlatformProgressService(): ProgressService

internal interface CompletionRecordStore {
    fun readRecords(): List<CompletionRecord>

    fun writeRecords(records: List<CompletionRecord>)

    fun clearRecords()
}

internal class StoredProgressService(
    private val store: CompletionRecordStore,
) : ProgressService {
    private val state = MutableStateFlow(store.readRecords().latestCompletionRecordsByStory())

    override val records: Flow<List<CompletionRecord>> = state

    override suspend fun getRecords(): List<CompletionRecord> = refreshRecords()

    override suspend fun markCompleted(record: CompletionRecord) {
        val updatedRecords = store.readRecords()
            .withLatestCompletionRecord(record)
        store.writeRecords(updatedRecords)
        state.value = updatedRecords
    }

    override suspend fun completedCount(): Int = getRecords().size

    override suspend fun clear() {
        store.clearRecords()
        state.value = emptyList()
    }

    private fun refreshRecords(): List<CompletionRecord> {
        val latestRecords = store.readRecords().latestCompletionRecordsByStory()
        state.value = latestRecords
        return latestRecords
    }
}

internal fun List<CompletionRecord>.withLatestCompletionRecord(
    record: CompletionRecord,
): List<CompletionRecord> {
    val latestRecord = (filter { it.storyId == record.storyId } + record)
        .reduce { current, candidate ->
            if (candidate.completedAtEpochMillis >= current.completedAtEpochMillis) {
                candidate
            } else {
                current
            }
        }

    return filterNot { it.storyId == record.storyId } + latestRecord
}

internal fun List<CompletionRecord>.latestCompletionRecordsByStory(): List<CompletionRecord> {
    val recordsByStory = linkedMapOf<String, CompletionRecord>()
    for (record in this) {
        val current = recordsByStory[record.storyId]
        if (
            current == null ||
            record.completedAtEpochMillis >= current.completedAtEpochMillis
        ) {
            recordsByStory[record.storyId] = record
        }
    }
    return recordsByStory.values.toList()
}

internal object CompletionRecordJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(records: List<CompletionRecord>): String =
        json.encodeToString(records.latestCompletionRecordsByStory())

    fun decode(encodedRecords: String?): List<CompletionRecord> {
        if (encodedRecords.isNullOrBlank()) return emptyList()

        return runCatching {
            json.decodeFromString<List<CompletionRecord>>(encodedRecords)
                .latestCompletionRecordsByStory()
        }.getOrElse {
            emptyList()
        }
    }
}
