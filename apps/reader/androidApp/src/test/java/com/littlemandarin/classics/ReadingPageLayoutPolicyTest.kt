package com.littlemandarin.classics

import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.Story
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingPageLayoutPolicyTest {

    @Test
    fun readingContentItemsIncludeEveryParagraphSentenceInStoryOrder() {
        val items = readingContentItems(
            storyWithParagraphs(
                "第一句。第二句。",
                "第三句。",
                "第四句。第五句。",
            ),
        ).filterIsInstance<ReadingContentItem.Sentence>()

        assertEquals(listOf(0, 0, 1, 2, 2), items.map { it.paragraphIndex })
        assertEquals(listOf(0, 1, 0, 0, 1), items.map { it.sentenceIndex })
        assertEquals(listOf("第一句。", "第二句。", "第三句。", "第四句。", "第五句。"), items.map { it.sentence.text })
    }

    @Test
    fun readingContentItemIndexMapsSentencePositionIntoWholeStoryList() {
        val items = readingContentItems(
            storyWithParagraphs(
                "第一句。第二句。",
                "第三句。",
                "第四句。第五句。",
            ),
        )

        assertEquals(0, readingContentItemIndexFor(items, paragraphIndex = 0, sentenceIndex = 0))
        assertEquals(3, readingContentItemIndexFor(items, paragraphIndex = 2, sentenceIndex = 0))
        assertEquals(4, readingContentItemIndexFor(items, paragraphIndex = 2, sentenceIndex = 1))
    }

    @Test
    fun readingContentItemsKeepFallbackParagraphBodiesInScrollOrder() {
        val items = readingContentItems(
            storyWithParagraphs(
                "   ",
                "第一句。",
            ),
        )

        assertTrue(items.first() is ReadingContentItem.ParagraphBody)
        assertEquals(0, items.first().paragraphIndex)
        assertEquals(0, readingContentItemIndexFor(items, paragraphIndex = 0, sentenceIndex = 0))
    }

    @Test
    fun visibleScrollItemMapsBackToReadingPosition() {
        val items = readingContentItems(
            storyWithParagraphs(
                "第一句。第二句。",
                "第三句。",
                "第四句。第五句。",
            ),
        )

        assertEquals(ReadingContentPosition(paragraphIndex = 0, sentenceIndex = 0), readingContentPositionForItemIndex(items, 0))
        assertEquals(ReadingContentPosition(paragraphIndex = 2, sentenceIndex = 1), readingContentPositionForItemIndex(items, 4))
        assertEquals(ReadingContentPosition(paragraphIndex = 2, sentenceIndex = 1), readingContentPositionForItemIndex(items, 99))
    }

    @Test
    fun stoppedPlaybackUsesVisiblePositionAfterManualScroll() {
        val visiblePosition = ReadingContentPosition(paragraphIndex = 2, sentenceIndex = 1)
        val playbackPosition = ReadingContentPosition(paragraphIndex = 0, sentenceIndex = 0)

        assertEquals(
            visiblePosition,
            stoppedPlaybackReadingPosition(
                autoFollowEnabled = false,
                visiblePosition = visiblePosition,
                playbackPosition = playbackPosition,
            ),
        )
        assertEquals(
            playbackPosition,
            stoppedPlaybackReadingPosition(
                autoFollowEnabled = true,
                visiblePosition = visiblePosition,
                playbackPosition = playbackPosition,
            ),
        )
    }

    @Test
    fun readingPageRemovesParagraphPagingAndBottomSupplementCards() {
        val policy = readingPageLayoutPolicy()

        assertFalse(policy.showVoicePracticeCard)
        assertFalse(policy.showParagraphExplanationCard)
    }
}

private fun storyWithParagraphs(vararg paragraphs: String): Story =
    Story(
        id = "sample",
        titleZh = "测试故事",
        titleEn = "Sample",
        level = 1,
        ageRange = "5-8",
        sourceNote = "test",
        paragraphs = paragraphs.map { text -> Paragraph(text = text, pinyin = "") },
        vocab = emptyList(),
        questions = emptyList(),
        retellPrompt = "",
    )
