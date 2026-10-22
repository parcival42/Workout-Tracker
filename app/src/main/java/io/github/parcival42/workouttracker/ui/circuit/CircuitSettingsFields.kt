package io.github.parcival42.workouttracker.ui.circuit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.CircuitSettings
import io.github.parcival42.workouttracker.ui.components.Stepper
import io.github.parcival42.workouttracker.ui.components.TimeField

val CIRCUIT_ROUNDS_RANGE = 1..20
val CIRCUIT_STATION_SECONDS_RANGE = 5..3_600
val CIRCUIT_REST_SECONDS_RANGE = 0..600
val CIRCUIT_COUNTDOWN_SECONDS_RANGE = 0..60

/** Rounds, station time, rest and countdown of a circuit plan; the caller owns the edited state. */
@Composable
fun CircuitSettingsFields(settings: CircuitSettings, onSettingsChange: (CircuitSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Stepper(
            label = stringResource(R.string.circuit_rounds),
            value = settings.rounds,
            onValueChange = { onSettingsChange(settings.copy(rounds = it)) },
            range = CIRCUIT_ROUNDS_RANGE,
        )
        StationTimeField(value = settings.stationSeconds, onValueChange = { onSettingsChange(settings.copy(stationSeconds = it)) })
        TimeField(
            label = stringResource(R.string.circuit_rest_time),
            value = settings.restSeconds,
            onValueChange = { onSettingsChange(settings.copy(restSeconds = it)) },
            range = CIRCUIT_REST_SECONDS_RANGE,
        )
        TimeField(
            label = stringResource(R.string.circuit_countdown_time),
            value = settings.countdownSeconds,
            onValueChange = { onSettingsChange(settings.copy(countdownSeconds = it)) },
            range = CIRCUIT_COUNTDOWN_SECONDS_RANGE,
            secondsOnly = true,
        )
    }
}

@Composable
fun StationTimeField(value: Int, onValueChange: (Int) -> Unit) {
    TimeField(
        label = stringResource(R.string.circuit_station_time),
        value = value,
        onValueChange = onValueChange,
        range = CIRCUIT_STATION_SECONDS_RANGE,
    )
}
