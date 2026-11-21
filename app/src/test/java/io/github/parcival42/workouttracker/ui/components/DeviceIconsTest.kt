package io.github.parcival42.workouttracker.ui.components

import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.defaultIcon
import io.github.parcival42.workouttracker.ui.devices.DeviceTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIconsTest {

    @Test
    fun everyTypeIconExists() {
        DeviceType.entries.forEach { assertTrue(it.name, it.defaultIcon in DeviceIcons) }
    }

    @Test
    fun newDeviceStartsWithItsTypeIcon() {
        assertEquals("cardio", Device(name = "Treadmill", type = DeviceType.CARDIO).icon)
    }

    @Test
    fun unknownKeyShowsTypeIcon() {
        assertEquals(
            deviceIconRes(DeviceType.BODYWEIGHT.defaultIcon, DeviceType.BODYWEIGHT),
            deviceIconRes("removed_icon", DeviceType.BODYWEIGHT),
        )
    }

    @Test
    fun knownKeyWinsOverType() {
        assertEquals(
            deviceIconRes(DeviceType.CARDIO.defaultIcon, DeviceType.CARDIO),
            deviceIconRes(DeviceType.CARDIO.defaultIcon, DeviceType.STRENGTH),
        )
    }

    @Test
    fun everyTemplateIconExists() {
        DeviceTemplates.forEach { assertTrue(it.icon, it.icon in DeviceIcons) }
    }

    @Test
    fun templateWithoutOwnIconUsesTypeIcon() {
        DeviceTemplates.filter { it.icon !in TEMPLATE_KEYS }.forEach { assertEquals(it.type.defaultIcon, it.icon) }
    }

    private companion object {
        val TEMPLATE_KEYS = setOf("leg", "arm", "biceps", "treadmill", "bike", "stairs", "run", "swimming")
    }
}
