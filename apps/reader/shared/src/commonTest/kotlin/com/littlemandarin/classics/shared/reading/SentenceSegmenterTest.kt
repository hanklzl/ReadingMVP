package com.littlemandarin.classics.shared.reading

import kotlin.test.Test
import kotlin.test.assertEquals

class SentenceSegmenterTest {
    @Test
    fun segmentsChineseSentencePunctuationAndKeepsOffsets() {
        val segments = SentenceSegmenter.segment("桃园开花。三人说：好！大家点头……")

        assertEquals(
            listOf("桃园开花。", "三人说：好！", "大家点头……"),
            segments.map { it.text },
        )
        assertEquals(listOf(0, 5, 11), segments.map { it.startOffset })
        assertEquals(listOf(5, 11, 17), segments.map { it.endOffset })
    }

    @Test
    fun keepsConsecutiveEndingMarksWithPreviousSentenceAndSkipsWhitespaceOnlyText() {
        val segments = SentenceSegmenter.segment("  好！？\n\n再说；！！  \t")

        assertEquals(
            listOf("好！？", "再说；！！"),
            segments.map { it.text },
        )
        assertEquals(listOf(2, 7), segments.map { it.startOffset })
        assertEquals(listOf(5, 12), segments.map { it.endOffset })
        assertEquals(emptyList(), SentenceSegmenter.segment(" \n\t "))
    }

    @Test
    fun keepsClosingQuotesAndParenthesesWithSentenceEnding() {
        val segments = SentenceSegmenter.segment("他说：“好！”大家点头。再看（可以吗？）")

        assertEquals(
            listOf("他说：“好！”", "大家点头。", "再看（可以吗？）"),
            segments.map { it.text },
        )
    }
}
