package io.github.parcival42.workouttracker.ui.workout

import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.PlanEntry
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.UserSettings
import io.github.parcival42.workouttracker.ui.components.SECONDS_PER_MINUTE
import io.github.parcival42.workouttracker.ui.components.TARGET_MINUTES_RANGE
import io.github.parcival42.workouttracker.ui.components.TARGET_REPS_RANGE
import io.github.parcival42.workouttracker.ui.components.TARGET_SETS_RANGE
import io.github.parcival42.workouttracker.ui.components.secondsToMinutes

/**
 * Start values of a device newly added to a plan: where the user left off with it ([lastSet] /
 * [lastSession], from any plan or a quick log), else the device minimum and the personal defaults.
 * Afterwards the values belong to the plan. Plan and sort order are set when inserting.
 */
internal fun newPlanEntry(device: Device, defaults: UserSettings, lastSet: SetLog?, lastSession: CardioLog?): PlanEntry {
    val defaultReps = defaults.defaultTargetReps.coerceIn(TARGET_REPS_RANGE)
    return PlanEntry(
        planId = 0,
        deviceId = device.id,
        sortOrder = 0,
        targetSets = defaults.defaultTargetSets.coerceIn(TARGET_SETS_RANGE),
        targetReps = lastSet?.reps ?: defaultReps,
        targetDurationSeconds = when {
            device.type == DeviceType.CARDIO -> lastSession?.durationSeconds
                ?: (secondsToMinutes(defaults.defaultTargetDurationSeconds).coerceIn(TARGET_MINUTES_RANGE) * SECONDS_PER_MINUTE)
            // A timed bodyweight set (plank) is much shorter than the cardio default.
            device.type == DeviceType.BODYWEIGHT && device.measure == BodyweightMeasure.TIME ->
                lastSet?.durationSeconds ?: PlanEntry.DEFAULT_HOLD_SECONDS
            else -> PlanEntry.DEFAULT_TARGET_DURATION_SECONDS
        },
        weightGrams = if (device.type == DeviceType.STRENGTH) lastSet?.weightGrams ?: device.minWeightGrams else 0,
    )
}
