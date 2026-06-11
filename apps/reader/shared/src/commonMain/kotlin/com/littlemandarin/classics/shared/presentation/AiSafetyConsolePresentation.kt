package com.littlemandarin.classics.shared.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Whether a controlled-AI explanation was answered or gently declined (out of scope). */
@Serializable
enum class AiInteractionOutcome {
    Allowed,
    OutOfScope,
}

/**
 * One controlled-AI interaction, logged locally for parent oversight (competitive
 * Top-12 #10). Fields are story content (word/sentence + a short answer preview) and
 * metadata — never child PII. The parent can review and delete these.
 */
@Serializable
data class AiInteractionRecord(
    val id: String,
    val storyId: String,
    val questionType: String,
    val query: String,
    val answerPreview: String,
    val outcome: AiInteractionOutcome,
    val epochMillis: Long,
)

/** A short summary for the safety console header. */
data class AiSafetyConsoleSummary(
    val totalCount: Int,
    val outOfScopeCount: Int,
    val lastEpochMillis: Long?,
)

object AiSafetyConsolePolicy {
    /** Keep only the most recent interactions on-device (data minimisation). */
    const val MaxRecords: Int = 200
}

/**
 * Pure helpers for the parent AI-safety console: append (newest-first, capped),
 * delete, and summarise the local interaction log. No side effects.
 */
class AiSafetyConsoleUseCases {
    fun record(
        existing: List<AiInteractionRecord>,
        new: AiInteractionRecord,
        maxRecords: Int = AiSafetyConsolePolicy.MaxRecords,
    ): List<AiInteractionRecord> =
        (listOf(new) + existing.filter { it.id != new.id }).take(maxRecords.coerceAtLeast(0))

    fun delete(existing: List<AiInteractionRecord>, id: String): List<AiInteractionRecord> =
        existing.filter { it.id != id }

    fun clear(): List<AiInteractionRecord> = emptyList()

    fun summary(records: List<AiInteractionRecord>): AiSafetyConsoleSummary =
        AiSafetyConsoleSummary(
            totalCount = records.size,
            outOfScopeCount = records.count { it.outcome == AiInteractionOutcome.OutOfScope },
            lastEpochMillis = records.maxOfOrNull { it.epochMillis },
        )
}

interface AiInteractionLogService {
    val records: Flow<List<AiInteractionRecord>>

    suspend fun read(): List<AiInteractionRecord>

    suspend fun append(record: AiInteractionRecord)

    suspend fun deleteById(id: String)

    suspend fun clear()
}

class InMemoryAiInteractionLogService(
    initialRecords: List<AiInteractionRecord> = emptyList(),
) : AiInteractionLogService {
    private val useCases = AiSafetyConsoleUseCases()
    private val state = MutableStateFlow(initialRecords)

    override val records: Flow<List<AiInteractionRecord>> = state

    override suspend fun read(): List<AiInteractionRecord> = state.value

    override suspend fun append(record: AiInteractionRecord) {
        state.value = useCases.record(state.value, record)
    }

    override suspend fun deleteById(id: String) {
        state.value = useCases.delete(state.value, id)
    }

    override suspend fun clear() {
        state.value = useCases.clear()
    }
}

internal interface AiInteractionLogStore {
    fun readLog(): List<AiInteractionRecord>

    fun writeLog(records: List<AiInteractionRecord>)

    fun clearLog()
}

internal class StoredAiInteractionLogService(
    private val store: AiInteractionLogStore,
) : AiInteractionLogService {
    private val useCases = AiSafetyConsoleUseCases()
    private val state = MutableStateFlow(store.readLog())

    override val records: Flow<List<AiInteractionRecord>> = state

    override suspend fun read(): List<AiInteractionRecord> {
        val latest = store.readLog()
        state.value = latest
        return latest
    }

    override suspend fun append(record: AiInteractionRecord) {
        val updated = useCases.record(state.value, record)
        store.writeLog(updated)
        state.value = updated
    }

    override suspend fun deleteById(id: String) {
        val updated = useCases.delete(state.value, id)
        store.writeLog(updated)
        state.value = updated
    }

    override suspend fun clear() {
        store.clearLog()
        state.value = emptyList()
    }
}

object AiInteractionLogJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(records: List<AiInteractionRecord>): String = json.encodeToString(records)

    fun decode(encoded: String?): List<AiInteractionRecord> {
        if (encoded.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<AiInteractionRecord>>(encoded) }
            .getOrElse { emptyList() }
    }
}
