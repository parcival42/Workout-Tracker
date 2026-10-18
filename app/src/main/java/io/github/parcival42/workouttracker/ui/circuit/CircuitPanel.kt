package io.github.parcival42.workouttracker.ui.circuit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.ui.components.SECONDS_PER_MINUTE
import io.github.parcival42.workouttracker.ui.components.formatMinutesSeconds

/** Fixed panel above the device cards of a circuit plan: start button, running timer or the end. */
@Composable
fun CircuitPanel(
    preview: CircuitPreview,
    circuit: CircuitUiState?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
) {
    var showAbortDialog by rememberSaveable { mutableStateOf(false) }
    // Back would close the app and lose the running circuit, so it asks first.
    BackHandler(enabled = circuit != null && !circuit.isFinished) { showAbortDialog = true }

    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val phase = circuit?.phase
            when {
                circuit == null -> {
                    Text(
                        text = stringResource(
                            R.string.circuit_summary,
                            pluralStringResource(R.plurals.circuit_stations_count, preview.stations, preview.stations),
                            pluralStringResource(R.plurals.circuit_rounds_count, preview.rounds, preview.rounds),
                            (preview.totalSeconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_start_circuit))
                    }
                }
                phase == null -> {
                    Text(stringResource(R.string.circuit_finished), style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_close))
                    }
                }
                else -> {
                    Text(
                        text = when (phase) {
                            is CircuitPhase.Countdown -> stringResource(R.string.circuit_phase_countdown)
                            is CircuitPhase.Station -> circuit.deviceName
                            is CircuitPhase.Rest -> stringResource(R.string.circuit_phase_rest, circuit.deviceName)
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    val round = when (phase) {
                        is CircuitPhase.Countdown -> 1
                        is CircuitPhase.Station -> phase.round
                        is CircuitPhase.Rest -> phase.nextRound
                    }
                    Text(
                        text = if (circuit.isPaused) {
                            stringResource(R.string.circuit_paused)
                        } else {
                            stringResource(R.string.circuit_phase_round, round, circuit.rounds)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMinutesSeconds(circuit.remainingSeconds),
                        style = MaterialTheme.typography.displayLarge,
                        color = if (phase is CircuitPhase.Station) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAbortDialog = true }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_abort), maxLines = 1)
                        }
                        OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_skip), maxLines = 1)
                        }
                        Button(onClick = if (circuit.isPaused) onResume else onPause, modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(if (circuit.isPaused) R.string.action_resume else R.string.action_pause),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAbortDialog) {
        AlertDialog(
            onDismissRequest = { showAbortDialog = false },
            title = { Text(stringResource(R.string.circuit_abort_title)) },
            text = { Text(stringResource(R.string.circuit_abort_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showAbortDialog = false
                    onStop()
                }) {
                    Text(stringResource(R.string.action_abort))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbortDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
