package com.littlemandarin.classics

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.littlemandarin.classics.shared.sfx.SfxCue
import java.io.File
import java.io.IOException
import java.util.Collections
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AndroidSfxPlayer(
    context: Context,
) {
    private val cacheRoot = File(context.applicationContext.cacheDir, "little-mandarin-sfx")
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MaxStreams)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val loadedSoundIds = Collections.synchronizedMap(mutableMapOf<String, Int>())
    private val loadingBySemanticKey = Collections.synchronizedMap(mutableMapOf<String, CompletableDeferred<Int>>())
    private val pendingLoadBySoundId = Collections.synchronizedMap(mutableMapOf<Int, String>())
    private val activeStreamIds = Collections.synchronizedSet(mutableSetOf<Int>())
    @Volatile
    private var stopGeneration: Int = 0

    init {
        cacheRoot.mkdirs()
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            // Take the same lock the loader uses so this callback can never run before the
            // loader has registered pendingLoadBySoundId[sampleId] (otherwise the deferred
            // would hang forever).
            val semanticKey: String?
            val deferred: CompletableDeferred<Int>?
            synchronized(lock) {
                semanticKey = pendingLoadBySoundId.remove(sampleId)
                deferred = semanticKey?.let { loadingBySemanticKey.remove(it) }
                if (semanticKey != null && deferred != null && status == 0) {
                    loadedSoundIds[semanticKey] = sampleId
                }
            }
            if (semanticKey == null || deferred == null) {
                soundPool.unload(sampleId)
                return@setOnLoadCompleteListener
            }
            if (status == 0) {
                deferred.complete(sampleId)
            } else {
                soundPool.unload(sampleId)
                deferred.completeExceptionally(
                    IOException("Failed to load SFX cue '$semanticKey': status=$status"),
                )
            }
        }
    }

    suspend fun play(cue: SfxCue) {
        val sanitizedVolume = sanitizeVolume(cue.volume)
        if (sanitizedVolume <= 0f) return

        val generation = stopGeneration
        val soundId = loadSfx(cue.semanticKey)
        if (generation != stopGeneration) return
        playSound(soundId = soundId, volume = sanitizedVolume)
    }

    suspend fun playPreview(volume: Float, semanticKey: String = PreviewSfxSemanticKey) {
        val sanitizedVolume = sanitizeVolume(volume)
        if (sanitizedVolume <= 0f) return

        val generation = stopGeneration
        val soundId = loadSfx(semanticKey)
        if (generation != stopGeneration) return
        playSound(soundId = soundId, volume = sanitizedVolume)
    }

    fun release() {
        stopAll()
        runCatching { soundPool.release() }
        scope.cancel()
    }

    fun stopAll() {
        stopGeneration += 1
        val streamIds = synchronized(activeStreamIds) {
            activeStreamIds.toList().also { activeStreamIds.clear() }
        }
        streamIds.forEach { streamId ->
            runCatching { soundPool.stop(streamId) }
        }
    }

    private suspend fun loadSfx(semanticKey: String): Int {
        loadedSoundIds[semanticKey]?.let { loadedSoundId ->
            return loadedSoundId
        }

        var shouldLoad = false
        val deferred = synchronized(lock) {
            loadingBySemanticKey[semanticKey]?.let { existing ->
                shouldLoad = false
                return@let existing
            }
            val newLoad = CompletableDeferred<Int>()
            loadingBySemanticKey[semanticKey] = newLoad
            shouldLoad = true
            newLoad
        }

        if (shouldLoad) {
            loadSfxToPool(semanticKey, deferred)
        }

        return deferred.await()
    }

    private fun loadSfxToPool(
        semanticKey: String,
        deferred: CompletableDeferred<Int>,
    ) {
        scope.launch {
            try {
                val cachedFile = resolveCachedSfx(semanticKey)
                // Register the sampleId→key mapping under the same lock as load() so the
                // load-complete callback (which also locks) can't run before it's set.
                val invalidSampleId = synchronized(lock) {
                    val sampleId = soundPool.load(cachedFile.absolutePath, 1)
                    if (sampleId == 0) {
                        loadingBySemanticKey.remove(semanticKey)
                        true
                    } else {
                        pendingLoadBySoundId[sampleId] = semanticKey
                        false
                    }
                }
                if (invalidSampleId) {
                    deferred.completeExceptionally(
                        IOException("SoundPool load returned invalid sampleId for '$semanticKey'"),
                    )
                    return@launch
                }
            } catch (error: Throwable) {
                synchronized(lock) {
                    loadingBySemanticKey.remove(semanticKey)
                }
                deferred.completeExceptionally(error)
            }
        }
    }

    private fun playSound(soundId: Int, volume: Float) {
        val streamId = soundPool.play(
            soundId,
            volume,
            volume,
            maxPriority,
            noLoop,
            normalRate,
        )
        if (streamId == 0) return

        activeStreamIds.add(streamId)
        scope.launch {
            delay(ActiveStreamRetentionMillis)
            activeStreamIds.remove(streamId)
        }
    }

    private fun resolveCachedSfx(semanticKey: String): File {
        val resourcePath = "sfx/$semanticKey.wav"
        val cachedFile = File(cacheRoot, "$semanticKey.wav".safeCacheFileName())
        if (cachedFile.exists()) return cachedFile

        val classLoader = this::class.java.classLoader
            ?: Thread.currentThread().contextClassLoader
            ?: ClassLoader.getSystemClassLoader()
        classLoader.getResourceAsStream(resourcePath)?.use { input ->
            cachedFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("SFX resource not found: $resourcePath")

        return cachedFile
    }

    private fun String.safeCacheFileName(): String =
        replace("/", "_").replace("\\", "_")

    private companion object {
        const val MaxStreams = 4
        const val PreviewSfxSemanticKey = "sound_toggle_preview"
        const val maxPriority = 1
        const val noLoop = 0
        const val normalRate = 1f
        const val ActiveStreamRetentionMillis = 2_000L
    }
}

private fun sanitizeVolume(volume: Float): Float =
    when {
        volume.isNaN() -> 0f
        volume < 0f -> 0f
        volume > 1f -> 1f
        else -> volume
    }
