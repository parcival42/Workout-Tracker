package io.github.parcival42.workouttracker.ui.devices

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.Device

/**
 * Lets the user pick several templates at once. Templates whose name already exists
 * (also among archived devices) are disabled to avoid duplicates.
 */
@Composable
fun TemplateDialog(
    existingNames: Set<String>,
    onConfirm: (List<Device>) -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalResources.current
    // Selected template indices; saved as a list because sets are not saveable by default.
    var selected by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val normalizedExisting = existingNames.mapTo(HashSet()) { it.trim().lowercase() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.template_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.template_dialog_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn {
                    TemplateGroup.entries.forEach { group ->
                        item(key = "group-${group.name}") {
                            Text(
                                text = stringResource(group.labelRes),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        val groupTemplates = DeviceTemplates.withIndex().filter { it.value.group == group }
                        items(groupTemplates, key = { it.index }) { (index, template) ->
                            val name = stringResource(template.nameRes)
                            val exists = name.lowercase() in normalizedExisting
                            TemplateRow(
                                name = name,
                                checked = index in selected,
                                exists = exists,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + index else selected - index
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val devices = selected.sorted().map { index ->
                        val template = DeviceTemplates[index]
                        Device(
                            name = resources.getString(template.nameRes),
                            type = template.type,
                            minWeightGrams = template.minWeightGrams,
                            weightStepGrams = template.weightStepGrams,
                            measure = template.measure,
                        )
                    }
                    onConfirm(devices)
                },
                enabled = selected.isNotEmpty(),
            ) {
                Text(stringResource(R.string.template_add_action, selected.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun TemplateRow(
    name: String,
    checked: Boolean,
    exists: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = !exists,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked || exists, onCheckedChange = null, enabled = !exists)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (exists) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (exists) {
                Text(
                    text = stringResource(R.string.template_already_added),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
