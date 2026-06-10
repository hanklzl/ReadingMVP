package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.progress.ProgressService
import com.littlemandarin.classics.shared.story.StoryRepository
import com.littlemandarin.classics.shared.story.Vocab
import kotlin.math.ceil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class VocabReviewAssessment {
    Known,
    NeedsPractice,
}

@Serializable
data class VocabSrsRecord(
    val word: String,
    val pinyin: String,
    val meaning: String,
    val example: String? = null,
    val sourceStoryIds: List<String>,
    val ease: Double = SrsScheduler.DefaultEase,
    val intervalDays: Int = 0,
    val dueEpochDay: Int,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReviewedEpochDay: Int? = null,
)

data class WordBookSummary(
    val totalWords: Int,
    val dueCount: Int,
    val items: List<WordBookItem>,
)

data class WordBookItem(
    val word: String,
    val pinyin: String,
    val meaning: String,
    val example: String?,
    val sourceStoryIds: List<String>,
    val dueEpochDay: Int,
    val intervalDays: Int,
    val reps: Int,
    val due: Boolean,
)

class SrsScheduler {
    fun newRecord(
        vocab: Vocab,
        storyId: String,
        todayEpochMillis: Long,
    ): VocabSrsRecord = VocabSrsRecord(
        word = vocab.word,
        pinyin = vocab.pinyin,
        meaning = vocab.meaning,
        example = vocab.example,
        sourceStoryIds = listOf(storyId),
        dueEpochDay = epochDayFromMillis(todayEpochMillis),
    )

    fun isDue(
        record: VocabSrsRecord,
        todayEpochMillis: Long,
    ): Boolean = record.dueEpochDay <= epochDayFromMillis(todayEpochMillis)

    fun review(
        record: VocabSrsRecord,
        assessment: VocabReviewAssessment,
        todayEpochMillis: Long,
    ): VocabSrsRecord {
        val today = epochDayFromMillis(todayEpochMillis)
        return when (assessment) {
            VocabReviewAssessment.Known -> {
                val nextReps = record.reps + 1
                val nextInterval = when (nextReps) {
                    1 -> 1
                    2 -> 3
                    else -> ceil(record.intervalDays.coerceAtLeast(1) * record.ease)
                        .toInt()
                        .coerceAtLeast(record.intervalDays + 1)
                }
                record.copy(
                    ease = (record.ease + KnownEaseBonus).coerceAtMost(MaxEase),
                    intervalDays = nextInterval,
                    dueEpochDay = today + nextInterval,
                    reps = nextReps,
                    lastReviewedEpochDay = today,
                )
            }
            VocabReviewAssessment.NeedsPractice -> {
                val nextInterval = 1
                record.copy(
                    ease = (record.ease - NeedsPracticeEasePenalty).coerceAtLeast(MinEase),
                    intervalDays = nextInterval,
                    dueEpochDay = today + nextInterval,
                    reps = 0,
                    lapses = record.lapses + 1,
                    lastReviewedEpochDay = today,
                )
            }
        }
    }

    companion object {
        const val DefaultEase: Double = 2.5
        private const val KnownEaseBonus: Double = 0.15
        private const val NeedsPracticeEasePenalty: Double = 0.25
        private const val MinEase: Double = 1.3
        private const val MaxEase: Double = 3.0
    }
}

interface VocabReviewService {
    val records: Flow<List<VocabSrsRecord>>

    suspend fun getRecords(): List<VocabSrsRecord>

    suspend fun replaceRecords(records: List<VocabSrsRecord>)

    suspend fun upsertRecord(record: VocabSrsRecord)

    suspend fun clear()
}

