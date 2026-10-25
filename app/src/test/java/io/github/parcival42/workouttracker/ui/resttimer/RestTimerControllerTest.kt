package io.github.parcival42.workouttracker.ui.resttimer

import io.github.parcival42.workouttracker.ui.circuit.CircuitEvent
import io.github.parcival42.workouttracker.ui.circuit.CircuitEvent.STATION_START
import io.github.parcival42.workouttracker.ui.circuit.CircuitEvent.TICK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerControllerTest {

    private fun TestScope.controller(): Pair<RestTimerController, List<CircuitEvent>> {
        val controller = RestTimerController(backgroundScope, clock = { testScheduler.currentTime })
        val events = mutableListOf<CircuitEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.events.collect { events += it } }
        return controller to events
    }

    @Test
    fun ticksInLastSecondsThenStartToneAtEnd() = runTest {
        val (controller, events) = controller()
        controller.start(5)
        runCurrent()
        assertEquals(5, controller.state.value)
        advanceTimeBy(2_001)
        assertEquals(3, controller.state.value)
        advanceTimeBy(3_000)
        assertEquals(listOf(TICK, TICK, TICK, STATION_START), events)
        assertNull(controller.state.value)
    }

    @Test
    fun restartDropsTheOldEndTone() = runTest {
        val (controller, events) = controller()
        controller.start(3)
        advanceTimeBy(1_001)
        controller.start(10)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(false, STATION_START in events)
        assertEquals(5, controller.state.value)
    }

    @Test
    fun stopEndsWithoutSound() = runTest {
        val (controller, events) = controller()
        controller.start(3)
        advanceTimeBy(500)
        controller.stop()
        advanceTimeBy(10_000)
        assertNull(controller.state.value)
        assertEquals(false, STATION_START in events)
    }
}
