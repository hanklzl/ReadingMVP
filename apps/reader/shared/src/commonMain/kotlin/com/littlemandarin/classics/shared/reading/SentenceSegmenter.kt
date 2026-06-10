package com.littlemandarin.classics.shared.reading

data class SentenceSegment(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

object SentenceSegmenter {
    private val SentenceEndingMarks = setOf('。', '！', '？', '；', '…')
    private val SentenceTrailingMarks = setOf('”', '’', '」', '』', '》', '〉', '）', ')')

    fun segment(text: String): List<SentenceSegment> {
        if (text.isBlank()) return emptyList()

        val segments = mutableListOf<SentenceSegment>()
        var segmentStart: Int? = null
        var index = 0

        while (index < text.length) {
            val current = text[index]
            if (segmentStart == null) {
                if (current.isWhitespace() || isSentenceEndingMark(current)) {
                    index += 1
                    continue
                }
                segmentStart = index
            }

            if (isSentenceEndingMark(current)) {
                val endOffset = consumeSentenceEnd(text, index + 1)
                addSegment(
                    target = segments,
                    source = text,
                    startOffset = segmentStart,
                    endOffset = endOffset,
                )
                segmentStart = null
                index = endOffset
            } else {
                index += 1
            }
        }

        val trailingStart = segmentStart
        if (trailingStart != null) {
            addSegment(
                target = segments,
                source = text,
                startOffset = trailingStart,
                endOffset = text.length,
            )
        }

        return segments
    }

    private fun consumeSentenceEnd(
        text: String,
        startOffset: Int,
    ): Int {
        var endOffset = startOffset
        while (
            endOffset < text.length &&
            (isSentenceEndingMark(text[endOffset]) || text[endOffset] in SentenceTrailingMarks)
        ) {
            endOffset += 1
        }
        return endOffset
    }

    private fun addSegment(
        target: MutableList<SentenceSegment>,
        source: String,
        startOffset: Int,
        endOffset: Int,
    ) {
        val trimmedEndOffset = source.trimTrailingWhitespace(startOffset, endOffset)
        if (trimmedEndOffset <= startOffset) return

        target += SentenceSegment(
            text = source.substring(startOffset, trimmedEndOffset),
            startOffset = startOffset,
            endOffset = trimmedEndOffset,
        )
    }

    private fun String.trimTrailingWhitespace(
        startOffset: Int,
        endOffset: Int,
    ): Int {
        var index = endOffset
        while (index > startOffset && this[index - 1].isWhitespace()) {
            index -= 1
        }
        return index
    }

    private fun isSentenceEndingMark(character: Char): Boolean =
        character in SentenceEndingMarks
}
