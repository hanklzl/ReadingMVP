package com.littlemandarin.classics.shared.resource

interface StoryResourceReader {
    suspend fun readText(path: String): String
}

expect fun defaultStoryResourceReader(): StoryResourceReader
