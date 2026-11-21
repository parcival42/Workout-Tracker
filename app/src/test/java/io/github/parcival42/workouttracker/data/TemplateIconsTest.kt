package io.github.parcival42.workouttracker.data

import io.github.parcival42.workouttracker.ui.components.DeviceIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateIconsTest {

    @Test
    fun templateNameInEitherLanguageGetsTemplateIcon() {
        assertEquals("leg", legacyDeviceIcon("Leg press", DeviceType.STRENGTH))
        assertEquals("leg", legacyDeviceIcon("Beinpresse", DeviceType.STRENGTH))
        assertEquals("biceps", legacyDeviceIcon("Bizepsmaschine", DeviceType.STRENGTH))
        assertEquals("bike", legacyDeviceIcon("Fahrradfahren", DeviceType.CARDIO))
        assertEquals("run", legacyDeviceIcon("Running", DeviceType.CARDIO))
    }

    @Test
    fun matchIgnoresCaseAndSurroundingSpaces() {
        assertEquals("leg", legacyDeviceIcon("  BEINPRESSE ", DeviceType.STRENGTH))
        assertEquals("leg", legacyDeviceIcon("abduktor (außen)", DeviceType.STRENGTH))
    }

    @Test
    fun otherNameGetsTypeIcon() {
        assertEquals("strength", legacyDeviceIcon("Beinpresse alt", DeviceType.STRENGTH))
        assertEquals("cardio", legacyDeviceIcon("Crosstrainer", DeviceType.CARDIO))
        assertEquals("bodyweight", legacyDeviceIcon("Plank", DeviceType.BODYWEIGHT))
    }

    @Test
    fun templateNameOfOtherTypeGetsTypeIcon() {
        assertEquals("bodyweight", legacyDeviceIcon("Laufen", DeviceType.BODYWEIGHT))
        assertEquals("strength", legacyDeviceIcon("Laufband", DeviceType.STRENGTH))
    }

    @Test
    fun everyLegacyIconExists() {
        val names = listOf("Leg press", "Shoulder press", "Biceps curl", "Treadmill", "Exercise bike", "Stair climber",
            "Running", "Swimming")
        val types = DeviceType.entries
        names.forEach { n -> types.forEach { t -> assertTrue(n, legacyDeviceIcon(n, t) in DeviceIcons) } }
    }
}
