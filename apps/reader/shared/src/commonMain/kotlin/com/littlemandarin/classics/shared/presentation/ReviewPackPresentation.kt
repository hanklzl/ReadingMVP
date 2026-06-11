package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.quiz.MissedQuestion
import com.littlemandarin.classics.shared.quiz.ReviewPack
import com.littlemandarin.classics.shared.quiz.ReviewParentTip
import com.littlemandarin.classics.shared.quiz.ReviewWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A persisted snapshot of the most recent [ReviewPack] plus the day it was saved,
 * so the Today screen can surface "finish today's review" the next day without a
 * backend. Fully serializable, content-bounded — no child PII (AGENTS.md §7).
 */
@Serializable
data class StoredReviewPack(
    val storyId: String,
    val titleZh: String,
    val titleEn: String,
    val missedQuestions: List<StoredMissedQuestion> = emptyList(),
    val reviewWords: List<StoredReviewWord> = emptyList(),
    val rereadSentence: String? = null,
    val parentTip: ReviewParentTip,
    val savedEpochDay: Int,
    val completedEpochDay: Int? = null,
)

@Serializable
data class StoredMissedQuestion(
    val questionId: String,
    val prompt: String,
    val correctAnswer: String,
    val explanation: String,
)

@Serializable
data class StoredReviewWord(
    val word: String,
    val pinyin: String,
    val meaning: String,
)

fun ReviewPack.toStored(savedEpochDay: Int): StoredReviewPack = StoredReviewPack(
    storyId = storyId,
    titleZh = titleZh,
    titleEn = titleEn,
    missedQuestions = missedQuestions.map {
        StoredMissedQuestion(it.questionId, it.prompt, it.correctAnswer, it.explanation)
    },
    reviewWords = reviewWords.map { StoredReviewWord(it.word, it.pinyin, it.meaning) },
    rereadSentence = rereadSentence,
    parentTip = parentTip,
    savedEpochDay = savedEpochDay,
    completedEpochDay = null,
)

fun StoredReviewPack.toReviewPack(): ReviewPack = ReviewPack(
    storyId = storyId,
    titleZh = titleZh,
    titleEn = titleEn,
    missedQuestions = missedQuestions.map {
        MissedQuestion(it.questionId, it.prompt, it.correctAnswer, it.explanation)
    },
    reviewWords = reviewWords.map { ReviewWord(it.word, it.pinyin, it.meaning) },
    rereadSentence = rereadSentence,
    parentTip = parentTip,
)

interface ReviewPackService {
    val pack: Flow<StoredReviewPack?>

    suspend fun read(): StoredReviewPack?

    suspend fun save(pack: StoredReviewPack)

    suspend fun markCompleted(completedEpochDay: Int)

    suspend fun clear()
}

class InMemoryReviewPackService(
    initialPack: StoredReviewPack? = null,
) : ReviewPackService {
    private val state = MutableStateFlow(initialPack)

    override val pack: Flow<StoredReviewPack?> = state

    override suspend fun read(): StoredReviewPack? = state.value

    override suspend fun save(pack: StoredReviewPack) {
        state.value = pack
    }

    override suspend fun markCompleted(completedEpochDay: Int) {
        state.value = state.value?.copy(completedEpochDay = completedEpochDay)
    }

    override suspend fun clear() {
        state.value = null
    }
}

expect fun createPlatformReviewPackService(): ReviewPackService

internal interface ReviewPackStore {
    fun readPack(): StoredReviewPack?

    fun writePack(pack: StoredReviewPack)

    fun clearPack()
}

internal class StoredReviewPackService(
    private val store: ReviewPackStore,
) : ReviewPackService {
    private val state = MutableStateFlow(store.readPack())

    override val pack: Flow<StoredReviewPack?> = state

    override suspend fun read(): StoredReviewPack? {
        val latest = store.readPack()
        state.value = latest
        return latest
    }

    override suspend fun save(pack: StoredReviewPack) {
        store.writePack(pack)
        state.value = pack
    }

    override suspend fun markCompleted(completedEpochDay: Int) {
        val current = store.readPack() ?: return
        val updated = current.copy(completedEpochDay = completedEpochDay)
        store.writePack(updated)
        state.value = updated
    }

    override suspend fun clear() {
        store.clearPack()
        state.value = null
    }
}

/**
 * Saves the just-built review pack and decides when to surface it on Today.
 * Surfacing rule (non-blocking, lightweight): show whenever a pack exists that the
 * child hasn't completed yet — primarily the day after it was earned.
 */
class ReviewPackUseCase(
    private val service: ReviewPackService,
) {
    suspend fun savePack(pack: ReviewPack, todayEpochMillis: Long) {
        service.save(pack.toStored(epochDayFromMillis(todayEpochMillis)))
    }

    suspend fun current(): StoredReviewPack? = service.read()

    suspend fun markCompleted(todayEpochMillis: Long) {
        service.markCompleted(epochDayFromMillis(todayEpochMillis))
    }

    /**
     * The pending pack to surface on Today, or null. A pack is pending while it
     * has not been marked completed. It is "fresh today" only on the day it was
     * earned; from the next day on it is highlighted as outstanding review.
     */
    suspend fun pendingReview(todayEpochMillis: Long): PendingReview? {
        val stored = service.read() ?: return null
        if (stored.completedEpochDay != null) return null
        val today = epochDayFromMillis(todayEpochMillis)
        return PendingReview(
            pack = stored.toReviewPack(),
            isFromPreviousDay = today > stored.savedEpochDay,
        )
    }
}

data class PendingReview(
    val pack: ReviewPack,
    val isFromPreviousDay: Boolean,
)

internal object ReviewPackJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(pack: StoredReviewPack): String = json.encodeToString(pack)

    fun decode(encodedPack: String?): StoredReviewPack? {
        if (encodedPack.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<StoredReviewPack>(encodedPack) }
            .getOrNull()
    }
}
