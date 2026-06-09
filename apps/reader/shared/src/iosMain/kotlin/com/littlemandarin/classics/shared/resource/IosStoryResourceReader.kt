package com.littlemandarin.classics.shared.resource

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

actual fun defaultStoryResourceReader(): StoryResourceReader = IosStoryResourceReader()

@OptIn(ExperimentalForeignApi::class)
class IosStoryResourceReader : StoryResourceReader {
    override suspend fun readText(path: String): String {
        val directory = path.substringBeforeLast("/", missingDelimiterValue = "")
        val fileName = path.substringAfterLast("/")
        val resourceName = fileName.substringBeforeLast(".")
        val resourceExtension = fileName.substringAfterLast(".", missingDelimiterValue = "")
        val resourcePath = NSBundle.mainBundle.pathForResource(
            name = resourceName,
            ofType = resourceExtension,
            inDirectory = directory.ifBlank { null },
        ) ?: error("Story resource not found: $path")

        return NSString.stringWithContentsOfFile(
            path = resourcePath,
            encoding = NSUTF8StringEncoding,
            error = null,
        ) ?: error("Story resource could not be read: $path")
    }
}
