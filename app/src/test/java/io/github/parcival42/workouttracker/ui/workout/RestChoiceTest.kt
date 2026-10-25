package io.github.parcival42.workouttracker.ui.workout

import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.RestTimerSettings
import io.github.parcival42.workouttracker.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestChoiceTest {

    private fun strength(entryId: Long, logged: Int, targetSets: Int = 3) = StrengthItem(
        entryId = entryId,
        deviceId = entryId,
        deviceName = "Device $entryId",
        targetSets = targetSets,
        todaySets = List(logged) { SetLog(deviceId = entryId, performedAt = it.toLong(), weightGrams = 10_000, reps = 10) },
        weightGrams = 10_000,
        minWeightGrams = 0,
        weightStepGrams = 2_500,
        reps = 10,
    )

    private fun cardio(entryId: Long, logged: Int) = CardioItem(
        entryId = entryId,
        deviceId = entryId,
        deviceName = "Device $entryId",
        todaySessions = List(logged) { CardioLog(deviceId = entryId, performedAt = it.toLong(), durationSeconds = 600) },
        durationSeconds = 600,
    )

    @Test
    fun setRestWhileSetsOpen() {
        val item = strength(1, logged = 0)
        assertEquals(RestKind.SET, chooseRest(item, loggedSets = 1, items = listOf(item, strength(2, logged = 0))))
    }

    @Test
    fun exerciseRestWhenLastSetOfExercise() {
        val item = strength(1, logged = 2)
        assertEquals(RestKind.EXERCISE, chooseRest(item, loggedSets = 1, items = listOf(item, strength(2, logged = 0))))
    }

    @Test
    fun exerciseRestOnDone() {
        val item = strength(1, logged = 0)
        assertEquals(RestKind.EXERCISE, chooseRest(item, loggedSets = 3, items = listOf(item, strength(2, logged = 1))))
    }

    @Test
    fun noRestWhenPlanCompletes() {
        val item = strength(1, logged = 2)
        assertNull(chooseRest(item, loggedSets = 1, items = listOf(item, strength(2, logged = 3), cardio(3, logged = 1))))
    }

    @Test
    fun extraSetGetsExerciseRestIfOtherOpen() {
        val item = strength(1, logged = 3)
        assertEquals(RestKind.EXERCISE, chooseRest(item, loggedSets = 1, items = listOf(item, strength(2, logged = 0))))
    }

    @Test
    fun extraSetInCompletePlanGetsNoRest() {
        val item = strength(1, logged = 3)
        assertNull(chooseRest(item, loggedSets = 1, items = listOf(item, strength(2, logged = 3))))
    }

    @Test
    fun cardioGetsExerciseRestIfOtherOpen() {
        val item = cardio(1, logged = 0)
        assertEquals(RestKind.EXERCISE, chooseRest(item, loggedSets = 1, items = listOf(item, strength(2, logged = 0))))
        assertNull(chooseRest(item, loggedSets = 1, items = listOf(item, strength(2, logged = 3))))
    }

    @Test
    fun exerciseRestFallsBackToSetRest() {
        val settings = RestTimerSettings(enabled = true, setSeconds = 60, exerciseEnabled = false, exerciseSeconds = 120)
        assertEquals(60, settings.secondsFor(RestKind.EXERCISE))
        assertEquals(120, settings.copy(exerciseEnabled = true).secondsFor(RestKind.EXERCISE))
        assertEquals(60, settings.copy(exerciseEnabled = true).secondsFor(RestKind.SET))
    }
}
