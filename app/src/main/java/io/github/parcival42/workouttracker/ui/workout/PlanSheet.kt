package io.github.parcival42.workouttracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.CircuitSettings
import io.github.parcival42.workouttracker.ui.circuit.CircuitSettingsFields

/**
 * The plan itself: name and, for circuit plans ([circuitSettings] not null), the circuit
 * settings. Changes apply on "Save"; deleting asks for confirmation first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanSheet(
    planName: String,
    circuitSettings: CircuitSettings?,
    onSave: (name: String, circuitSettings: CircuitSettings?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(planName) }
    // Saved as plain ints; CircuitSettings is not saveable.
    var rounds by rememberSaveable { mutableIntStateOf(circuitSettings?.rounds ?: 0) }
    var stationSeconds by rememberSaveable { mutableIntStateOf(circuitSettings?.stationSeconds ?: 0) }
    var restSeconds by rememberSaveable { mutableIntStateOf(circuitSettings?.restSeconds ?: 0) }
    var countdownSeconds by rememberSaveable { mutableIntStateOf(circuitSettings?.countdownSeconds ?: 0) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val editedCircuit = circuitSettings?.let { CircuitSettings(rounds, stationSeconds, restSeconds, countdownSeconds) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Expanded right away: with circuit settings the sheet is long and "Save" would sit below the fold.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Deleting is only a small icon up here, away from "Save" at the bottom, so it is not hit by mistake.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.action_edit_plan),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.action_delete_plan),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name_label)) },
                singleLine = true,
                isError = name.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (editedCircuit != null) {
                Text(stringResource(R.string.circuit_settings_title), style = MaterialTheme.typography.titleMedium)
                CircuitSettingsFields(
                    settings = editedCircuit,
                    onSettingsChange = {
                        rounds = it.rounds
                        stationSeconds = it.stationSeconds
                        restSeconds = it.restSeconds
                        countdownSeconds = it.countdownSeconds
                    },
                )
            }
            Button(
                onClick = { onSave(name, editedCircuit) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.plan_delete_title)) },
            text = { Text(stringResource(R.string.plan_delete_message, planName)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
