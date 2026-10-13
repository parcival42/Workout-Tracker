package com.example.workouttracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightStepsTest {
    private val min = 13_000
    private val step = 2_500

    @Test
    fun stepsAlongTheGridFromTheMinimum() {
        assertEquals(15_500, stepWeightUp(13_000, min, step))
        assertEquals(18_000, stepWeightUp(15_500, min, step))
        assertEquals(15_500, stepWeightDown(18_000, min, step))
    }

    @Test
    fun snapsOffGridValuesToTheNeighbouringStep() {
        assertEquals(18_000, stepWeightUp(16_000, min, step))
        assertEquals(15_500, stepWeightDown(16_000, min, step))
    }

    @Test
    fun neverGoesBelowTheMinimum() {
        assertEquals(13_000, stepWeightDown(13_000, min, step))
        assertEquals(13_000, stepWeightDown(14_000, min, step))
    }

    @Test
    fun valueBelowMinimumJumpsToMinimum() {
        assertEquals(13_000, stepWeightUp(0, min, step))
        assertEquals(13_000, stepWeightDown(5_000, min, step))
    }
}
