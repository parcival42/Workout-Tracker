package io.github.parcival42.workouttracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_plan")
data class TrainingPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // At most one plan is the default; it starts automatically in the Training tab.
    @ColumnInfo(name = "is_default", defaultValue = "0") val isDefault: Boolean = false,
    // Set when the user restarts the plan on the same day; the workout only counts sets
    // logged since then (but never before midnight). Transient state, not part of the backup.
    @ColumnInfo(name = "session_started_at") val sessionStartedAt: Long? = null,
    // Fixed when the plan is created.
    @ColumnInfo(defaultValue = "NORMAL") val type: PlanType = PlanType.NORMAL,
    // Circuit settings; null for normal plans (read through [circuitSettings]).
    @ColumnInfo(name = "circuit_rounds") val circuitRounds: Int? = null,
    @ColumnInfo(name = "circuit_station_seconds") val circuitStationSeconds: Int? = null,
    @ColumnInfo(name = "circuit_rest_seconds") val circuitRestSeconds: Int? = null,
    @ColumnInfo(name = "circuit_countdown_seconds") val circuitCountdownSeconds: Int? = null,
)

/** Circuit settings of a plan; unset values fall back to the defaults. */
data class CircuitSettings(
    val rounds: Int = DEFAULT_ROUNDS,
    val stationSeconds: Int = DEFAULT_STATION_SECONDS,
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    val countdownSeconds: Int = DEFAULT_COUNTDOWN_SECONDS,
) {
    companion object {
        const val DEFAULT_ROUNDS = 2
        const val DEFAULT_STATION_SECONDS = 300
        const val DEFAULT_REST_SECONDS = 60
        const val DEFAULT_COUNTDOWN_SECONDS = 10
    }
}

val TrainingPlan.circuitSettings: CircuitSettings
    get() {
        val defaults = CircuitSettings()
        return CircuitSettings(
            rounds = circuitRounds ?: defaults.rounds,
            stationSeconds = circuitStationSeconds ?: defaults.stationSeconds,
            restSeconds = circuitRestSeconds ?: defaults.restSeconds,
            countdownSeconds = circuitCountdownSeconds ?: defaults.countdownSeconds,
        )
    }

fun TrainingPlan.withCircuitSettings(settings: CircuitSettings): TrainingPlan = copy(
    circuitRounds = settings.rounds,
    circuitStationSeconds = settings.stationSeconds,
    circuitRestSeconds = settings.restSeconds,
    circuitCountdownSeconds = settings.countdownSeconds,
)
