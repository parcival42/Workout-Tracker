package io.github.parcival42.workouttracker.ui.workout

import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.PlanEntry
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class NewPlanEntryTest {

    private val defaults = UserSettings(defaultTargetSets = 4, defaultTargetReps = 12, defaultTargetDurationSeconds = 1_500)
    private val legPress = Device(id = 1, name = "Leg Press", minWeightGrams = 40_000)
    private val treadmill = Device(id = 2, name = "Treadmill", type = DeviceType.CARDIO)
    private val plank = Device(id = 3, name = "Plank", type = DeviceType.BODYWEIGHT, measure = BodyweightMeasure.TIME)
    private val pushUps = Device(id = 4, name = "Push-ups", type = DeviceType.BODYWEIGHT)

    @Test
    fun strengthWithoutHistoryStartsAtMinimumAndDefaults() {
        val entry = newPlanEntry(legPress, defaults, lastSet = null, lastSession = null)
        assertEquals(40_000, entry.weightGrams)
        assertEquals(4, entry.targetSets)
        assertEquals(12, entry.targetReps)
        assertEquals(1, entry.deviceId)
    }

    @Test
    fun strengthStartsWhereTheUserLeftOff() {
        val last = SetLog(deviceId = 1, performedAt = 0, weightGrams = 55_000, reps = 8)
        val entry = newPlanEntry(legPress, defaults, lastSet = last, lastSession = null)
        assertEquals(55_000, entry.weightGrams)
        assertEquals(8, entry.targetReps)
        // The number of sets is a plan decision, never taken from the history.
        assertEquals(4, entry.targetSets)
    }

    @Test
    fun timedCircuitSetWithoutRepsFallsBackToDefaultReps() {
        val last = SetLog(deviceId = 1, performedAt = 0, weightGrams = 50_000, reps = null, durationSeconds = 60)
        val entry = newPlanEntry(legPress, defaults, lastSet = last, lastSession = null)
        assertEquals(50_000, entry.weightGrams)
        assertEquals(12, entry.targetReps)
    }

    @Test
    fun cardioUsesLastSessionElseDefaultInWholeMinutes() {
        val last = CardioLog(deviceId = 2, performedAt = 0, durationSeconds = 1_530)
        assertEquals(1_530, newPlanEntry(treadmill, defaults, lastSet = null, lastSession = last).targetDurationSeconds)
        assertEquals(1_500, newPlanEntry(treadmill, defaults, lastSet = null, lastSession = null).targetDurationSeconds)
        assertEquals(0, newPlanEntry(treadmill, defaults, lastSet = null, lastSession = last).weightGrams)
    }

    @Test
    fun timedBodyweightUsesLastHoldElseDefaultHold() {
        val last = SetLog(deviceId = 3, performedAt = 0, weightGrams = 0, reps = null, durationSeconds = 75)
        assertEquals(75, newPlanEntry(plank, defaults, lastSet = last, lastSession = null).targetDurationSeconds)
        assertEquals(
            PlanEntry.DEFAULT_HOLD_SECONDS,
            newPlanEntry(plank, defaults, lastSet = null, lastSession = null).targetDurationSeconds,
        )
    }

    @Test
    fun bodyweightHasNoWeight() {
        val last = SetLog(deviceId = 4, performedAt = 0, weightGrams = 0, reps = 20)
        val entry = newPlanEntry(pushUps, defaults, lastSet = last, lastSession = null)
        assertEquals(0, entry.weightGrams)
        assertEquals(20, entry.targetReps)
    }
}
