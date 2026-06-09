package com.littlemandarin.classics.shared.resource

import java.nio.charset.StandardCharsets

actual fun defaultStoryResourceReader(): StoryResourceReader = AndroidStoryResourceReader()

class AndroidStoryResourceReader(
    private val classLoader: ClassLoader = defaultClassLoader(),
) : StoryResourceReader {
    override suspend fun readText(path: String): String {
        return classLoader.getResourceAsStream(path)
            ?.use { input -> input.readBytes().toString(StandardCharsets.UTF_8) }
            ?: error("Story resource not found: $path")
    }
}

private fun defaultClassLoader(): ClassLoader =
    StoryResourceReader::class.java.classLoader
        ?: Thread.currentThread().contextClassLoader
        ?: ClassLoader.getSystemClassLoader()
