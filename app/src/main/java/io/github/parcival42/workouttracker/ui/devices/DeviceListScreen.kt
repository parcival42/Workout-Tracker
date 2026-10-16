package io.github.parcival42.workouttracker.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.ui.components.EmptyHint
import io.github.parcival42.workouttracker.ui.components.formatKg
import kotlinx.coroutines.launch

@Composable
fun DeviceListScreen(
    onShowArchived: () -> Unit,
    viewModel: DeviceListViewModel = viewModel(factory = DeviceListViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val archivedDevices by viewModel.archivedDevices.collectAsStateWithLifecycle()
    val deviceIdsWithLogs by viewModel.deviceIdsWithLogs.collectAsStateWithLifecycle()
    var showAddChoice by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showTemplateDialog by rememberSaveable { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<Device?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    DeviceListContent(
        devices = devices,
        hasArchivedDevices = archivedDevices.isNotEmpty(),
        snackbarHostState = snackbarHostState,
        onAddClick = { showAddChoice = true },
        onTemplatesClick = { showTemplateDialog = true },
        onDeviceClick = { editingDevice = it },
        onShowArchived = onShowArchived,
    )

    if (showAddChoice) {
        AddDeviceSheet(
            onOwnDevice = {
                showAddChoice = false
                showAddDialog = true
            },
            onTemplates = {
                showAddChoice = false
                showTemplateDialog = true
            },
            onDismiss = { showAddChoice = false },
        )
    }

    if (showTemplateDialog) {
        TemplateDialog(
            existingNames = (devices + archivedDevices).mapTo(HashSet()) { it.name },
            onConfirm = {
                viewModel.addAll(it)
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false },
        )
    }

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
    hasArchivedDevices: Boolean,
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onDeviceClick: (Device) -> Unit,
    onShowArchived: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.devices_title)) },
                actions = {
                    if (hasArchivedDevices) {
                        IconButton(onClick = onShowArchived) {
                            Icon(
                                painter = painterResource(R.drawable.ic_archive),
                                contentDescription = stringResource(R.string.devices_archived_title),
                            )
                        }
                    }
                },
            )
        },
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
        if (devices.isEmpty()) {
            EmptyHint(
                text = stringResource(R.string.devices_empty),
                modifier = Modifier.padding(innerPadding),
                action = {
                    OutlinedButton(onClick = onTemplatesClick) {
                        Text(stringResource(R.string.template_open_action))
                    }
                },
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

// The single entry point for adding devices, so the templates are not hidden behind a separate icon.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeviceSheet(onOwnDevice: () -> Unit, onTemplates: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.device_add_own)) },
                supportingContent = { Text(stringResource(R.string.device_add_own_description)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onOwnDevice),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.template_open_action)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_list), contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onTemplates),
            )
        }
    }
}

@Composable
private fun DeviceRow(device: Device, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
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
