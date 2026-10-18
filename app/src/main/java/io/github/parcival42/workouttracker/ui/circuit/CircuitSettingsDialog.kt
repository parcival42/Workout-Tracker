package io.github.parcival42.workouttracker.ui.circuit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.CircuitSettings
import io.github.parcival42.workouttracker.ui.components.Stepper
import io.github.parcival42.workouttracker.ui.components.TimeField
import io.github.parcival42.workouttracker.ui.components.formatDuration

val CIRCUIT_ROUNDS_RANGE = 1..20
val CIRCUIT_STATION_SECONDS_RANGE = 5..3_600
val CIRCUIT_REST_SECONDS_RANGE = 0..600
val CIRCUIT_COUNTDOWN_SECONDS_RANGE = 0..60

/** Header row of a circuit plan in the editor; tapping it opens [CircuitSettingsDialog]. */
@Composable
fun CircuitSettingsRow(settings: CircuitSettings, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(stringResource(R.string.circuit_settings_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(
                R.string.circuit_settings_summary,
                pluralStringResource(R.plurals.circuit_rounds_count, settings.rounds, settings.rounds),
                formatDuration(settings.stationSeconds),
                formatDuration(settings.restSeconds),
                stringResource(R.string.duration_seconds, settings.countdownSeconds),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CircuitSettingsDialog(
    settings: CircuitSettings,
    onConfirm: (CircuitSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var rounds by rememberSaveable { mutableIntStateOf(settings.rounds) }
    var stationSeconds by rememberSaveable { mutableIntStateOf(settings.stationSeconds) }
    var restSeconds by rememberSaveable { mutableIntStateOf(settings.restSeconds) }
    var countdownSeconds by rememberSaveable { mutableIntStateOf(settings.countdownSeconds) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.circuit_settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(
                    label = stringResource(R.string.circuit_rounds),
                    value = rounds,
                    onValueChange = { rounds = it },
                    range = CIRCUIT_ROUNDS_RANGE,
                )
                StationTimeField(value = stationSeconds, onValueChange = { stationSeconds = it })
                TimeField(
                    label = stringResource(R.string.circuit_rest_time),
                    value = restSeconds,
                    onValueChange = { restSeconds = it },
                    range = CIRCUIT_REST_SECONDS_RANGE,
                )
                TimeField(
                    label = stringResource(R.string.circuit_countdown_time),
                    value = countdownSeconds,
                    onValueChange = { countdownSeconds = it },
                    range = CIRCUIT_COUNTDOWN_SECONDS_RANGE,
                    secondsOnly = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(CircuitSettings(rounds, stationSeconds, restSeconds, countdownSeconds)) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
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
