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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.R
import com.example.workouttracker.data.Device
import com.example.workouttracker.ui.components.EmptyHint
import com.example.workouttracker.ui.components.formatKg

@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel = viewModel(factory = DeviceListViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<Device?>(null) }

    DeviceListContent(
        devices = devices,
        onAddClick = { showAddDialog = true },
        onDeviceClick = { editingDevice = it },
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
            onArchive = {
                viewModel.archive(device)
                editingDevice = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceListContent(
    devices: List<Device>,
    onAddClick: () -> Unit,
    onDeviceClick: (Device) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.devices_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.device_add_title),
                )
            }
        },
    ) { innerPadding ->
        if (devices.isEmpty()) {
            EmptyHint(
                text = stringResource(R.string.devices_empty),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(devices, key = { it.id }) { device ->
                    DeviceRow(device = device, onClick = { onDeviceClick(device) })
                    HorizontalDivider()
                }
            }
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
                text = stringResource(
                    R.string.device_weight_info,
                    formatKg(device.startWeightGrams),
                    formatKg(device.weightStepGrams),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
