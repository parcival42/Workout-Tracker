package io.github.parcival42.workouttracker.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.ui.components.formatKg
import io.github.parcival42.workouttracker.ui.components.parseKgToGrams

@Composable
fun DeviceDialog(
    title: String,
    initial: Device,
    onConfirm: (Device) -> Unit,
    onDismiss: () -> Unit,
    onArchive: (() -> Unit)? = null,
    // The type can't change once training was logged, otherwise the history would not fit the device.
    typeLocked: Boolean = false,
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var type by rememberSaveable { mutableStateOf(initial.type) }
    var minWeight by rememberSaveable { mutableStateOf(formatKg(initial.minWeightGrams)) }
    var weightStep by rememberSaveable { mutableStateOf(formatKg(initial.weightStepGrams)) }
    var measure by rememberSaveable { mutableStateOf(initial.measure) }
    var tracksDistance by rememberSaveable { mutableStateOf(initial.tracksDistance) }

    val isStrength = type == DeviceType.STRENGTH
    val minWeightGrams = parseKgToGrams(minWeight)
    val weightStepGrams = parseKgToGrams(weightStep)?.takeIf { it > 0 }
    val valid = name.isNotBlank() && (!isStrength || (minWeightGrams != null && weightStepGrams != null))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.device_name_label)) },
                    placeholder = { Text(stringResource(R.string.device_name_placeholder)) },
                    singleLine = true,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DeviceType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option },
                            enabled = !typeLocked,
                            shape = SegmentedButtonDefaults.itemShape(index, DeviceType.entries.size),
                            // No check mark: three labels are tight in a dialog, the colour shows the selection.
                            icon = {},
                        ) {
                            Text(stringResource(option.shortLabelRes()), maxLines = 1)
                        }
                    }
                }
                if (typeLocked) {
                    Text(
                        text = stringResource(R.string.device_type_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (type == DeviceType.BODYWEIGHT) {
                    Text(
                        text = stringResource(R.string.bodyweight_measure_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        BodyweightMeasure.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = measure == option,
                                onClick = { measure = option },
                                enabled = !typeLocked,
                                shape = SegmentedButtonDefaults.itemShape(index, BodyweightMeasure.entries.size),
                                // Like the type switch above: the colour shows the selection.
                                icon = {},
                            ) {
                                Text(stringResource(option.labelRes()))
                            }
                        }
                    }
                }
                if (type == DeviceType.CARDIO) {
                    // Not locked: switching it off only stops asking, logged distances stay.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = tracksDistance,
                                role = Role.Switch,
                                onValueChange = { tracksDistance = it },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.device_tracks_distance),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.device_tracks_distance_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = tracksDistance, onCheckedChange = null)
                    }
                }
                if (isStrength) {
                    OutlinedTextField(
                        value = minWeight,
                        onValueChange = { minWeight = it },
                        label = { Text(stringResource(R.string.device_min_weight_label)) },
                        supportingText = {
                            Text(
                                stringResource(
                                    if (minWeightGrams == null) R.string.device_min_weight_error
                                    else R.string.device_min_weight_help
                                )
                            )
                        },
                        singleLine = true,
                        isError = minWeightGrams == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = weightStep,
                        onValueChange = { weightStep = it },
                        label = { Text(stringResource(R.string.device_weight_step_label)) },
                        supportingText = {
                            Text(
                                stringResource(
                                    if (weightStepGrams == null) R.string.device_weight_step_error
                                    else R.string.device_weight_step_help
                                )
                            )
                        },
                        singleLine = true,
                        isError = weightStepGrams == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Cardio devices keep their previous weight values untouched.
                    val device = if (isStrength && minWeightGrams != null && weightStepGrams != null) {
                        initial.copy(minWeightGrams = minWeightGrams, weightStepGrams = weightStepGrams)
                    } else {
                        initial
                    }
                    if (valid) {
                        onConfirm(device.copy(name = name.trim(), type = type, measure = measure, tracksDistance = tracksDistance))
                    }
                },
                enabled = valid,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (onArchive != null) {
                    TextButton(onClick = onArchive) { Text(stringResource(R.string.action_archive)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

fun DeviceType.labelRes(): Int = when (this) {
    DeviceType.STRENGTH -> R.string.device_type_strength
    DeviceType.CARDIO -> R.string.device_type_cardio
    DeviceType.BODYWEIGHT -> R.string.device_type_bodyweight
}

// Short labels for the three narrow segments in the device dialog.
private fun DeviceType.shortLabelRes(): Int = when (this) {
    DeviceType.BODYWEIGHT -> R.string.device_type_bodyweight_short
    else -> labelRes()
}

fun BodyweightMeasure.labelRes(): Int = when (this) {
    BodyweightMeasure.REPS -> R.string.bodyweight_measure_reps
    BodyweightMeasure.TIME -> R.string.bodyweight_measure_time
}
