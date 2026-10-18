package io.github.parcival42.workouttracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.ui.circuit.StationTimeField
import io.github.parcival42.workouttracker.ui.components.formatKg
import io.github.parcival42.workouttracker.ui.components.stepWeightDown
import io.github.parcival42.workouttracker.ui.components.stepWeightUp

/**
 * Next station of a circuit plan: the weight (strength only; a draft for the next log, like
 * [NextSetSheet]) and the station time, which is saved in the plan. Edits apply only on "Apply".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StationSheet(
    item: WorkoutItem,
    stationSeconds: Int,
    onApply: (weightGrams: Int?, stationSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // Only strength devices with a weight; bodyweight stations just have their time.
    val strength = (item as? StrengthItem)?.takeIf { !it.isBodyweight }
    var weightGrams by rememberSaveable { mutableIntStateOf(strength?.weightGrams ?: 0) }
    var seconds by rememberSaveable { mutableIntStateOf(stationSeconds) }
    var showWeightDialog by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(item.deviceName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.next_station_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (strength != null) {
                ValueStepper(
                    label = stringResource(R.string.weight_label),
                    value = stringResource(R.string.weight_kg, formatKg(weightGrams)),
                    onDecrease = { weightGrams = stepWeightDown(weightGrams, strength.minWeightGrams, strength.weightStepGrams) },
                    onIncrease = { weightGrams = stepWeightUp(weightGrams, strength.minWeightGrams, strength.weightStepGrams) },
                    decreaseEnabled = weightGrams > strength.minWeightGrams,
                    onValueClick = { showWeightDialog = true },
                )
            }
            Column {
                StationTimeField(value = seconds, onValueChange = { seconds = it })
                Text(
                    text = stringResource(R.string.station_time_plan_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { onApply(weightGrams.takeIf { strength != null }, seconds) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_apply))
            }
        }
    }

    if (showWeightDialog) {
        WeightDialog(
            initialWeightGrams = weightGrams,
            onConfirm = {
                weightGrams = it
                showWeightDialog = false
            },
            onDismiss = { showWeightDialog = false },
        )
    }
}
