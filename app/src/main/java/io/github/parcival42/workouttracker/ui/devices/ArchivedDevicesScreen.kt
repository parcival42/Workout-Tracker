package io.github.parcival42.workouttracker.ui.devices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.ArchivedDevice
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.ui.components.EmptyHint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedDevicesScreen(
    onBack: () -> Unit,
    viewModel: ArchivedDevicesViewModel = viewModel(factory = ArchivedDevicesViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    var deletingDeviceId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.devices_archived_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val loadedDevices = devices ?: return@Scaffold
        if (loadedDevices.isEmpty()) {
            EmptyHint(
                text = stringResource(R.string.devices_archived_empty),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(loadedDevices, key = { it.device.id }) { archived ->
                    ArchivedDeviceRow(
                        archived = archived,
                        onRestore = { viewModel.restore(archived.device.id) },
                        onDelete = { deletingDeviceId = archived.device.id },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    devices?.firstOrNull { it.device.id == deletingDeviceId }?.let { archived ->
        DeleteDeviceDialog(
            archived = archived,
            onConfirm = {
                viewModel.delete(archived.device.id)
                deletingDeviceId = null
            },
            onDismiss = { deletingDeviceId = null },
        )
    }
}

@Composable
private fun ArchivedDeviceRow(archived: ArchivedDevice, onRestore: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(archived.device.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = logCountLabel(archived),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onRestore) {
            Text(stringResource(R.string.action_restore))
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.action_delete),
            )
        }
    }
}

@Composable
private fun logCountLabel(archived: ArchivedDevice): String = when {
    archived.device.type == DeviceType.CARDIO && archived.sessionCount > 0 ->
        pluralStringResource(R.plurals.log_count_sessions, archived.sessionCount, archived.sessionCount)
    archived.device.type != DeviceType.CARDIO && archived.setCount > 0 ->
        pluralStringResource(R.plurals.log_count_sets, archived.setCount, archived.setCount)
    else -> stringResource(R.string.log_count_none)
}

@Composable
private fun DeleteDeviceDialog(archived: ArchivedDevice, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val name = archived.device.name
    // A device logs either sets or sessions, depending on its type.
    val logCount = if (archived.device.type == DeviceType.CARDIO) archived.sessionCount else archived.setCount
    val message = when {
        logCount == 0 -> stringResource(R.string.device_delete_message_no_logs, name)
        archived.device.type == DeviceType.CARDIO ->
            pluralStringResource(R.plurals.device_delete_message_sessions, logCount, name, logCount)
        else -> pluralStringResource(R.plurals.device_delete_message_sets, logCount, name, logCount)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.device_delete_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
