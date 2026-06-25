package com.littlemandarin.classics.shared.service

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.littlemandarin.classics.shared.story.StoryAudioSegment
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object AndroidAudioServiceProvider {
    private var applicationContext: Context? = null
    private var service: AudioService? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun create(): AudioService {
        service?.let { return it }

        return AndroidAudioService(applicationContext).also { service = it }
    }
}

actual fun createAudioService(): AudioService = AndroidAudioServiceProvider.create()

private class AndroidAudioService(
    context: Context?,
) : AudioService {
    private val cacheRoot = context?.applicationContext?.cacheDir
        ?: File(System.getProperty("java.io.tmpdir") ?: ".")
    private val lock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private var activeRangeStartMillis: Long = 0L

    override suspend fun play(resourcePath: String, speedMultiplier: Float) {
        if (resourcePath.isBlank()) return

        val audioFile = resolveAudioFile(resourcePath)
        playFile(
            audioFile = audioFile,
            speedMultiplier = speedMultiplier,
            startMillis = 0L,
            endMillis = null,
        )
    }

    override suspend fun playRange(
        resourcePath: String,
        startMillis: Long,
        endMillis: Long,
        speedMultiplier: Float,
    ) {
        if (resourcePath.isBlank() || endMillis <= startMillis) return

        val audioFile = resolveAudioFile(resourcePath)
        playFile(
            audioFile = audioFile,
            speedMultiplier = speedMultiplier,
            startMillis = startMillis,
            endMillis = endMillis,
        )
    }

    private suspend fun playFile(
        audioFile: File,
        speedMultiplier: Float,
        startMillis: Long,
        endMillis: Long?,
    ) {
        val safeSpeed = speedMultiplier.coerceIn(MinPlaybackSpeed, MaxPlaybackSpeed)
        suspendCancellableCoroutine { continuation ->
            val player = MediaPlayer()
            val handler = Handler(Looper.getMainLooper())
            var endWatcher: Runnable? = null
            var settled = false

            fun settle(error: Throwable? = null) {
                endWatcher?.let { handler.removeCallbacks(it) }
                synchronized(lock) {
                    if (settled) return
                    settled = true
                    if (mediaPlayer === player) {
                        mediaPlayer = null
                        activeRangeStartMillis = 0L
                    }
                }
                runCatching {
                    player.release()
                }
                if (continuation.isActive) {
                    if (error == null) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(error)
                    }
                }
            }

            continuation.invokeOnCancellation {
                settle()
            }

            try {
                player.setOnCompletionListener {
                    settle()
                }
                player.setOnErrorListener { _, what, extra ->
                    settle(IllegalStateException("Audio playback failed: what=$what extra=$extra"))
                    true
                }
                player.setDataSource(audioFile.absolutePath)
                player.prepare()
                val fileDurationMillis = player.duration.toLong().coerceAtLeast(0L)
                val safeStartMillis = startMillis.coerceAtLeast(0L).coerceAtMost(fileDurationMillis)
                val safeEndMillis = endMillis
                    ?.takeIf { it > safeStartMillis }
                    ?.coerceAtMost(fileDurationMillis)
                player.playbackParams = player.playbackParams.setSpeed(safeSpeed)
                if (safeStartMillis > 0L) {
                    player.seekTo(safeStartMillis.toInt())
                }
                synchronized(lock) {
                    releasePlayerLocked()
                    mediaPlayer = player
                    activeRangeStartMillis = safeStartMillis
                }
                player.start()
                if (safeEndMillis != null) {
                    endWatcher = object : Runnable {
                        override fun run() {
                            val reachedEnd = runCatching {
                                player.currentPosition.toLong() >= safeEndMillis
                            }.getOrDefault(true)
                            if (reachedEnd) {
                                settle()
                            } else {
                                handler.postDelayed(this, RangeWatchIntervalMillis)
                            }
                        }
                    }
                    handler.post(endWatcher)
                }
            } catch (error: Throwable) {
                settle(error)
            }
        }
    }

    override suspend fun hasAudio(segment: StoryAudioSegment?): Boolean =
        segment != null && runCatching {
            resolveAudioFile(segment.resourcePath)
        }.isSuccess

    override suspend fun hasSentenceAudio(
        storyId: String,
        paragraphIndex: Int,
        sentenceIndex: Int,
    ): Boolean = runCatching {
        resolveAudioFile(sentenceAudioResourcePath(storyId, paragraphIndex, sentenceIndex))
    }.isSuccess

    override suspend fun playSentence(
        storyId: String,
        paragraphIndex: Int,
        sentenceIndex: Int,
        speedMultiplier: Float,
    ) {
        play(sentenceAudioResourcePath(storyId, paragraphIndex, sentenceIndex), speedMultiplier)
    }

    override suspend fun pause() {
        synchronized(lock) {
            runCatching {
                mediaPlayer?.takeIf { it.isPlaying }?.pause()
            }
        }
    }

    override suspend fun stop() {
        synchronized(lock) {
            releasePlayerLocked()
        }
    }

    override fun currentPositionMillis(): Long? = synchronized(lock) {
        runCatching {
            mediaPlayer
                ?.takeIf { it.isPlaying }
                ?.currentPosition
                ?.toLong()
                ?.minus(activeRangeStartMillis)
                ?.coerceAtLeast(0L)
        }.getOrNull()
    }

    private fun resolveAudioFile(resourcePath: String): File {
        val directFile = File(resourcePath)
        if (directFile.exists()) return directFile

        val cachedFile = File(
            File(cacheRoot, AudioCacheDirectoryName),
            resourcePath.safeCacheFileName(),
        )
        if (cachedFile.exists()) return cachedFile

        cachedFile.parentFile?.mkdirs()
        val classLoader = AudioService::class.java.classLoader
            ?: Thread.currentThread().contextClassLoader
            ?: ClassLoader.getSystemClassLoader()
        classLoader.getResourceAsStream(resourcePath)?.use { input ->
            cachedFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Audio resource not found: $resourcePath")

        return cachedFile
    }

    private fun releasePlayerLocked() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            player.release()
        }
        mediaPlayer = null
        activeRangeStartMillis = 0L
    }

    private fun String.safeCacheFileName(): String =
        map { character ->
            if (character.isLetterOrDigit() || character == '.' || character == '-' || character == '_') {
                character
            } else {
                '_'
            }
        }.joinToString(separator = "")

    private companion object {
        const val AudioCacheDirectoryName: String = "little-mandarin-audio"
        const val MinPlaybackSpeed: Float = 0.5f
        const val MaxPlaybackSpeed: Float = 1.5f
        const val RangeWatchIntervalMillis: Long = 20L
    }
}
