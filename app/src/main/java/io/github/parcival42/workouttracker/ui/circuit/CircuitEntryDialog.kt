package io.github.parcival42.workouttracker.ui.circuit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.CircuitSettings
import io.github.parcival42.workouttracker.data.PlanEntryItem

/** Plan entry of a circuit plan: only an optional own station time; stations have no sets or reps. */
@Composable
fun CircuitEntryDialog(
    entry: PlanEntryItem,
    circuit: CircuitSettings,
    onConfirm: (stationSeconds: Int?) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var ownTime by rememberSaveable { mutableStateOf(entry.stationSeconds != null) }
    var stationSeconds by rememberSaveable { mutableIntStateOf(entry.stationSeconds ?: circuit.stationSeconds) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.deviceName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.circuit_own_station_time), modifier = Modifier.weight(1f))
                    Switch(checked = ownTime, onCheckedChange = { ownTime = it })
                }
                if (ownTime) {
                    StationTimeField(value = stationSeconds, onValueChange = { stationSeconds = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(stationSeconds.takeIf { ownTime }) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.action_remove_from_plan))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )
}
