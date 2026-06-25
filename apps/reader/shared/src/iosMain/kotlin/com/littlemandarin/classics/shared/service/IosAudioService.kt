package com.littlemandarin.classics.shared.service

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

actual fun createAudioService(): AudioService = IosAudioService()

@OptIn(ExperimentalForeignApi::class)
private class IosAudioService : AudioService {
    private var player: AVAudioPlayer? = null

    override suspend fun play(resourcePath: String, speedMultiplier: Float) {
        if (resourcePath.isBlank()) return

        playResolvedResource(
            resourcePath = resourcePath,
            speedMultiplier = speedMultiplier,
            startMillis = 0L,
        )
    }

    override suspend fun playRange(
        resourcePath: String,
        startMillis: Long,
        endMillis: Long,
        speedMultiplier: Float,
    ) {
        if (resourcePath.isBlank() || endMillis <= startMillis) return

        playResolvedResource(
            resourcePath = resourcePath,
            speedMultiplier = speedMultiplier,
            startMillis = startMillis,
        )
    }

    private suspend fun playResolvedResource(
        resourcePath: String,
        speedMultiplier: Float,
        startMillis: Long,
    ) {
        stop()

        val resolvedPath = resolvePath(resourcePath)
        val url = NSURL.fileURLWithPath(resolvedPath)
        val audioPlayer = AVAudioPlayer(contentsOfURL = url, error = null)
        audioPlayer.enableRate = true
        audioPlayer.rate = speedMultiplier.coerceIn(MinPlaybackSpeed, MaxPlaybackSpeed)
        audioPlayer.currentTime = startMillis.coerceAtLeast(0L).toDouble() / 1000.0
        audioPlayer.prepareToPlay()
        audioPlayer.play()
        player = audioPlayer
    }

    override suspend fun hasSentenceAudio(
        storyId: String,
        paragraphIndex: Int,
        sentenceIndex: Int,
    ): Boolean = resolveBundledPath(
        sentenceAudioResourcePath(storyId, paragraphIndex, sentenceIndex),
    ) != null

    override suspend fun playSentence(
        storyId: String,
        paragraphIndex: Int,
        sentenceIndex: Int,
        speedMultiplier: Float,
    ) {
        play(sentenceAudioResourcePath(storyId, paragraphIndex, sentenceIndex), speedMultiplier)
    }

    override suspend fun pause() {
        player?.pause()
    }

    override suspend fun stop() {
        player?.stop()
        player = null
    }

    private fun resolvePath(resourcePath: String): String {
        if (resourcePath.startsWith("/")) return resourcePath

        return resolveBundledPath(resourcePath) ?: resourcePath
    }

    private fun resolveBundledPath(resourcePath: String): String? {
        val directory = resourcePath.substringBeforeLast("/", missingDelimiterValue = "")
        val fileName = resourcePath.substringAfterLast("/")
        val resourceName = fileName.substringBeforeLast(".")
        val resourceExtension = fileName.substringAfterLast(".", missingDelimiterValue = "")

        return NSBundle.mainBundle.pathForResource(
            name = resourceName,
            ofType = resourceExtension,
            inDirectory = directory.ifBlank { null },
        )
    }

    private companion object {
        const val MinPlaybackSpeed: Float = 0.5f
        const val MaxPlaybackSpeed: Float = 1.5f
    }
}
