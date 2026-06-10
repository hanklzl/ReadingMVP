package com.littlemandarin.classics.shared.story

import com.littlemandarin.classics.shared.resource.StoryResourceReader
import com.littlemandarin.classics.shared.resource.defaultStoryResourceReader
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StoryAudioManifest(
    val storyId: String,
    val segments: List<StoryAudioSegment> = emptyList(),
) {
    fun segmentFor(
        paragraphIndex: Int,
        sentenceIndex: Int,
    ): StoryAudioSegment? = segments.firstOrNull { segment ->
        segment.paragraphIndex == paragraphIndex && segment.sentenceIndex == sentenceIndex
    }

    companion object {
        fun empty(storyId: String): StoryAudioManifest = StoryAudioManifest(
            storyId = storyId,
            segments = emptyList(),
        )
    }
}

@Serializable
data class StoryAudioSegment(
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val text: String,
    val resourcePath: String,
    val durationMillis: Long? = null,
    val unavailable: Boolean = false,
    /**
     * Per-character karaoke timings, 1:1 with the Unicode characters of [text]
     * (non-hanzi characters keep placeholder slots). Empty when the manifest was
     * generated before word-level alignment was added.
     */
    val chars: List<StoryAudioCharTiming> = emptyList(),
)

/** One character's start/end offset (ms, relative to the sentence clip start). */
@Serializable
data class StoryAudioCharTiming(
    val c: String,
    val startMillis: Long,
    val endMillis: Long,
)

object StoryAudioJson {
    private val format = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun decodeManifest(
        json: String,
        storyId: String = "",
    ): StoryAudioManifest {
        val trimmedJson = json.trimStart()
        val payload = if (trimmedJson.startsWith("[")) {
            StoryAudioManifestPayload(
                sentences = format.decodeFromString(
                    deserializer = ListSerializer(StoryAudioSentencePayload.serializer()),
                    string = json,
                ),
            )
        } else {
            format.decodeFromString(
                deserializer = StoryAudioManifestPayload.serializer(),
                string = json,
            )
        }
        val resolvedStoryId = storyId.ifBlank { payload.storyId.orEmpty() }
        return StoryAudioManifest(
            storyId = resolvedStoryId,
            segments = payload.sentences.mapNotNull { sentence ->
                sentence.toDomainSegment(storyId = resolvedStoryId)
            },
        )
    }

    fun encodeManifest(manifest: StoryAudioManifest): String =
        format.encodeToString(
            serializer = StoryAudioManifestPayload.serializer(),
            value = StoryAudioManifestPayload(
                storyId = manifest.storyId,
                sentences = manifest.segments.map { segment ->
                    StoryAudioSentencePayload(
                        paraIndex = segment.paragraphIndex,
                        sentIndex = segment.sentenceIndex,
                        text = segment.text,
                        audioPath = segment.resourcePath,
                        durationMs = segment.durationMillis,
                        unavailable = segment.unavailable,
                        chars = segment.chars.map { timing ->
                            StoryAudioCharPayload(
                                c = timing.c,
                                startMs = timing.startMillis,
                                endMs = timing.endMillis,
                            )
                        },
                    )
                },
            ),
        )
}

@Serializable
private data class StoryAudioManifestPayload(
    @SerialName("story_id")
    val storyId: String? = null,
    val sentences: List<StoryAudioSentencePayload> = emptyList(),
)

@Serializable
private data class StoryAudioSentencePayload(
    val paraIndex: Int,
    val sentIndex: Int,
    val text: String,
    val audioPath: String? = null,
    val durationMs: Long? = null,
    val unavailable: Boolean = false,
    val chars: List<StoryAudioCharPayload> = emptyList(),
) {
    fun toDomainSegment(storyId: String): StoryAudioSegment? {
        if (unavailable) return null
        val path = audioPath?.takeIf { it.isNotBlank() } ?: return null
        return StoryAudioSegment(
            paragraphIndex = paraIndex,
            sentenceIndex = sentIndex,
            text = text,
            resourcePath = path.toAppResourcePath(storyId),
            durationMillis = durationMs,
            unavailable = false,
            chars = chars.map { timing ->
                StoryAudioCharTiming(
                    c = timing.c,
                    startMillis = timing.startMs,
                    endMillis = timing.endMs,
                )
            },
        )
    }

    private fun String.toAppResourcePath(storyId: String): String =
        if (startsWith("stories/") || storyId.isBlank()) {
            this
        } else {
            "stories/$storyId/$this"
        }
}

@Serializable
private data class StoryAudioCharPayload(
    val c: String,
    val startMs: Long,
    val endMs: Long,
)

class LoadStoryAudioManifestUseCase(
    private val resourceReader: StoryResourceReader = defaultStoryResourceReader(),
) {
    suspend operator fun invoke(storyId: String): StoryAudioManifest {
        val normalizedStoryId = storyId.trim()
        if (normalizedStoryId.isEmpty()) return StoryAudioManifest.empty(storyId)

        val json = try {
            resourceReader.readText(audioManifestPath(normalizedStoryId))
        } catch (_: IllegalStateException) {
            return StoryAudioManifest.empty(normalizedStoryId)
        }

        return StoryAudioJson.decodeManifest(
            json = json,
            storyId = normalizedStoryId,
        )
    }

    private fun audioManifestPath(storyId: String): String =
        "stories/$storyId/audio.json"
}
