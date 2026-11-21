package io.github.parcival42.workouttracker.ui.history

import io.github.parcival42.workouttracker.data.Device
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeriesColorSlotsTest {

    @Test
    fun outOfRangeColorIsAutomatic() {
        assertNull(Device(name = "a", color = 8).fixedColorSlot())
        assertNull(Device(name = "a", color = -1).fixedColorSlot())
        assertNull(Device(name = "a").fixedColorSlot())
        assertEquals(0, Device(name = "a", color = 0).fixedColorSlot())
        assertEquals(7, Device(name = "a", color = 7).fixedColorSlot())
    }

    @Test
    fun fixedColorWinsOverStoredSlot() {
        assertEquals(5, shownSlot(Device(name = "a", color = 5), 2))
        assertEquals(2, shownSlot(Device(name = "a"), 2))
        assertEquals(2, shownSlot(null, 2))
    }

    @Test
    fun automaticDeviceSkipsShownSlots() {
        val result = addToSelection(
            selection = mapOf(1L to 0, 2L to 5),
            deviceId = 3L,
            fixedSlots = mapOf(2L to 1),
            visibleIds = setOf(1L, 2L, 3L),
        )
        assertEquals(mapOf(1L to 0, 2L to 5, 3L to 2), result)
    }

    @Test
    fun fixedDevicesMayShareTheirColor() {
        val result = addToSelection(mapOf(1L to 3), 2L, fixedSlots = mapOf(1L to 3, 2L to 3), visibleIds = setOf(1L, 2L))
        assertEquals(mapOf(1L to 3, 2L to 3), result)
    }

    @Test
    fun fixedDeviceMovesAutomaticDeviceOffItsColor() {
        // Device 1 is automatic on slot 0, device 3 fixed on slot 1; device 2 is fixed on slot 0.
        val result = addToSelection(
            selection = mapOf(1L to 0, 3L to 1),
            deviceId = 2L,
            fixedSlots = mapOf(2L to 0, 3L to 1),
            visibleIds = setOf(1L, 2L, 3L),
        )
        assertEquals(mapOf(1L to 2, 2L to 0, 3L to 1), result)
    }

    @Test
    fun automaticDeviceOnOtherColorStays() {
        val result = addToSelection(mapOf(1L to 4), 2L, fixedSlots = mapOf(2L to 0), visibleIds = setOf(1L, 2L))
        assertEquals(mapOf(1L to 4, 2L to 0), result)
    }

    @Test
    fun fullVisibleSelectionIgnoresClick() {
        val full = (1L..8L).associateWith { (it - 1).toInt() }
        val result = addToSelection(full, 9L, fixedSlots = emptyMap(), visibleIds = (1L..9L).toSet())
        assertEquals(full, result)
    }

    @Test
    fun fullSelectionDropsHiddenDevices() {
        val full = (1L..8L).associateWith { (it - 1).toInt() }
        val result = addToSelection(full, 9L, fixedSlots = emptyMap(), visibleIds = (1L..6L).toSet() + 9L)
        assertEquals((1L..6L).associateWith { (it - 1).toInt() } + (9L to 6), result)
    }
}
