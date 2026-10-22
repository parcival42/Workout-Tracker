package io.github.parcival42.workouttracker.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.Device

/**
 * Picks several exercises at once; they are added in the order they were checked. Exercises
 * already in the plan are shown checked and disabled (a device can be in a plan only once).
 */
@Composable
internal fun AddExercisesDialog(
    devices: List<Device>,
    inPlanDeviceIds: Set<Long>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    // In selection order; saved as a list because sets are not saveable by default.
    var selected by rememberSaveable { mutableStateOf(listOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_exercises_title)) },
        text = {
            if (devices.isEmpty()) {
                Text(stringResource(R.string.add_exercises_none))
            } else {
                LazyColumn {
                    items(devices, key = { it.id }) { device ->
                        ExerciseRow(
                            name = device.name,
                            checked = device.id in selected,
                            inPlan = device.id in inPlanDeviceIds,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + device.id else selected - device.id
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }, enabled = selected.isNotEmpty()) {
                Text(stringResource(R.string.template_add_action, selected.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ExerciseRow(
    name: String,
    checked: Boolean,
    inPlan: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = !inPlan,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked || inPlan, onCheckedChange = null, enabled = !inPlan)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (inPlan) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (inPlan) {
                Text(
                    text = stringResource(R.string.add_exercises_in_plan),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
