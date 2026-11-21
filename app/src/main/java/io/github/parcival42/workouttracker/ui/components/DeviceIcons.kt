package io.github.parcival42.workouttracker.ui.components

import androidx.annotation.DrawableRes
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.defaultIcon

/**
 * Every icon a device can have, by key. Keys are stored in the database and in backups, so they
 * are never renamed or reused for another meaning; the drawable behind a key can be replaced
 * freely, and a new icon is a new entry.
 */
val DeviceIcons: Map<String, Int> = mapOf(
    DeviceType.STRENGTH.defaultIcon to R.drawable.ic_fitness,
    DeviceType.CARDIO.defaultIcon to R.drawable.ic_cardio,
    DeviceType.BODYWEIGHT.defaultIcon to R.drawable.ic_bodyweight,
    // Template icons (see DeviceTemplates and the frozen name list in data/TemplateIcons.kt).
    "leg" to R.drawable.ic_device_leg,
    "arm" to R.drawable.ic_device_arm,
    "biceps" to R.drawable.ic_device_biceps,
    "treadmill" to R.drawable.ic_device_treadmill,
    "bike" to R.drawable.ic_device_bike,
    "stairs" to R.drawable.ic_device_stairs,
    "run" to R.drawable.ic_device_run,
    "swimming" to R.drawable.ic_device_swimming,
)

/** The icon behind [key]; a key that does not exist (any more) shows the type icon. */
@DrawableRes
fun deviceIconRes(key: String, type: DeviceType): Int = DeviceIcons[key] ?: DeviceIcons.getValue(type.defaultIcon)
