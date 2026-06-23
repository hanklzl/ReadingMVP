package com.littlemandarin.classics

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadingKaraokeLayoutPolicyTest {

    @Test
    fun activeKaraokeTextUsesTheSameFontWeightAsInactiveText() {
        val mediumStyle = TextStyle(fontWeight = FontWeight.Medium)

        assertEquals(FontWeight.Medium, karaokeTextFontWeight(mediumStyle, isActiveChar = false))
        assertEquals(FontWeight.Medium, karaokeTextFontWeight(mediumStyle, isActiveChar = true))

        val plainStyle = TextStyle()

        assertNull(karaokeTextFontWeight(plainStyle, isActiveChar = false))
        assertNull(karaokeTextFontWeight(plainStyle, isActiveChar = true))
    }

    @Test
    fun activeKaraokeCellReservesTheSameHorizontalPaddingAsInactiveCells() {
        assertEquals(
            karaokeCellHorizontalPadding(isActiveChar = false),
            karaokeCellHorizontalPadding(isActiveChar = true),
        )
    }

    @Test
    fun sentenceSpeakerButtonKeepsAccessibleTouchTargetWithCompactVisualSize() {
        assertEquals(48.dp, sentenceSpeakerTouchTargetSize())
        assertTrue(sentenceSpeakerVisualSize() < sentenceSpeakerTouchTargetSize())
    }
}
