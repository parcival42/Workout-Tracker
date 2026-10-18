package io.github.parcival42.workouttracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationFormatTest {

    @Test
    fun formatShowsWholeMinutesPlainAndEverythingElseExactly() {
        assertEquals("20", formatDuration(1_200))
        assertEquals("0:30", formatDuration(30))
        assertEquals("2:05", formatDuration(125))
    }

    @Test
    fun parseReadsPlainNumbersAsMinutesAndColonAsExactSeconds() {
        assertEquals(1_200, parseDuration("20"))
        assertEquals(150, parseDuration(" 2:30 "))
        assertEquals(45, parseDuration("0:45"))
    }

    @Test
    fun parseRejectsMalformedAndOutOfRangeInput() {
        assertNull(parseDuration(""))
        assertNull(parseDuration("2:5"))
        assertNull(parseDuration("2:60"))
        assertNull(parseDuration("-1"))
        assertNull(parseDuration("1:2:3"))
        assertNull(parseDuration("0"))
        assertEquals(0, parseDuration("0", range = 0..60))
    }
}
