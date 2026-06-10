package com.littlemandarin.classics.shared.story

import com.littlemandarin.classics.shared.resource.StoryResourceReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class StoryAudioManifestTest {
    @Test
    fun decodesAudioManifestSidecarWithoutChangingStorySchema() {
        val manifest = StoryAudioJson.decodeManifest(
            """
            {
              "sentences": [
                {
                  "paraIndex": 0,
                  "sentIndex": 1,
                  "text": "三人说：好！",
                  "audioPath": "audio/p1_s2.wav",
                  "durationMs": 2100
                },
                {
                  "paraIndex": 0,
                  "sentIndex": 2,
                  "text": "备用句。",
                  "audioPath": "audio/p1_s3.wav",
                  "unavailable": true
                }
              ]
            }
            """.trimIndent(),
            storyId = "sample",
        )

        val segment = manifest.segmentFor(paragraphIndex = 0, sentenceIndex = 1)

        assertEquals("sample", manifest.storyId)
        assertEquals(1, manifest.segments.size)
        assertEquals("stories/sample/audio/p1_s2.wav", segment?.resourcePath)
        assertEquals("三人说：好！", segment?.text)
        assertEquals(2100L, segment?.durationMillis)
        assertNull(manifest.segmentFor(paragraphIndex = 0, sentenceIndex = 2))
    }

    @Test
    fun decodesPerCharacterKaraokeTimings() {
        val manifest = StoryAudioJson.decodeManifest(
            """
            {
              "sentences": [
                {
                  "paraIndex": 0,
                  "sentIndex": 0,
                  "text": "好，吗",
                  "audioPath": "audio/p1_s1.wav",
                  "durationMs": 1000,
                  "chars": [
                    { "c": "好", "startMs": 0, "endMs": 400 },
                    { "c": "，", "startMs": 400, "endMs": 600 },
                    { "c": "吗", "startMs": 600, "endMs": 1000 }
                  ]
                }
              ]
            }
            """.trimIndent(),
            storyId = "sample",
        )

        val segment = manifest.segmentFor(paragraphIndex = 0, sentenceIndex = 0)
        assertEquals(3, segment?.chars?.size)
        assertEquals("好", segment?.chars?.first()?.c)
        assertEquals(0L, segment?.chars?.first()?.startMillis)
        assertEquals(1000L, segment?.chars?.last()?.endMillis)
    }

    @Test
    fun loadAudioManifestReturnsEmptyWhenSidecarIsMissing() = runTest {
        val reader = MapStoryResourceReader(
            resources = mapOf(
                "stories/sample/audio.json" to
                    """
                    {
                      "sentences": [
                        {
                          "paraIndex": 0,
                          "sentIndex": 0,
                          "text": "第一句。",
                          "audioPath": "audio/p1_s1.wav"
                        }
                      ]
                    }
                    """.trimIndent(),
            ),
        )
        val useCase = LoadStoryAudioManifestUseCase(reader)

        val present = useCase("sample")
        val missing = useCase("missing")

        assertEquals("sample", present.storyId)
        assertEquals("stories/sample/audio/p1_s1.wav", present.segmentFor(0, 0)?.resourcePath)
        assertEquals("missing", missing.storyId)
        assertEquals(emptyList(), missing.segments)
    }

    @Test
    fun decodesLegacyTopLevelSentenceList() {
        val manifest = StoryAudioJson.decodeManifest(
            """
            [
              {
                "paraIndex": 0,
                "sentIndex": 0,
                "text": "第一句。",
                "audioPath": "audio/p1_s1.wav"
              }
            ]
            """.trimIndent(),
            storyId = "sample",
        )

        assertEquals("stories/sample/audio/p1_s1.wav", manifest.segmentFor(0, 0)?.resourcePath)
    }
}

private class MapStoryResourceReader(
    private val resources: Map<String, String>,
) : StoryResourceReader {
    override suspend fun readText(path: String): String =
        resources[path] ?: error("Story resource not found: $path")
}
