package com.example.workouttracker.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.workouttracker.R
import com.example.workouttracker.data.Device
import com.example.workouttracker.ui.components.formatKg
import com.example.workouttracker.ui.components.parseKgToGrams

@Composable
fun DeviceDialog(
    title: String,
    initial: Device,
    onConfirm: (Device) -> Unit,
    onDismiss: () -> Unit,
    onArchive: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var minWeight by rememberSaveable { mutableStateOf(formatKg(initial.minWeightGrams)) }
    var weightStep by rememberSaveable { mutableStateOf(formatKg(initial.weightStepGrams)) }

    val minWeightGrams = parseKgToGrams(minWeight)
    val weightStepGrams = parseKgToGrams(weightStep)?.takeIf { it > 0 }
    val valid = name.isNotBlank() && minWeightGrams != null && weightStepGrams != null

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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (minWeightGrams != null && weightStepGrams != null) {
                        onConfirm(
                            initial.copy(
                                name = name.trim(),
                                minWeightGrams = minWeightGrams,
                                weightStepGrams = weightStepGrams,
                            )
                        )
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
