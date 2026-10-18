package io.github.parcival42.workouttracker.ui.circuit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CircuitStation(val deviceId: Long, val deviceName: String, val seconds: Int)

/**
 * Drives a [CircuitRun] in [scope]: ticks the clock, publishes [state], emits sound [events]
 * and reports every finished station with the time it ran through [onStationDone].
 */
class CircuitController(
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val onStationDone: suspend (station: CircuitStation, seconds: Int) -> Unit,
) {
    private var run: CircuitRun? = null
    private var stations: List<CircuitStation> = emptyList()
    private var rounds = 0
    private var job: Job? = null

    private val stateFlow = MutableStateFlow<CircuitUiState?>(null)
    val state: StateFlow<CircuitUiState?> = stateFlow.asStateFlow()

    private val eventsFlow = MutableSharedFlow<CircuitEvent>(extraBufferCapacity = 8)

    /** Sound cues at the real transitions; nothing is replayed, so returning to the screen stays quiet. */
    val events: SharedFlow<CircuitEvent> = eventsFlow.asSharedFlow()

    val isRunning: Boolean get() = run?.isFinished == false

    fun start(stations: List<CircuitStation>, rounds: Int, restSeconds: Int, countdownSeconds: Int) {
        val phases = buildCircuitPhases(stations.map { it.seconds }, rounds, restSeconds, countdownSeconds)
        if (phases.isEmpty()) return
        this.stations = stations
        this.rounds = rounds
        val started = CircuitRun.start(phases, clock())
        started.currentPhase?.let { eventsFlow.tryEmit(it.startEvent()) }
        publish(started)
        tick()
    }

    fun pause() {
        val current = run ?: return
        job?.cancel()
        publish(current.pause(clock()))
    }

    fun resume() {
        val current = run ?: return
        if (!current.isPaused) return
        publish(current.resume(clock()))
        tick()
    }

    /** Changes a station's time from its next pass on; the running station keeps its time. */
    fun updateStationSeconds(deviceId: Long, seconds: Int) {
        val index = stations.indexOfFirst { it.deviceId == deviceId }
        if (index < 0) return
        stations = stations.mapIndexed { i, station -> if (i == index) station.copy(seconds = seconds) else station }
        run?.let { publish(it.withStationSeconds(index, seconds)) }
    }

    /** Ends the current phase early; a skipped station is not reported as done. */
    fun skip() {
        val current = run ?: return
        if (current.isFinished) return
        val next = current.skip(clock())
        eventsFlow.tryEmit(next.currentPhase?.startEvent() ?: CircuitEvent.FINISHED)
        publish(next)
    }

    /** Ends a running circuit or closes a finished one. */
    fun stop() {
        job?.cancel()
        run = null
        stateFlow.value = null
    }

    private fun tick() {
        job?.cancel()
        job = scope.launch {
            var lastBeepSecond = -1
            while (true) {
                val current = run ?: break
                if (current.isFinished || current.isPaused) break
                val now = clock()
                val (next, completed) = current.advance(now)
                publish(next, now)
                // Outside this job, so pausing or stopping right at a station end cannot lose the log.
                completed.filterIsInstance<CircuitPhase.Station>().forEach { phase ->
                    stations.getOrNull(phase.stationIndex)?.let { station ->
                        scope.launch { onStationDone(station, phase.durationSeconds) }
                    }
                }
                if (completed.isNotEmpty()) {
                    eventsFlow.tryEmit(next.currentPhase?.startEvent() ?: CircuitEvent.FINISHED)
                    lastBeepSecond = -1
                }
                val remainingSeconds = next.remainingSeconds(now)
                if (!next.isFinished && remainingSeconds in 1..BEEP_SECONDS && remainingSeconds != lastBeepSecond) {
                    eventsFlow.tryEmit(CircuitEvent.TICK)
                    lastBeepSecond = remainingSeconds
                }
                delay(TICK_MILLIS)
            }
        }
    }

    private fun publish(next: CircuitRun, now: Long = clock()) {
        run = next
        val phase = next.currentPhase
        // During countdown and rest the upcoming station is the active one.
        val station = when (phase) {
            is CircuitPhase.Station -> stations.getOrNull(phase.stationIndex)
            is CircuitPhase.Rest -> stations.getOrNull(phase.nextStationIndex)
            is CircuitPhase.Countdown -> stations.firstOrNull()
            null -> null
        }
        stateFlow.value = CircuitUiState(
            phase = phase,
            activeDeviceId = station?.deviceId,
            deviceName = station?.deviceName.orEmpty(),
            rounds = rounds,
            remainingSeconds = next.remainingSeconds(now),
            isPaused = next.isPaused,
        )
    }

    private companion object {
        const val TICK_MILLIS = 100L
        // Short beeps in the last seconds of every phase.
        const val BEEP_SECONDS = 3
    }
}

private fun CircuitPhase.startEvent(): CircuitEvent = when (this) {
    is CircuitPhase.Station -> CircuitEvent.STATION_START
    is CircuitPhase.Rest -> CircuitEvent.REST_START
    is CircuitPhase.Countdown -> CircuitEvent.COUNTDOWN_START
}

// Rounded up, so the display reaches 0 exactly when the phase ends.
private fun CircuitRun.remainingSeconds(now: Long): Int = ((remainingAt(now) + 999) / 1_000).toInt()
