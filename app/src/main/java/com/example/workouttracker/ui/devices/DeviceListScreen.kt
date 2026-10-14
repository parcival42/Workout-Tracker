package com.example.workouttracker.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.R
import com.example.workouttracker.data.Device
import com.example.workouttracker.data.DeviceType
import com.example.workouttracker.ui.components.EmptyHint
import com.example.workouttracker.ui.components.formatKg
import kotlinx.coroutines.launch

@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel = viewModel(factory = DeviceListViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val archivedDevices by viewModel.archivedDevices.collectAsStateWithLifecycle()
    val deviceIdsWithLogs by viewModel.deviceIdsWithLogs.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<Device?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    DeviceListContent(
        devices = devices,
        archivedDevices = archivedDevices,
        snackbarHostState = snackbarHostState,
        onAddClick = { showAddDialog = true },
        onDeviceClick = { editingDevice = it },
        onRestore = viewModel::restore,
    )

    if (showAddDialog) {
        DeviceDialog(
            title = stringResource(R.string.device_add_title),
            initial = Device(name = ""),
            onConfirm = {
                viewModel.save(it)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editingDevice?.let { device ->
        DeviceDialog(
            title = stringResource(R.string.device_edit_title),
            initial = device,
            onConfirm = {
                viewModel.save(it)
                editingDevice = null
            },
            onDismiss = { editingDevice = null },
            typeLocked = device.id in deviceIdsWithLogs,
            onArchive = {
                viewModel.archive(device)
                editingDevice = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.device_archived_message, device.name),
                        actionLabel = resources.getString(R.string.action_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(device)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceListContent(
    devices: List<Device>,
    archivedDevices: List<Device>,
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onDeviceClick: (Device) -> Unit,
    onRestore: (Device) -> Unit,
) {
    var showArchived by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.devices_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.device_add_title),
                )
            }
        },
    ) { innerPadding ->
        if (devices.isEmpty() && archivedDevices.isEmpty()) {
            EmptyHint(
                text = stringResource(R.string.devices_empty),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                if (devices.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.devices_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(devices, key = { it.id }) { device ->
                    DeviceRow(device = device, onClick = { onDeviceClick(device) })
                    HorizontalDivider()
                }
                if (archivedDevices.isNotEmpty()) {
                    item(key = "archived-toggle") {
                        TextButton(
                            onClick = { showArchived = !showArchived },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        ) {
                            Text(
                                if (showArchived) {
                                    stringResource(R.string.devices_archived_hide)
                                } else {
                                    stringResource(R.string.devices_archived_show, archivedDevices.size)
                                }
                            )
                        }
                    }
                    if (showArchived) {
                        items(archivedDevices, key = { "archived-${it.id}" }) { device ->
                            ArchivedDeviceRow(device = device, onRestore = { onRestore(device) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedDeviceRow(device: Device, onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = device.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRestore) {
            Text(stringResource(R.string.action_restore))
        }
    }
}

@Composable
private fun DeviceRow(device: Device, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_image_placeholder),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = device.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = when (device.type) {
                    DeviceType.STRENGTH -> stringResource(
                        R.string.device_weight_info,
                        formatKg(device.minWeightGrams),
                        formatKg(device.weightStepGrams),
                    )
                    DeviceType.CARDIO -> stringResource(R.string.device_type_cardio)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
