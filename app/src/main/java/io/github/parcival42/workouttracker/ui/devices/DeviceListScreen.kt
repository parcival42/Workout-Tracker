package io.github.parcival42.workouttracker.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.ui.components.DeviceTypeBadge
import io.github.parcival42.workouttracker.ui.components.EmptyHint
import io.github.parcival42.workouttracker.ui.components.cardioSummary
import io.github.parcival42.workouttracker.ui.components.setSummary
import io.github.parcival42.workouttracker.ui.components.formatKg
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun DeviceListScreen(
    onShowArchived: () -> Unit,
    viewModel: DeviceListViewModel = viewModel(factory = DeviceListViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val archivedDevices by viewModel.archivedDevices.collectAsStateWithLifecycle()
    val deviceIdsWithLogs by viewModel.deviceIdsWithLogs.collectAsStateWithLifecycle()
    val lastLogs by viewModel.lastLogs.collectAsStateWithLifecycle()
    var showAddChoice by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showTemplateDialog by rememberSaveable { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<Device?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    DeviceListContent(
        devices = devices,
        lastLogs = lastLogs,
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
    lastLogs: Map<Long, LastLog>,
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
            // Grouped by type; the FAB must not cover the last row.
            val groups = remember(devices) { devices.groupBy { it.type } }
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                DeviceType.entries.forEach { type ->
                    val group = groups[type] ?: return@forEach
                    item(key = "header-$type") {
                        Text(
                            text = stringResource(type.labelRes()),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                                .semantics { heading() },
                        )
                    }
                    items(group, key = { it.id }) { device ->
                        DeviceRow(device = device, lastLog = lastLogs[device.id], onClick = { onDeviceClick(device) })
                    }
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

/** Trained devices show their last entry; untrained ones their setup (weights, measure). */
@Composable
private fun DeviceRow(device: Device, lastLog: LastLog?, onClick: () -> Unit) {
    val resources = LocalResources.current
    val lastText = lastLog?.let { log ->
        val entry = log.set?.let { resources.setSummary(it, device.type == DeviceType.BODYWEIGHT) }
            ?: log.session?.let { resources.cardioSummary(it) }
            ?: return@let null
        stringResource(R.string.device_last_log, entry, relativeDay(log.performedAt))
    }
    ListItem(
        headlineContent = { Text(text = device.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(lastText ?: setupText(device)) },
        leadingContent = { DeviceTypeBadge(type = device.type) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** "today", "yesterday" or "3 days ago". */
@Composable
private fun relativeDay(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(day, LocalDate.now(zone)).toInt().coerceAtLeast(0)
    return when (days) {
        0 -> stringResource(R.string.relative_today)
        1 -> stringResource(R.string.relative_yesterday)
        else -> pluralStringResource(R.plurals.relative_days_ago, days, days)
    }
}

@Composable
private fun setupText(device: Device): String = when (device.type) {
    DeviceType.STRENGTH -> stringResource(
        R.string.device_weight_info,
        formatKg(device.minWeightGrams),
        formatKg(device.weightStepGrams),
    )
    DeviceType.CARDIO -> stringResource(
        if (device.tracksDistance) R.string.device_cardio_distance_info else R.string.device_type_cardio
    )
    DeviceType.BODYWEIGHT -> stringResource(
        R.string.device_bodyweight_info,
        stringResource(R.string.device_type_bodyweight),
        stringResource(device.measure.labelRes()),
    )
}
