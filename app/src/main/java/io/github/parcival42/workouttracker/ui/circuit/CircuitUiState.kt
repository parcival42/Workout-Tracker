package io.github.parcival42.workouttracker.ui.circuit

/** What a circuit plan looks like before it starts. */
data class CircuitPreview(val stations: Int, val rounds: Int, val totalSeconds: Int)

/** A started circuit; [phase] is null once it has finished. */
data class CircuitUiState(
    val phase: CircuitPhase?,
    // The current station, or the next one during countdown and rest.
    val activeDeviceId: Long?,
    val deviceName: String,
    val rounds: Int,
    val remainingSeconds: Int,
    val isPaused: Boolean,
) {
    val isFinished: Boolean get() = phase == null
}

enum class CircuitEvent {
    COUNTDOWN_START,
    STATION_START,
    REST_START,
    TICK,
    FINISHED,
}
