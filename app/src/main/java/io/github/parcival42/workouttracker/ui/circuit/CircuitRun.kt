package io.github.parcival42.workouttracker.ui.circuit

sealed interface CircuitPhase {
    val durationSeconds: Int

    data class Countdown(override val durationSeconds: Int) : CircuitPhase

    /** [stationIndex] is the position in the plan; [round] starts at 1. */
    data class Station(val stationIndex: Int, val round: Int, override val durationSeconds: Int) : CircuitPhase

    /** Rest before the station [nextStationIndex] in [nextRound]. */
    data class Rest(val nextStationIndex: Int, val nextRound: Int, override val durationSeconds: Int) : CircuitPhase
}

/**
 * Countdown, then every station once per round with a rest in between (also between rounds,
 * but not after the last station). A zero countdown or rest is left out.
 */
fun buildCircuitPhases(
    stationSeconds: List<Int>,
    rounds: Int,
    restSeconds: Int,
    countdownSeconds: Int,
): List<CircuitPhase> {
    if (stationSeconds.isEmpty() || rounds <= 0) return emptyList()
    val phases = mutableListOf<CircuitPhase>()
    if (countdownSeconds > 0) phases += CircuitPhase.Countdown(countdownSeconds)
    for (round in 1..rounds) {
        stationSeconds.forEachIndexed { index, seconds ->
            if (phases.lastOrNull() is CircuitPhase.Station && restSeconds > 0) {
                phases += CircuitPhase.Rest(nextStationIndex = index, nextRound = round, durationSeconds = restSeconds)
            }
            phases += CircuitPhase.Station(stationIndex = index, round = round, durationSeconds = seconds)
        }
    }
    return phases
}

/**
 * Immutable state of a running circuit. Times are monotonic clock millis; the current phase
 * had [remainingMillis] left at [updatedAt] and only counts down while not paused.
 */
data class CircuitRun(
    val phases: List<CircuitPhase>,
    val phaseIndex: Int,
    val remainingMillis: Long,
    val updatedAt: Long,
    val isPaused: Boolean = false,
) {
    val isFinished: Boolean get() = phaseIndex >= phases.size
    val currentPhase: CircuitPhase? get() = phases.getOrNull(phaseIndex)

    fun remainingAt(now: Long): Long =
        if (isPaused || isFinished) remainingMillis else (remainingMillis - (now - updatedAt)).coerceAtLeast(0)

    fun pause(now: Long): CircuitRun =
        if (isPaused || isFinished) this else copy(remainingMillis = remainingAt(now), updatedAt = now, isPaused = true)

    fun resume(now: Long): CircuitRun = if (!isPaused) this else copy(updatedAt = now, isPaused = false)

    /** Sets the duration of every later pass of a station; the running phase keeps its time. */
    fun withStationSeconds(stationIndex: Int, seconds: Int): CircuitRun = copy(
        phases = phases.mapIndexed { index, phase ->
            if (index > phaseIndex && phase is CircuitPhase.Station && phase.stationIndex == stationIndex) {
                phase.copy(durationSeconds = seconds)
            } else {
                phase
            }
        },
    )

    /** Ends the current phase early; a skipped phase does not count as completed. */
    fun skip(now: Long): CircuitRun = if (isFinished) this else moveTo(phaseIndex + 1, startedAt = now)

    /**
     * Moves past every phase that has run out by [now] and returns the phases that completed.
     * The next phase starts when the previous one ended, so ticking late does not add drift.
     */
    fun advance(now: Long): Pair<CircuitRun, List<CircuitPhase>> {
        var run = this
        val completed = mutableListOf<CircuitPhase>()
        while (!run.isFinished && !run.isPaused && run.remainingAt(now) == 0L) {
            completed += run.phases[run.phaseIndex]
            run = run.moveTo(run.phaseIndex + 1, startedAt = run.updatedAt + run.remainingMillis)
        }
        return run to completed
    }

    private fun moveTo(index: Int, startedAt: Long): CircuitRun = copy(
        phaseIndex = index,
        remainingMillis = phases.getOrNull(index)?.let { it.durationSeconds * MILLIS_PER_SECOND } ?: 0L,
        updatedAt = startedAt,
    )

    companion object {
        private const val MILLIS_PER_SECOND = 1_000L

        fun start(phases: List<CircuitPhase>, now: Long): CircuitRun =
            CircuitRun(phases = phases, phaseIndex = -1, remainingMillis = 0, updatedAt = now).moveTo(0, startedAt = now)
    }
}
