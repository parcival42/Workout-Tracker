package io.github.parcival42.workouttracker.ui.quicklog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.ui.components.EmptyHint
import io.github.parcival42.workouttracker.ui.components.formatKg
import io.github.parcival42.workouttracker.ui.components.stepWeightDown
import io.github.parcival42.workouttracker.ui.components.stepWeightUp
import io.github.parcival42.workouttracker.ui.components.DurationDialog
import io.github.parcival42.workouttracker.ui.devices.labelRes
import io.github.parcival42.workouttracker.ui.components.parseKmToMeters
import io.github.parcival42.workouttracker.ui.workout.DistanceField
import io.github.parcival42.workouttracker.ui.workout.DurationStepper
import io.github.parcival42.workouttracker.ui.workout.ValueStepper
import io.github.parcival42.workouttracker.ui.workout.WeightDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogScreen(
    onBack: () -> Unit,
    viewModel: QuickLogViewModel = viewModel(factory = QuickLogViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedDeviceId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    val onLogged: (QuickLogResult) -> Unit = { result ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val outcome = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.quick_log_logged, result.deviceName),
                actionLabel = resources.getString(R.string.action_undo),
                duration = SnackbarDuration.Long,
            )
            if (outcome == SnackbarResult.ActionPerformed) viewModel.undo(result)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quick_log_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val loadedDevices = devices ?: return@Scaffold
        if (loadedDevices.isEmpty()) {
            EmptyHint(
                text = stringResource(R.string.quick_log_no_devices),
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        val filtered = loadedDevices.filter { it.name.contains(query.trim(), ignoreCase = true) }
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.quick_log_search)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (filtered.isEmpty()) {
                EmptyHint(text = stringResource(R.string.quick_log_no_match))
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { device ->
                        DeviceRow(device = device, onClick = { selectedDeviceId = device.id })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // Looked up from the loaded list, so the sheet never starts with values from before loading.
    val selected = devices?.firstOrNull { it.id == selectedDeviceId }
    if (selected != null) {
        when (selected.type) {
            DeviceType.STRENGTH, DeviceType.BODYWEIGHT -> QuickLogStrengthSheet(
                device = selected,
                onLog = { weightGrams, reps, sets, durationSeconds ->
                    viewModel.logSets(selected, weightGrams, reps, sets, durationSeconds, onLogged)
                    selectedDeviceId = null
                },
                onDismiss = { selectedDeviceId = null },
            )
            DeviceType.CARDIO -> QuickLogCardioSheet(
                device = selected,
                onLog = { seconds, distanceMeters ->
                    viewModel.logSession(selected, seconds, distanceMeters, onLogged)
                    selectedDeviceId = null
                },
                onDismiss = { selectedDeviceId = null },
            )
        }
    }
}

@Composable
private fun DeviceRow(device: QuickLogDevice, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(device.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(device.type.labelRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Edits stay local until "Log"; dismissing the sheet discards them.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickLogStrengthSheet(
    device: QuickLogDevice,
    onLog: (weightGrams: Int, reps: Int, sets: Int, durationSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // Bodyweight exercises have no weight; those measured by time log a duration per set.
    val isBodyweight = device.type == DeviceType.BODYWEIGHT
    val timed = isBodyweight && device.measure == BodyweightMeasure.TIME
    var weightGrams by rememberSaveable { mutableIntStateOf(device.weightGrams) }
    var reps by rememberSaveable { mutableIntStateOf(device.reps) }
    var sets by rememberSaveable { mutableIntStateOf(device.sets) }
    var durationSeconds by rememberSaveable { mutableIntStateOf(device.holdSeconds) }
    var showWeightDialog by rememberSaveable { mutableStateOf(false) }
    var showDurationDialog by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetContent(deviceName = device.name) {
            if (!isBodyweight) {
                ValueStepper(
                    label = stringResource(R.string.weight_label),
                    value = stringResource(R.string.weight_kg, formatKg(weightGrams)),
                    onDecrease = { weightGrams = stepWeightDown(weightGrams, device.minWeightGrams, device.weightStepGrams) },
                    onIncrease = { weightGrams = stepWeightUp(weightGrams, device.minWeightGrams, device.weightStepGrams) },
                    decreaseEnabled = weightGrams > device.minWeightGrams,
                    onValueClick = { showWeightDialog = true },
                )
            }
            if (timed) {
                DurationStepper(
                    seconds = durationSeconds,
                    onSecondsChange = { durationSeconds = it },
                    onValueClick = { showDurationDialog = true },
                )
            } else {
                ValueStepper(
                    label = stringResource(R.string.target_reps),
                    value = reps.toString(),
                    onDecrease = { reps = (reps - 1).coerceAtLeast(1) },
                    onIncrease = { reps += 1 },
                    decreaseEnabled = reps > 1,
                )
            }
            ValueStepper(
                label = stringResource(R.string.target_sets),
                value = sets.toString(),
                onDecrease = { sets = (sets - 1).coerceAtLeast(1) },
                onIncrease = { sets = (sets + 1).coerceAtMost(QuickLogViewModel.MAX_SETS) },
                decreaseEnabled = sets > 1,
            )
            Button(onClick = { onLog(weightGrams, reps, sets, durationSeconds) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_log))
            }
        }
    }

    if (showWeightDialog) {
        WeightDialog(
            initialWeightGrams = weightGrams,
            onConfirm = {
                weightGrams = it
                showWeightDialog = false
            },
            onDismiss = { showWeightDialog = false },
        )
    }

    if (showDurationDialog) {
        DurationDialog(
            title = stringResource(R.string.duration_dialog_title),
            initialSeconds = durationSeconds,
            onConfirm = {
                durationSeconds = it
                showDurationDialog = false
            },
            onDismiss = { showDurationDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickLogCardioSheet(
    device: QuickLogDevice,
    onLog: (seconds: Int, distanceMeters: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var seconds by rememberSaveable { mutableIntStateOf(device.durationSeconds) }
    // Not prefilled: a distance is a result, an old value would be logged unnoticed.
    var distanceInput by rememberSaveable { mutableStateOf("") }
    var showDurationDialog by rememberSaveable { mutableStateOf(false) }
    val distanceMeters = parseKmToMeters(distanceInput)
    val distanceValid = distanceInput.isBlank() || distanceMeters != null

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetContent(deviceName = device.name) {
            DurationStepper(seconds = seconds, onSecondsChange = { seconds = it }, onValueClick = { showDurationDialog = true })
            if (device.tracksDistance) {
                DistanceField(value = distanceInput, onValueChange = { distanceInput = it }, isError = !distanceValid)
            }
            Button(
                onClick = { onLog(seconds, distanceMeters.takeIf { device.tracksDistance }) },
                enabled = distanceValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_log))
            }
        }
    }

    if (showDurationDialog) {
        DurationDialog(
            title = stringResource(R.string.duration_dialog_title),
            initialSeconds = seconds,
            onConfirm = {
                seconds = it
                showDurationDialog = false
            },
            onDismiss = { showDurationDialog = false },
        )
    }
}

@Composable
private fun SheetContent(deviceName: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(deviceName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.quick_log_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}
