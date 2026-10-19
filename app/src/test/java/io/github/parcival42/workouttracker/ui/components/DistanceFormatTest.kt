package io.github.parcival42.workouttracker.ui.components

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class DistanceFormatTest {
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun parseAcceptsCommaAndDot() {
        assertEquals(2_400, parseKmToMeters("2,4"))
        assertEquals(2_400, parseKmToMeters("2.4"))
        assertEquals(5_000, parseKmToMeters(" 5 "))
        assertEquals(2_345, parseKmToMeters("2,345"))
    }

    @Test
    fun parseRejectsInvalidInput() {
        assertNull(parseKmToMeters(""))
        assertNull(parseKmToMeters("abc"))
        assertNull(parseKmToMeters("-1"))
        assertNull(parseKmToMeters("0"))
        assertNull(parseKmToMeters("1001"))
    }

    @Test
    fun formatIsExactAndUsesLocale() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals("2,4", formatKm(2_400))
        assertEquals("2,345", formatKm(2_345))
        assertEquals("1000", formatKm(1_000_000))
        Locale.setDefault(Locale.US)
        assertEquals("2.4", formatKm(2_400))
    }

    @Test
    fun formatThenParseRoundTrips() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals(12_345, parseKmToMeters(formatKm(12_345)))
        assertEquals(1_000_000, parseKmToMeters(formatKm(1_000_000)))
    }
}
