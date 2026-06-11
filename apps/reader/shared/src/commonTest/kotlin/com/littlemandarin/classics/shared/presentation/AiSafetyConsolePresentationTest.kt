package com.littlemandarin.classics.shared.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiSafetyConsolePresentationTest {
    private val useCases = AiSafetyConsoleUseCases()

    private fun record(
        id: String,
        outcome: AiInteractionOutcome = AiInteractionOutcome.Allowed,
        epochMillis: Long = 1L,
    ) = AiInteractionRecord(
        id = id,
        storyId = "s1",
        questionType = "explain_word",
        query = "词$id",
        answerPreview = "preview $id",
        outcome = outcome,
        epochMillis = epochMillis,
    )

    @Test
    fun recordPrependsNewestAndCapsAtMax() {
        var log = emptyList<AiInteractionRecord>()
        repeat(5) { i -> log = useCases.record(log, record("r$i", epochMillis = i.toLong()), maxRecords = 3) }
        assertEquals(listOf("r4", "r3", "r2"), log.map { it.id }) // newest first, capped to 3
    }

    @Test
    fun recordReplacesSameIdWithoutDuplicating() {
        val log = useCases.record(listOf(record("a"), record("b")), record("a", epochMillis = 99L))
        assertEquals(listOf("a", "b"), log.map { it.id })
        assertEquals(99L, log.first { it.id == "a" }.epochMillis)
    }

    @Test
    fun deleteRemovesById() {
        val log = useCases.delete(listOf(record("a"), record("b")), "a")
        assertEquals(listOf("b"), log.map { it.id })
    }

    @Test
    fun summaryCountsTotalOutOfScopeAndLastTimestamp() {
        val records = listOf(
            record("a", AiInteractionOutcome.Allowed, epochMillis = 10L),
            record("b", AiInteractionOutcome.OutOfScope, epochMillis = 30L),
            record("c", AiInteractionOutcome.OutOfScope, epochMillis = 20L),
        )
        val summary = useCases.summary(records)
        assertEquals(3, summary.totalCount)
        assertEquals(2, summary.outOfScopeCount)
        assertEquals(30L, summary.lastEpochMillis)
    }

    @Test
    fun summaryOfEmptyLogHasNullTimestamp() {
        val summary = useCases.summary(emptyList())
        assertEquals(0, summary.totalCount)
        assertEquals(null, summary.lastEpochMillis)
    }

    @Test
    fun codecRoundTripAndMalformed() {
        val records = listOf(
            record("a", AiInteractionOutcome.Allowed, 1L),
            record("b", AiInteractionOutcome.OutOfScope, 2L),
        )
        val encoded = AiInteractionLogJsonCodec.encode(records)
        assertEquals(records, AiInteractionLogJsonCodec.decode(encoded))
        assertTrue(AiInteractionLogJsonCodec.decode(null).isEmpty())
        assertTrue(AiInteractionLogJsonCodec.decode("not json").isEmpty())
    }
}
