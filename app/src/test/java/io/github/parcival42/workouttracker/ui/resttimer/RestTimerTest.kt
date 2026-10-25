package io.github.parcival42.workouttracker.ui.resttimer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerTest {

    @Test
    fun remainingCountsDownFromStart() {
        val timer = RestTimer.start(60, now = 1_000)
        assertEquals(60, timer.remainingSeconds(1_000))
        // Rounded up, so the display reaches 0 exactly at the end.
        assertEquals(60, timer.remainingSeconds(1_001))
        assertEquals(2, timer.remainingSeconds(59_000))
        assertEquals(1, timer.remainingSeconds(60_000))
        assertEquals(0, timer.remainingSeconds(61_000))
    }

    @Test
    fun finishedOnlyAtEnd() {
        val timer = RestTimer.start(60, now = 1_000)
        assertFalse(timer.isFinished(60_999))
        assertTrue(timer.isFinished(61_000))
        assertEquals(0, timer.remainingMillis(100_000))
    }
}
