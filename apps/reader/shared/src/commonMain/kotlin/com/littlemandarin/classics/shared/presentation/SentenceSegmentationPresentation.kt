package com.littlemandarin.classics.shared.presentation

typealias SentenceSegment = com.littlemandarin.classics.shared.reading.SentenceSegment

object SentenceSegmenter {
    fun segment(text: String): List<SentenceSegment> =
        com.littlemandarin.classics.shared.reading.SentenceSegmenter.segment(text)
}
