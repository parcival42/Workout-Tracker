package io.github.parcival42.workouttracker.ui.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.Device
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.PlanEntryItem
import io.github.parcival42.workouttracker.ui.components.EmptyHint
import io.github.parcival42.workouttracker.ui.components.MAX_DURATION_MINUTES
import io.github.parcival42.workouttracker.ui.components.NameDialog
import io.github.parcival42.workouttracker.ui.components.secondsToMinutes
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: PlanEditorViewModel = viewModel(factory = PlanEditorViewModel.Factory),
) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()

    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showAddDeviceDialog by rememberSaveable { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<PlanEntryItem?>(null) }

    // Local copy so drag & drop can reorder instantly; persisted when the drag ends.
    var orderedEntries by remember { mutableStateOf(entries) }
    LaunchedEffect(entries) { orderedEntries = entries }

    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        orderedEntries = orderedEntries.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.action_rename),
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.action_delete),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDeviceDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.plan_add_device_title),
                )
            }
        },
    ) { innerPadding ->
        if (orderedEntries.isEmpty()) {
            EmptyHint(
                text = stringResource(R.string.plan_entries_empty),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(state = lazyListState, modifier = Modifier.padding(innerPadding)) {
                items(orderedEntries, key = { it.id }) { entry ->
                    ReorderableItem(reorderableState, key = entry.id) { isDragging ->
                        Surface(tonalElevation = if (isDragging) 8.dp else 0.dp) {
                            Column {
                                PlanEntryRow(
                                    entry = entry,
                                    onClick = { editingEntry = entry },
                                    dragHandle = {
                                        IconButton(
                                            onClick = {},
                                            modifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                                },
                                                onDragStopped = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                                    viewModel.saveOrder(orderedEntries.map { it.id })
                                                },
                                            ),
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_drag_handle),
                                                contentDescription = stringResource(R.string.action_reorder),
                                            )
                                        }
                                    },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        NameDialog(
            title = stringResource(R.string.plan_rename_title),
            initialName = plan?.name.orEmpty(),
            onConfirm = { name ->
                viewModel.rename(name)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.plan_delete_title)) },
            text = { Text(stringResource(R.string.plan_delete_message, plan?.name.orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete(onDeleted = onDeleted)
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

    if (showAddDeviceDialog) {
        AddDeviceDialog(
            devices = availableDevices,
            onSelect = { device ->
                viewModel.addDevice(device.id)
                showAddDeviceDialog = false
            },
            onDismiss = { showAddDeviceDialog = false },
        )
    }

    editingEntry?.let { entry ->
        EntryTargetsDialog(
            entry = entry,
            onConfirm = { sets, reps ->
                viewModel.updateTargets(entry.id, sets, reps)
                editingEntry = null
            },
            onConfirmDuration = { minutes ->
                viewModel.updateTargetDuration(entry.id, minutes)
                editingEntry = null
            },
            onRemove = {
                viewModel.removeEntry(entry.id)
                editingEntry = null
            },
            onDismiss = { editingEntry = null },
        )
    }
}

@Composable
private fun PlanEntryRow(
    entry: PlanEntryItem,
    onClick: () -> Unit,
    dragHandle: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.deviceName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = when (entry.deviceType) {
                    DeviceType.STRENGTH -> stringResource(R.string.plan_entry_targets, entry.targetSets, entry.targetReps)
                    DeviceType.CARDIO -> stringResource(R.string.duration_min, secondsToMinutes(entry.targetDurationSeconds))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        dragHandle()
    }
}

@Composable
private fun AddDeviceDialog(
    devices: List<Device>,
    onSelect: (Device) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plan_add_device_title)) },
        text = {
            if (devices.isEmpty()) {
                Text(stringResource(R.string.plan_no_devices_available))
            } else {
                LazyColumn {
                    items(devices, key = { it.id }) { device ->
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(device) }
                                .padding(vertical = 14.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun EntryTargetsDialog(
    entry: PlanEntryItem,
    onConfirm: (sets: Int, reps: Int) -> Unit,
    onConfirmDuration: (minutes: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var sets by rememberSaveable { mutableIntStateOf(entry.targetSets) }
    var reps by rememberSaveable { mutableIntStateOf(entry.targetReps) }
    var minutes by rememberSaveable { mutableIntStateOf(secondsToMinutes(entry.targetDurationSeconds)) }
    val isCardio = entry.deviceType == DeviceType.CARDIO
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.deviceName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isCardio) {
                    Stepper(
                        label = stringResource(R.string.duration_label_min),
                        value = minutes,
                        onValueChange = { minutes = it },
                        range = 1..MAX_DURATION_MINUTES,
                    )
                } else {
                    Stepper(
                        label = stringResource(R.string.target_sets),
                        value = sets,
                        onValueChange = { sets = it },
                        range = 1..20,
                    )
                    Stepper(
                        label = stringResource(R.string.target_reps),
                        value = reps,
                        onValueChange = { reps = it },
                        range = 1..100,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (isCardio) onConfirmDuration(minutes) else onConfirm(sets, reps) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.action_remove_from_plan))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1f))
        FilledTonalIconButton(
            onClick = { onValueChange(value - 1) },
            enabled = value > range.first,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = stringResource(R.string.action_decrease),
            )
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp),
        )
        FilledTonalIconButton(
            onClick = { onValueChange(value + 1) },
            enabled = value < range.last,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.action_increase),
            )
        }
    }
}
