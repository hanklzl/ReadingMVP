package com.littlemandarin.classics.shared.story

import kotlinx.serialization.json.Json

object StoryJson {
    private val format = Json {
        ignoreUnknownKeys = false
        encodeDefaults = false
    }

    fun decodeStory(json: String): Story = format.decodeFromString(
        deserializer = Story.serializer(),
        string = json,
    )

    fun encodeStory(story: Story): String = format.encodeToString(
        serializer = Story.serializer(),
        value = story,
    )
}