class InMemoryVocabReviewService(
    initialRecords: List<VocabSrsRecord> = emptyList(),
) : VocabReviewService {
    private val state = MutableStateFlow(initialRecords.normalizedVocabRecords())

    override val records: Flow<List<VocabSrsRecord>> = state

    override suspend fun getRecords(): List<VocabSrsRecord> = state.value

    override suspend fun replaceRecords(records: List<VocabSrsRecord>) {
        state.value = records.normalizedVocabRecords()
    }

    override suspend fun upsertRecord(record: VocabSrsRecord) {
        replaceRecords(state.value.upsertVocabRecord(record))
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

expect fun createPlatformVocabReviewService(): VocabReviewService

internal interface VocabReviewRecordStore {
    fun readRecords(): List<VocabSrsRecord>

    fun writeRecords(records: List<VocabSrsRecord>)

    fun clearRecords()
}

internal class StoredVocabReviewService(
    private val store: VocabReviewRecordStore,
) : VocabReviewService {
    private val state = MutableStateFlow(store.readRecords().normalizedVocabRecords())

    override val records: Flow<List<VocabSrsRecord>> = state

    override suspend fun getRecords(): List<VocabSrsRecord> = refreshRecords()

    override suspend fun replaceRecords(records: List<VocabSrsRecord>) {
        val normalized = records.normalizedVocabRecords()
        store.writeRecords(normalized)
        state.value = normalized
    }

    override suspend fun upsertRecord(record: VocabSrsRecord) {
        replaceRecords(store.readRecords().upsertVocabRecord(record))
    }

    override suspend fun clear() {
        store.clearRecords()
        state.value = emptyList()
    }

    private fun refreshRecords(): List<VocabSrsRecord> {
        val latest = store.readRecords().normalizedVocabRecords()
        state.value = latest
        return latest
    }
}

class VocabReviewUseCase(
    private val storyRepository: StoryRepository,
    private val progressService: ProgressService,
    private val reviewService: VocabReviewService,
    private val scheduler: SrsScheduler = SrsScheduler(),
) {
    suspend fun syncLearnedWords(todayEpochMillis: Long) {
        val completedStoryIds = progressService.getRecords()
            .mapTo(mutableSetOf()) { it.storyId }
        if (completedStoryIds.isEmpty()) return

        val existingByWord = reviewService.getRecords()
            .associateBy { it.word }
            .toMutableMap()

        for (story in storyRepository.listStories()) {
            if (story.id !in completedStoryIds) continue

            for (vocab in story.vocab) {
                val existing = existingByWord[vocab.word]
                existingByWord[vocab.word] = if (existing == null) {
                    scheduler.newRecord(
                        vocab = vocab,
                        storyId = story.id,
                        todayEpochMillis = todayEpochMillis,
                    )
                } else {
                    existing.copy(
                        sourceStoryIds = (existing.sourceStoryIds + story.id).distinct(),
                        pinyin = existing.pinyin.ifBlank { vocab.pinyin },
                        meaning = existing.meaning.ifBlank { vocab.meaning },
                        example = existing.example ?: vocab.example,
                    )
                }
            }
        }

        reviewService.replaceRecords(existingByWord.values.toList())
    }

    suspend fun wordBook(todayEpochMillis: Long): WordBookSummary {
        val items = reviewService.getRecords()
            .normalizedVocabRecords()
            .map { it.toWordBookItem(scheduler.isDue(it, todayEpochMillis)) }
        return WordBookSummary(
            totalWords = items.size,
            dueCount = items.count { it.due },
            items = items,
        )
    }

    suspend fun dueReviewItems(todayEpochMillis: Long): List<WordBookItem> =
        wordBook(todayEpochMillis).items
            .filter { it.due }
            .sortedWith(compareBy<WordBookItem> { it.dueEpochDay }.thenBy { it.word })

    suspend fun review(
        word: String,
        assessment: VocabReviewAssessment,
        todayEpochMillis: Long,
    ): WordBookItem? {
        val record = reviewService.getRecords().firstOrNull { it.word == word } ?: return null
        val updated = scheduler.review(
            record = record,
            assessment = assessment,
            todayEpochMillis = todayEpochMillis,
        )
        reviewService.upsertRecord(updated)
        return updated.toWordBookItem(due = scheduler.isDue(updated, todayEpochMillis))
    }
}

internal object VocabReviewRecordJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(records: List<VocabSrsRecord>): String =
        json.encodeToString(records.normalizedVocabRecords())

    fun decode(encodedRecords: String?): List<VocabSrsRecord> {
        if (encodedRecords.isNullOrBlank()) return emptyList()

        return runCatching {
            json.decodeFromString<List<VocabSrsRecord>>(encodedRecords)
                .normalizedVocabRecords()
        }.getOrElse {
            emptyList()
        }
    }
}

internal fun epochDayFromMillis(epochMillis: Long): Int =
    (epochMillis / DayMillis).toInt()

private fun VocabSrsRecord.toWordBookItem(due: Boolean): WordBookItem = WordBookItem(
    word = word,
    pinyin = pinyin,
    meaning = meaning,
    example = example,
    sourceStoryIds = sourceStoryIds,
    dueEpochDay = dueEpochDay,
    intervalDays = intervalDays,
    reps = reps,
    due = due,
)

private fun List<VocabSrsRecord>.normalizedVocabRecords(): List<VocabSrsRecord> =
    filter { it.word.isNotBlank() }
        .groupBy { it.word }
        .map { (_, records) ->
            records.reduce { current, candidate ->
                current.copy(
                    pinyin = current.pinyin.ifBlank { candidate.pinyin },
                    meaning = current.meaning.ifBlank { candidate.meaning },
                    example = current.example ?: candidate.example,
                    sourceStoryIds = (current.sourceStoryIds + candidate.sourceStoryIds).distinct(),
                    ease = candidate.ease,
                    intervalDays = candidate.intervalDays,
                    dueEpochDay = candidate.dueEpochDay,
                    reps = candidate.reps,
                    lapses = candidate.lapses,
                    lastReviewedEpochDay = candidate.lastReviewedEpochDay,
                )
            }
        }

private fun List<VocabSrsRecord>.upsertVocabRecord(record: VocabSrsRecord): List<VocabSrsRecord> {
    val existingIndex = indexOfFirst { it.word == record.word }
    if (existingIndex < 0) return this + record

    return mapIndexed { index, existing ->
        if (index == existingIndex) record else existing
    }
}

private const val DayMillis: Long = 24L * 60L * 60L * 1_000L
