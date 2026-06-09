package com.littlemandarin.classics.shared.story

import com.littlemandarin.classics.shared.resource.StoryResourceReader
import com.littlemandarin.classics.shared.resource.defaultStoryResourceReader

interface StoryRepository {
    suspend fun listStories(): List<Story>

    suspend fun getStory(id: String): Story?
}

class DefaultStoryRepository(
    private val resourceReader: StoryResourceReader = defaultStoryResourceReader(),
    private val catalog: List<StoryResourceEntry> = StoryResourceCatalog.entries,
) : StoryRepository {
    override suspend fun listStories(): List<Story> = catalog.map { entry ->
        readStory(entry)
    }

    override suspend fun getStory(id: String): Story? {
        val entry = catalog.firstOrNull { it.id == id } ?: return null
        return readStory(entry)
    }

    private suspend fun readStory(entry: StoryResourceEntry): Story {
        val json = resourceReader.readText(entry.path)
        return StoryJson.decodeStory(json)
    }
}
