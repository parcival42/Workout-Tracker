package io.github.parcival42.workouttracker.ui.circuit

import io.github.parcival42.workouttracker.ui.circuit.CircuitPhase.Countdown
import io.github.parcival42.workouttracker.ui.circuit.CircuitPhase.Rest
import io.github.parcival42.workouttracker.ui.circuit.CircuitPhase.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CircuitRunTest {

    @Test
    fun phasesHaveRestBetweenStationsAndRoundsButNotAtTheEnd() {
        val phases = buildCircuitPhases(listOf(30, 45), rounds = 2, restSeconds = 10, countdownSeconds = 5)
        assertEquals(
            listOf(
                Countdown(5),
                Station(0, 1, 30), Rest(1, 1, 10), Station(1, 1, 45),
                Rest(0, 2, 10),
                Station(0, 2, 30), Rest(1, 2, 10), Station(1, 2, 45),
            ),
            phases,
        )
    }

    @Test
    fun zeroCountdownAndRestAreLeftOut() {
        val phases = buildCircuitPhases(listOf(30), rounds = 2, restSeconds = 0, countdownSeconds = 0)
        assertEquals(listOf(Station(0, 1, 30), Station(0, 2, 30)), phases)
    }

    @Test
    fun noStationsMeansNoPhases() {
        assertTrue(buildCircuitPhases(emptyList(), rounds = 3, restSeconds = 10, countdownSeconds = 10).isEmpty())
    }

    @Test
    fun advanceCompletesPhasesWithoutDrift() {
        val run = CircuitRun.start(listOf(Countdown(5), Station(0, 1, 30), Rest(0, 2, 10)), now = 1_000)
        val (afterCountdown, completed) = run.advance(now = 6_500)
        assertEquals(listOf(Countdown(5)), completed)
        assertEquals(1, afterCountdown.phaseIndex)
        // The station started at 6_000, when the countdown ended, not at the late tick.
        assertEquals(29_500, afterCountdown.remainingAt(6_500))
    }

    @Test
    fun advanceCanCompleteSeveralPhasesAtOnce() {
        val run = CircuitRun.start(listOf(Countdown(5), Station(0, 1, 30)), now = 0)
        val (finished, completed) = run.advance(now = 40_000)
        assertEquals(listOf(Countdown(5), Station(0, 1, 30)), completed)
        assertTrue(finished.isFinished)
    }

    @Test
    fun pauseStopsTheClock() {
        val paused = CircuitRun.start(listOf(Station(0, 1, 30)), now = 0).pause(now = 10_000)
        assertEquals(20_000, paused.remainingAt(100_000))
        assertTrue(paused.advance(now = 100_000).second.isEmpty())
        val resumed = paused.resume(now = 100_000)
        assertEquals(15_000, resumed.remainingAt(105_000))
    }

    @Test
    fun newStationTimeOnlyChangesLaterPasses() {
        val phases = listOf(Station(0, 1, 30), Station(1, 1, 30), Station(0, 2, 30))
        val changed = CircuitRun.start(phases, now = 0).withStationSeconds(stationIndex = 0, seconds = 60)
        assertEquals(listOf(Station(0, 1, 30), Station(1, 1, 30), Station(0, 2, 60)), changed.phases)
        assertEquals(30_000, changed.remainingAt(0))
    }

    @Test
    fun skipMovesOnWithoutCompleting() {
        val run = CircuitRun.start(listOf(Station(0, 1, 30), Station(1, 1, 30)), now = 0)
        val skipped = run.skip(now = 5_000)
        assertEquals(1, skipped.phaseIndex)
        assertEquals(30_000, skipped.remainingAt(5_000))
        assertTrue(skipped.advance(now = 5_000).second.isEmpty())
    }
}
