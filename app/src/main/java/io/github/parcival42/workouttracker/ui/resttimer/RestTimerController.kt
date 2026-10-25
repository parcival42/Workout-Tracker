package io.github.parcival42.workouttracker.ui.resttimer

import io.github.parcival42.workouttracker.ui.circuit.CircuitEvent
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

/**
 * Drives one [RestTimer] in [scope]: publishes the remaining seconds (null = no rest) and emits
 * the circuit's sound cues, ticks in the last seconds and the station start tone at the end.
 */
class RestTimerController(
    private val scope: CoroutineScope,
    private val clock: () -> Long,
) {
    private var job: Job? = null

    private val stateFlow = MutableStateFlow<Int?>(null)
    val state: StateFlow<Int?> = stateFlow.asStateFlow()

    private val eventsFlow = MutableSharedFlow<CircuitEvent>(extraBufferCapacity = 8)

    /** Nothing is replayed, so cues emitted while no screen listens are simply lost. */
    val events: SharedFlow<CircuitEvent> = eventsFlow.asSharedFlow()

    /** Starts a rest of [seconds]; a running rest is replaced without its end tone. */
    fun start(seconds: Int) {
        job?.cancel()
        val timer = RestTimer.start(seconds, clock())
        stateFlow.value = timer.remainingSeconds(clock())
        job = scope.launch {
            var lastBeepSecond = -1
            while (true) {
                val now = clock()
                if (timer.isFinished(now)) {
                    stateFlow.value = null
                    eventsFlow.tryEmit(CircuitEvent.STATION_START)
                    break
                }
                val remaining = timer.remainingSeconds(now)
                stateFlow.value = remaining
                if (remaining in 1..BEEP_SECONDS && remaining != lastBeepSecond) {
                    eventsFlow.tryEmit(CircuitEvent.TICK)
                    lastBeepSecond = remaining
                }
                // Wakes only when the shown second changes, so a long rest in the background stays cheap.
                val untilNextSecond = timer.remainingMillis(now) % MILLIS_PER_SECOND
                delay(if (untilNextSecond == 0L) MILLIS_PER_SECOND else untilNextSecond)
            }
        }
    }

    /** Ends a running rest without a sound. */
    fun stop() {
        job?.cancel()
        job = null
        stateFlow.value = null
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val BEEP_SECONDS = 3
    }
}
