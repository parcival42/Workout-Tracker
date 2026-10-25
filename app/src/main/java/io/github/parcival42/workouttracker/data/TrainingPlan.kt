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
    // Rest timer of normal plans (read through [restTimerSettings]); a switched-off time stays stored.
    @ColumnInfo(name = "rest_timer_enabled", defaultValue = "0") val restTimerEnabled: Boolean = false,
    @ColumnInfo(name = "rest_set_seconds") val restSetSeconds: Int? = null,
    @ColumnInfo(name = "rest_exercise_enabled", defaultValue = "0") val restExerciseEnabled: Boolean = false,
    @ColumnInfo(name = "rest_exercise_seconds") val restExerciseSeconds: Int? = null,
)

/**
 * Rest timer of a normal plan: [setSeconds] runs between sets and, unless [exerciseEnabled],
 * also between exercises.
 */
data class RestTimerSettings(
    val enabled: Boolean = false,
    val setSeconds: Int = DEFAULT_SET_SECONDS,
    val exerciseEnabled: Boolean = false,
    val exerciseSeconds: Int = DEFAULT_EXERCISE_SECONDS,
) {
    companion object {
        const val DEFAULT_SET_SECONDS = 60
        const val DEFAULT_EXERCISE_SECONDS = 120
    }
}

val TrainingPlan.restTimerSettings: RestTimerSettings
    get() {
        val defaults = RestTimerSettings()
        return RestTimerSettings(
            enabled = restTimerEnabled,
            setSeconds = restSetSeconds ?: defaults.setSeconds,
            exerciseEnabled = restExerciseEnabled,
            exerciseSeconds = restExerciseSeconds ?: defaults.exerciseSeconds,
        )
    }

fun TrainingPlan.withRestTimerSettings(settings: RestTimerSettings): TrainingPlan = copy(
    restTimerEnabled = settings.enabled,
    restSetSeconds = settings.setSeconds,
    restExerciseEnabled = settings.exerciseEnabled,
    restExerciseSeconds = settings.exerciseSeconds,
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
