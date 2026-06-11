package com.littlemandarin.classics.shared.recording

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class RecordingLogicTest {
    private val stateMachine = RecordingStateMachine()

    @Test
    fun stateMachineTransitionsThroughRequestRecordPlayLifecycle() {
        val requested = stateMachine.reduce(
            state = RecordingState.Idle,
            action = RecordingAction.Request(storyId = "story-1", paragraphIndex = 2),
        )
        assertIs<RecordingState.Requesting>(requested)
        assertEquals("story-1", requested.storyId)
        assertEquals(2, requested.paragraphIndex)

        val recording = stateMachine.reduce(
            state = requested,
            action = RecordingAction.PermissionGranted,
        )
        assertIs<RecordingState.Recording>(recording)
        assertEquals("story-1", recording.storyId)
        assertEquals(2, recording.paragraphIndex)

        val stopped = stateMachine.reduce(
            state = recording,
            action = RecordingAction.Stopped(recordingId = "recording-1"),
        )
        assertIs<RecordingState.Stopped>(stopped)
        assertEquals("recording-1", stopped.activeRecordingId)

        val playing = stateMachine.reduce(
            state = stopped,
            action = RecordingAction.Playing("recording-1"),
        )
        assertIs<RecordingState.Playing>(playing)
        assertEquals("recording-1", playing.activeRecordingId)

        val backToStopped = stateMachine.reduce(
            state = playing,
            action = RecordingAction.Completed,
        )
        assertIs<RecordingState.Stopped>(backToStopped)
        assertEquals("recording-1", backToStopped.activeRecordingId)
        assertEquals(stopped.storyId, backToStopped.storyId)
        assertEquals(stopped.paragraphIndex, backToStopped.paragraphIndex)
    }

    @Test
    fun stateMachineReturnsToIdleOnPermissionDeniedOrReset() {
        val denied = stateMachine.reduce(
            state = RecordingState.Idle,
            action = RecordingAction.Request(storyId = "story-1", paragraphIndex = 1),
        )
        val result = stateMachine.reduce(
            state = denied,
            action = RecordingAction.PermissionDenied,
        )

        assertEquals(RecordingState.Idle, result)

        val reset = stateMachine.reduce(
            state = RecordingState.Requesting(storyId = "story-1", paragraphIndex = 1),
            action = RecordingAction.Reset,
        )

        assertEquals(RecordingState.Idle, reset)
    }

    @Test
    fun newestFirstSortsByCreationTimeDescendingThenId() {
        val unsortedRecordings = listOf(
            makeRecording(id = "older", createdAtEpochMillis = 1_000L),
            makeRecording(id = "newest", createdAtEpochMillis = 30_000L),
            makeRecording(id = "middle", createdAtEpochMillis = 20_000L),
            makeRecording(id = "same-time", createdAtEpochMillis = 20_000L, storyId = "story-b"),
        )

        assertEquals(
            listOf("newest", "same-time", "middle", "older"),
            unsortedRecordings.newestFirst().map { it.id },
        )
    }

    @Test
    fun retentionCapsPerStoryAndOverallAndReturnsEvicted() {
        val recordings = listOf(
            makeRecording(id = "s1-a1", storyId = "story-a", createdAtEpochMillis = 1_000L),
            makeRecording(id = "s1-a2", storyId = "story-a", createdAtEpochMillis = 2_000L),
            makeRecording(id = "s1-a3", storyId = "story-a", createdAtEpochMillis = 3_000L),
            makeRecording(id = "s1-a4", storyId = "story-a", createdAtEpochMillis = 4_000L),
            makeRecording(id = "s2-b1", storyId = "story-b", createdAtEpochMillis = 5_000L),
            makeRecording(id = "s2-b2", storyId = "story-b", createdAtEpochMillis = 6_000L),
        )
        val result = recordings.applyRetention(
            policy = RecordingRetentionPolicy(
                maxPerStory = 2,
                maxOverall = 3,
            ),
        )

        assertEquals(listOf("s2-b2", "s2-b1", "s1-a4"), result.retained.map { it.id })
        assertEquals(listOf("s1-a2", "s1-a1", "s1-a3"), result.removed.map { it.id })
    }

    @Test
    fun inMemoryServiceDeletesOneRecordingAndEmitsLatestList() = runTest {
        val service = InMemoryVoiceRecordingService(
            initialRecordings = listOf(
                makeRecording(id = "a", createdAtEpochMillis = 1_000L),
                makeRecording(id = "b", createdAtEpochMillis = 2_000L),
                makeRecording(id = "c", createdAtEpochMillis = 3_000L),
            ),
            retentionPolicy = RecordingRetentionPolicy(maxPerStory = 3, maxOverall = 10),
        )

        assertEquals(listOf("c", "b", "a"), service.recordings.first().map { it.id })

        val deleteResult = service.delete("b")

        assertEquals(listOf("c", "a"), service.recordings.first().map { it.id })
        assertEquals(listOf("b"), deleteResult.removed.map { it.id })
        assertEquals(listOf("c", "a"), deleteResult.retained.map { it.id })
        assertNull(service.recordings.first().firstOrNull { it.id == "b" })
    }

    @Test
    fun inMemoryServiceClearAllRemovesEveryRecording() = runTest {
        val service = InMemoryVoiceRecordingService(
            initialRecordings = listOf(
                makeRecording(id = "a", createdAtEpochMillis = 1_000L),
                makeRecording(id = "b", createdAtEpochMillis = 2_000L),
            ),
        )

        val result = service.clearAll()

        assertTrue(service.recordings.first().isEmpty())
        assertTrue(result.retained.isEmpty())
        assertEquals(listOf("b", "a"), result.removed.map { it.id })
    }

    @Test
    fun jsonCodecRoundTripsRecordingsAndSortsNewestFirst() {
        val recordings = listOf(
            makeRecording(id = "second", createdAtEpochMillis = 2_000L),
            makeRecording(id = "first", createdAtEpochMillis = 1_000L),
        )

        val encoded = VoiceRecordingJsonCodec.encode(recordings)
        val decoded = VoiceRecordingJsonCodec.decode(encoded)

        assertEquals(listOf("second", "first"), decoded.map { it.id })
    }

    private fun makeRecording(
        id: String,
        storyId: String = "story-1",
        createdAtEpochMillis: Long,
    ): VoiceRecording = VoiceRecording(
        id = id,
        storyId = storyId,
        paragraphIndex = 0,
        filePath = "/tmp/$id.wav",
        durationMs = 5_000L,
        createdAtEpochMillis = createdAtEpochMillis,
    )
}
