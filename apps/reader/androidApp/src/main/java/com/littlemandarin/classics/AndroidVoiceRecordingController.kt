package com.littlemandarin.classics

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.File

internal class AndroidVoiceRecordingController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val recordingsDir = File(appContext.filesDir, RecordingDirectory).also { it.mkdirs() }

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var activeFile: File? = null
    private var startedAtEpochMillis: Long = 0L

    val isRecording: Boolean
        get() = recorder != null

    @Suppress("DEPRECATION")
    fun startRecording(
        storyId: String,
        paragraphIndex: Int,
    ) {
        stopPlayback()
        cancelRecording()

        val file = nextFile(storyId = storyId, paragraphIndex = paragraphIndex)
        val mediaRecorder = MediaRecorder()
        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            activeFile = file
            startedAtEpochMillis = System.currentTimeMillis()
        } catch (error: Throwable) {
            runCatching { mediaRecorder.release() }
            file.delete()
            recorder = null
            activeFile = null
            startedAtEpochMillis = 0L
            throw error
        }
    }

    fun stopRecording(): CaptureResult? {
        val mediaRecorder = recorder ?: return null
        val file = activeFile ?: return null
        val startMillis = startedAtEpochMillis

        recorder = null
        activeFile = null
        startedAtEpochMillis = 0L

        try {
            mediaRecorder.stop()
        } catch (_: Throwable) {
            file.delete()
            runCatching { mediaRecorder.release() }
            return null
        }
        runCatching { mediaRecorder.release() }

        if (!file.exists() || file.length() <= 0L) {
            file.delete()
            return null
        }

        return CaptureResult(
            file = file,
            durationMs = (System.currentTimeMillis() - startMillis).coerceAtLeast(0L),
        )
    }

    fun cancelRecording() {
        val mediaRecorder = recorder
        val file = activeFile
        recorder = null
        activeFile = null
        startedAtEpochMillis = 0L
        runCatching { mediaRecorder?.stop() }
        runCatching { mediaRecorder?.release() }
        file?.delete()
    }

    fun play(
        filePath: String,
        onCompletion: () -> Unit,
        onError: () -> Unit,
    ) {
        cancelRecording()
        stopPlayback()
        val file = File(filePath)
        if (!file.exists()) {
            onError()
            return
        }

        try {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopPlayback()
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    onError()
                    true
                }
                prepare()
            }
            player = mediaPlayer
            mediaPlayer.start()
        } catch (_: Throwable) {
            stopPlayback()
            onError()
        }
    }

    fun stopPlayback() {
        val mediaPlayer = player
        player = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }

    private fun nextFile(
        storyId: String,
        paragraphIndex: Int,
    ): File {
        val safeStoryId = storyId
            .map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }
            .joinToString(separator = "")
            .ifBlank { "story" }
        val now = System.currentTimeMillis()
        return File(recordingsDir, "${safeStoryId}_p${paragraphIndex + 1}_$now.m4a")
    }

    data class CaptureResult(
        val file: File,
        val durationMs: Long,
    )

    private companion object {
        const val RecordingDirectory: String = "voice_recordings"
    }
}
