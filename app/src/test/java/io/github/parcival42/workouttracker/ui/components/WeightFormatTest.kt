package io.github.parcival42.workouttracker.ui.components

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class WeightFormatTest {
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
        assertEquals(52_500, parseKgToGrams("52,5"))
        assertEquals(52_500, parseKgToGrams("52.5"))
        assertEquals(40_000, parseKgToGrams(" 40 "))
        assertEquals(1_250, parseKgToGrams("1,25"))
    }

    @Test
    fun parseRejectsInvalidInput() {
        assertNull(parseKgToGrams(""))
        assertNull(parseKgToGrams("abc"))
        assertNull(parseKgToGrams("-5"))
        assertNull(parseKgToGrams("1001"))
    }

    @Test
    fun formatUsesLocaleAndDropsTrailingZeros() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals("52,5", formatKg(52_500))
        assertEquals("40", formatKg(40_000))
        Locale.setDefault(Locale.US)
        assertEquals("52.5", formatKg(52_500))
    }

    @Test
    fun formatThenParseRoundTrips() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals(12_750, parseKgToGrams(formatKg(12_750)))
    }
}
