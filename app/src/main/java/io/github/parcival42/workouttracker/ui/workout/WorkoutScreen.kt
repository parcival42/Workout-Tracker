package io.github.parcival42.workouttracker.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import io.github.parcival42.workouttracker.ui.components.TARGET_SETS_RANGE
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.ui.circuit.CircuitEffects
import io.github.parcival42.workouttracker.ui.circuit.CircuitPanel
import io.github.parcival42.workouttracker.ui.circuit.TimerEffects
import io.github.parcival42.workouttracker.ui.resttimer.REST_SLOT_HEIGHT
import io.github.parcival42.workouttracker.ui.resttimer.RestTimerBar
import io.github.parcival42.workouttracker.ui.components.DurationDialog
import io.github.parcival42.workouttracker.ui.components.EmptyHint
import io.github.parcival42.workouttracker.ui.components.bodyweightSetSummary
import io.github.parcival42.workouttracker.ui.components.cardioSummary
import io.github.parcival42.workouttracker.ui.components.formatDuration
import io.github.parcival42.workouttracker.ui.components.formatKg
import io.github.parcival42.workouttracker.ui.components.parseDuration
import io.github.parcival42.workouttracker.ui.components.parseKgToGrams
import io.github.parcival42.workouttracker.ui.components.setSummary
import io.github.parcival42.workouttracker.ui.components.stepWeightDown
import io.github.parcival42.workouttracker.ui.components.stepWeightUp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    planId: Long,
    onSwitchPlan: () -> Unit,
    viewModel: WorkoutViewModel = viewModel(key = "workout-$planId", factory = WorkoutViewModel.factory(planId)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingSet by remember { mutableStateOf<Pair<Int, SetLog>?>(null) }
    var editingSession by remember { mutableStateOf<Pair<Int, CardioLog>?>(null) }
    var nextSetDeviceId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPlanSheet by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    // Reorder mode: compact cards to drag or remove; everything else on the screen is disabled.
    var reordering by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val listState = rememberLazyListState()
    val circuit = state.circuit
    // While a circuit runs, the plan cannot be left or changed from here.
    val circuitActive = circuit != null && !circuit.isFinished
    // In a circuit plan only the timer logs; the cards keep their chips and undo.
    val isCircuitPlan = state.circuitPreview != null

    // Read straight from the timer, so it is current right away after the background.
    val restSeconds by viewModel.restSeconds.collectAsStateWithLifecycle()
    val lastRestSeconds = remember { IntArray(1) }
    // One effect for both plan types, so its sounds are not built twice while the plan loads.
    TimerEffects(
        events = if (isCircuitPlan) viewModel.circuitEvents else viewModel.restTimerEvents,
        keepScreenOn = if (isCircuitPlan) circuitActive && circuit?.isPaused == false else restSeconds != null,
    )
    // The rest keeps running when the screen is left (only its sounds stop); a circuit pauses.
    if (isCircuitPlan) CircuitEffects(onLeave = viewModel::pauseCircuit)

    // Bring the active station into view.
    LaunchedEffect(circuit?.activeDeviceId) {
        val index = state.items.indexOfFirst { it.deviceId == circuit?.activeDeviceId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    val restartPlan: () -> Unit = {
        viewModel.restartPlan()
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.workout_plan_restarted),
                actionLabel = resources.getString(R.string.action_undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoRestartPlan()
        }
    }

    // The distance is prefilled from the last session, so it may be stale: show what was saved and
    // offer correcting it right away.
    val onSessionLogged: (CardioLog) -> Unit = { session ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.workout_session_logged, resources.cardioSummary(session)),
                actionLabel = resources.getString(R.string.action_adjust),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                // Number it like its chip; the list may not contain the new session yet.
                val sessions = (state.items.firstOrNull { it.deviceId == session.deviceId } as? CardioItem)?.todaySessions.orEmpty()
                val index = sessions.indexOfFirst { it.id == session.id }
                editingSession = (if (index >= 0) index + 1 else sessions.size + 1) to (sessions.getOrNull(index) ?: session)
            }
        }
    }

    val removeEntry: (WorkoutItem) -> Unit = { item ->
        viewModel.removeEntry(item)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.workout_entry_removed, item.deviceName),
                actionLabel = resources.getString(R.string.action_undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoRemoveEntry()
        }
    }

    // Nothing left to reorder, or a circuit was started meanwhile.
    if (reordering && (state.items.isEmpty() || circuitActive)) reordering = false
    BackHandler(enabled = reordering) { reordering = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (reordering) stringResource(R.string.action_reorder) else state.planName) },
                navigationIcon = {
                    if (!reordering && !circuitActive) {
                        IconButton(onClick = onSwitchPlan) {
                            Icon(
                                painter = painterResource(R.drawable.ic_list),
                                contentDescription = stringResource(R.string.action_switch_plan),
                            )
                        }
                    }
                },
                actions = {
                    if (reordering) {
                        TextButton(onClick = { reordering = false }) { Text(stringResource(R.string.action_done)) }
                    } else if (!circuitActive) {
                        IconButton(onClick = { showPlanSheet = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.action_edit_plan),
                            )
                        }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = stringResource(R.string.add_exercises_title),
                            )
                        }
                        if (state.items.isNotEmpty()) {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_more_vert),
                                        contentDescription = stringResource(R.string.action_more),
                                    )
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_reorder)) },
                                        leadingIcon = { Icon(painterResource(R.drawable.ic_drag_handle), contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            reordering = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_restart_plan)) },
                                        leadingIcon = { Icon(painterResource(R.drawable.ic_restart), contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            restartPlan()
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.items.isEmpty()) {
            EmptyHint(
                text = stringResource(R.string.workout_empty),
                modifier = Modifier.padding(innerPadding),
                action = {
                    Button(onClick = { showAddDialog = true }) { Text(stringResource(R.string.add_exercises_title)) }
                },
            )
        } else if (reordering) {
            ReorderList(
                items = state.items,
                onSaveOrder = viewModel::saveOrder,
                onRemove = removeEntry,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            Column(modifier = Modifier.padding(innerPadding)) {
                state.circuitPreview?.let { preview ->
                    CircuitPanel(
                        preview = preview,
                        circuit = circuit,
                        onStart = viewModel::startCircuit,
                        onPause = viewModel::pauseCircuit,
                        onResume = viewModel::resumeCircuit,
                        onSkip = viewModel::skipCircuitPhase,
                        onStop = viewModel::stopCircuit,
                    )
                }
                val done = state.items.count { it.isComplete }
                if (state.restTimerSettings?.enabled == true) {
                    // Rest bar and progress share one slot of fixed height, so a rest starting or
                    // ending never moves the cards under the finger.
                    Crossfade(
                        targetState = restSeconds != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(REST_SLOT_HEIGHT),
                        label = "rest",
                    ) { resting ->
                        if (resting) {
                            // Plain holder, not state: keeps the last time while the bar fades out.
                            restSeconds?.let { lastRestSeconds[0] = it }
                            RestTimerBar(
                                remainingSeconds = lastRestSeconds[0],
                                onSkip = viewModel::skipRest,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                PlanProgress(done = done, total = state.items.size)
                            }
                        }
                    }
                } else if (!isCircuitPlan) {
                    PlanProgress(done = done, total = state.items.size)
                }
                // The first open device is the one to train next (circuit plans have the timer for that).
                val activeDeviceId = state.items.firstOrNull { !it.isComplete }?.deviceId.takeIf { !isCircuitPlan }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = { it.deviceId }) { item ->
                        val highlighted = circuitActive && item.deviceId == circuit?.activeDeviceId
                        val isActive = item.deviceId == activeDeviceId
                        when (item) {
                            is StrengthItem -> WorkoutCard(
                                item = item,
                                onEditNextSet = { nextSetDeviceId = item.deviceId },
                                onCompleteDevice = { viewModel.completeDevice(item) },
                                onCompleteOneSet = { viewModel.completeOneSet(item) },
                                onUndo = { viewModel.undoLastSet(item) },
                                onSetClick = { number, set -> editingSet = number to set },
                                highlighted = highlighted,
                                isCircuitPlan = isCircuitPlan,
                                isActive = isActive,
                            )
                            is CardioItem -> CardioCard(
                                item = item,
                                onEditNextSession = { nextSetDeviceId = item.deviceId },
                                onLogSession = {
                                    if (item.tracksDistance) viewModel.logSession(item, onSessionLogged) else viewModel.logSession(item)
                                },
                                onUndo = { viewModel.undoLastSession(item) },
                                onSessionClick = { number, session -> editingSession = number to session },
                                highlighted = highlighted,
                                isCircuitPlan = isCircuitPlan,
                                isActive = isActive,
                            )
                        }
                    }
                    if (state.isComplete && !circuitActive) {
                        item(key = "plan-complete") {
                            PlanCompleteCard(items = state.items, onRestart = restartPlan)
                        }
                    }
                }
            }
        }
    }

    val sheetItem = state.items.firstOrNull { it.deviceId == nextSetDeviceId }
    val sheetStationSeconds = sheetItem?.stationSeconds
    when {
        sheetItem != null && sheetStationSeconds != null -> StationSheet(
            item = sheetItem,
            stationSeconds = sheetStationSeconds,
            onApply = { weightGrams, seconds ->
                viewModel.updateStation(sheetItem, weightGrams, seconds)
                nextSetDeviceId = null
            },
            onRemove = {
                nextSetDeviceId = null
                removeEntry(sheetItem)
            }.takeIf { !circuitActive },
            onDismiss = { nextSetDeviceId = null },
        )
        sheetItem is StrengthItem -> NextSetSheet(
            item = sheetItem,
            onApply = { weightGrams, sets, reps, durationSeconds ->
                viewModel.updateValues(sheetItem, weightGrams, sets, reps, durationSeconds)
                nextSetDeviceId = null
            },
            onRemove = {
                nextSetDeviceId = null
                removeEntry(sheetItem)
            },
            onDismiss = { nextSetDeviceId = null },
        )
        sheetItem is CardioItem -> NextSessionSheet(
            item = sheetItem,
            onApply = { seconds, distanceMeters ->
                viewModel.updateNextSession(sheetItem, seconds, distanceMeters)
                nextSetDeviceId = null
            },
            onRemove = {
                nextSetDeviceId = null
                removeEntry(sheetItem)
            },
            onDismiss = { nextSetDeviceId = null },
        )
    }

    if (showPlanSheet) {
        PlanSheet(
            planName = state.planName,
            circuitSettings = state.circuitSettings,
            restTimerSettings = state.restTimerSettings,
            onSave = { name, circuitSettings, restTimerSettings ->
                viewModel.savePlan(name, circuitSettings, restTimerSettings)
                showPlanSheet = false
            },
            onDelete = {
                showPlanSheet = false
                viewModel.deletePlan()
            },
            onDismiss = { showPlanSheet = false },
        )
    }

    if (showAddDialog) {
        val devices by viewModel.devices.collectAsStateWithLifecycle()
        AddExercisesDialog(
            devices = devices,
            inPlanDeviceIds = state.items.mapTo(HashSet()) { it.deviceId },
            onConfirm = { deviceIds ->
                viewModel.addDevices(deviceIds)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editingSession?.let { (number, session) ->
        EditSessionDialog(
            title = stringResource(R.string.edit_session_title, number),
            session = session,
            tracksDistance = (state.items.firstOrNull { it.deviceId == session.deviceId } as? CardioItem)?.tracksDistance == true,
            onConfirm = { seconds, distanceMeters ->
                viewModel.updateSession(session, seconds, distanceMeters)
                editingSession = null
            },
            onDelete = {
                viewModel.deleteSession(session)
                editingSession = null
            },
            onDismiss = { editingSession = null },
        )
    }

    editingSet?.let { (number, set) ->
        EditSetDialog(
            title = stringResource(R.string.edit_set_title, number),
            set = set,
            isBodyweight = (state.items.firstOrNull { it.deviceId == set.deviceId } as? StrengthItem)?.isBodyweight == true,
            onConfirm = { weightGrams, reps, durationSeconds ->
                viewModel.updateSet(set, weightGrams, reps, durationSeconds)
                editingSet = null
            },
            onDelete = {
                viewModel.deleteSet(set)
                editingSet = null
            },
            onDismiss = { editingSet = null },
        )
    }
}

@Composable
internal fun EditSetDialog(
    title: String,
    set: SetLog,
    onConfirm: (weightGrams: Int, reps: Int?, durationSeconds: Int?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    // Bodyweight sets have no weight to edit.
    isBodyweight: Boolean = false,
) {
    // Timed circuit sets have a duration, and their reps are optional.
    val isTimed = set.durationSeconds != null
    var weightInput by rememberSaveable { mutableStateOf(formatKg(set.weightGrams)) }
    var repsInput by rememberSaveable { mutableStateOf(set.reps?.toString().orEmpty()) }
    var durationInput by rememberSaveable { mutableStateOf(set.durationSeconds?.let(::formatDuration).orEmpty()) }
    val weightGrams = parseKgToGrams(weightInput)
    val reps = repsInput.trim().toIntOrNull()?.takeIf { it in 1..999 }
    val repsValid = reps != null || (isTimed && repsInput.isBlank())
    val durationSeconds = if (isTimed) parseDuration(durationInput) else null
    val valid = weightGrams != null && repsValid && (!isTimed || durationSeconds != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isBodyweight) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text(stringResource(R.string.weight_label_kg)) },
                        singleLine = true,
                        isError = weightGrams == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text(stringResource(if (isTimed) R.string.reps_optional_label else R.string.target_reps)) },
                    singleLine = true,
                    isError = !repsValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (isTimed) {
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it },
                        label = { Text(stringResource(R.string.duration_input_label)) },
                        singleLine = true,
                        isError = durationSeconds == null,
                        // Number and phone keyboards have no colon on many devices.
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { weightGrams?.let { onConfirm(it, reps, durationSeconds) } },
                enabled = valid,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

@Composable
private fun WorkoutCard(
    item: StrengthItem,
    onEditNextSet: () -> Unit,
    onCompleteDevice: () -> Unit,
    onCompleteOneSet: () -> Unit,
    onUndo: () -> Unit,
    onSetClick: (setNumber: Int, set: SetLog) -> Unit,
    highlighted: Boolean = false,
    // Circuit plans log only through the timer: no log buttons, and the next station only has a weight.
    isCircuitPlan: Boolean = false,
    // The next device to train: only its "Done" is the filled main action.
    isActive: Boolean = false,
) {
    val resources = LocalResources.current
    DeviceCard(
        deviceName = item.deviceName,
        type = if (item.isBodyweight) DeviceType.BODYWEIGHT else DeviceType.STRENGTH,
        isComplete = item.isComplete,
        done = item.todaySets.size,
        target = item.targetSets,
        summary = item.todaySets.joinToString("  ·  ") { resources.setSummary(it, item.isBodyweight) },
        isActive = isActive,
        highlighted = highlighted,
        // Nothing to undo before the first set.
        onUndo = onUndo.takeIf { item.todaySets.isNotEmpty() },
        undoDescription = stringResource(R.string.action_undo_set),
    ) {
        if (item.todaySets.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.todaySets.forEachIndexed { index, set ->
                    SuggestionChip(
                        onClick = { onSetClick(index + 1, set) },
                        label = { Text(resources.setSummary(set, item.isBodyweight)) },
                    )
                }
            }
        }

        // Values are only editable in the sheet, so a stray tap on the card can't change them.
        val stationSeconds = item.stationSeconds
        val (label, value, detail) = when {
            stationSeconds != null && item.isBodyweight -> Triple(
                stringResource(R.string.workout_station_time_label),
                stringResource(R.string.duration_exact, formatDuration(stationSeconds)),
                null,
            )
            stationSeconds != null -> Triple(
                stringResource(R.string.workout_next_station_label),
                stringResource(R.string.weight_kg, formatKg(item.weightGrams)),
                stringResource(R.string.duration_exact, formatDuration(stationSeconds)),
            )
            item.measure == BodyweightMeasure.TIME -> Triple(
                stringResource(R.string.workout_next_set_label),
                resources.bodyweightSetSummary(reps = null, durationSeconds = item.durationSeconds),
                null,
            )
            item.isBodyweight -> Triple(
                stringResource(R.string.workout_next_set_label),
                resources.bodyweightSetSummary(reps = item.reps, durationSeconds = null),
                null,
            )
            else -> Triple(
                stringResource(R.string.workout_next_set_label),
                stringResource(R.string.weight_kg, formatKg(item.weightGrams)),
                stringResource(R.string.workout_reps_detail, item.reps),
            )
        }
        NextValueRow(
            label = label,
            value = value,
            detail = detail,
            onClickLabel = stringResource(R.string.action_edit_next_set),
            onClick = onEditNextSet,
        )

        if (!isCircuitPlan) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onCompleteOneSet, modifier = Modifier.weight(1f)) {
                    val nextSet = item.todaySets.size + 1
                    Text(
                        text = if (nextSet <= item.targetSets) {
                            stringResource(R.string.action_set_n_of_m, nextSet, item.targetSets)
                        } else {
                            stringResource(R.string.action_extra_set)
                        },
                        maxLines = 1,
                    )
                }
                DoneButton(
                    onClick = onCompleteDevice,
                    prominent = isActive,
                    enabled = !item.isComplete,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// The device's values in this plan. Edits stay local until "Apply" saves them in the plan;
// dismissing the sheet discards them.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextSetSheet(
    item: StrengthItem,
    onApply: (weightGrams: Int, sets: Int, reps: Int, durationSeconds: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var weightGrams by rememberSaveable { mutableIntStateOf(item.weightGrams) }
    var sets by rememberSaveable { mutableIntStateOf(item.targetSets) }
    var reps by rememberSaveable { mutableIntStateOf(item.reps) }
    var durationSeconds by rememberSaveable { mutableIntStateOf(item.durationSeconds) }
    var showWeightDialog by rememberSaveable { mutableStateOf(false) }
    var showDurationDialog by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetTitle(item.deviceName, onRemove)
            if (!item.isBodyweight) {
                ValueStepper(
                    label = stringResource(R.string.weight_label),
                    value = stringResource(R.string.weight_kg, formatKg(weightGrams)),
                    onDecrease = { weightGrams = stepWeightDown(weightGrams, item.minWeightGrams, item.weightStepGrams) },
                    onIncrease = { weightGrams = stepWeightUp(weightGrams, item.minWeightGrams, item.weightStepGrams) },
                    decreaseEnabled = weightGrams > item.minWeightGrams,
                    onValueClick = { showWeightDialog = true },
                )
            }
            if (item.measure == BodyweightMeasure.TIME) {
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
                onDecrease = { sets = (sets - 1).coerceIn(TARGET_SETS_RANGE) },
                onIncrease = { sets = (sets + 1).coerceIn(TARGET_SETS_RANGE) },
                decreaseEnabled = sets > TARGET_SETS_RANGE.first,
            )
            Button(onClick = { onApply(weightGrams, sets, reps, durationSeconds) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_apply))
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

/**
 * Title of the value sheets. Removing from the plan is only a small red icon up here, away from
 * "Apply" at the bottom, so it is not hit by mistake; it can be undone from a snackbar.
 * No icon when [onRemove] is null.
 */
@Composable
internal fun SheetTitle(title: String, onRemove: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.action_remove_from_plan),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun ValueStepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean,
    onValueClick: (() -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(onClick = onDecrease, enabled = decreaseEnabled, modifier = Modifier.size(56.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = stringResource(R.string.action_decrease),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 112.dp)
                .heightIn(min = 48.dp)
                .then(if (onValueClick != null) Modifier.clickable(onClick = onValueClick) else Modifier)
                .wrapContentHeight(),
        )
        FilledTonalIconButton(onClick = onIncrease, modifier = Modifier.size(56.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.action_increase),
            )
        }
    }
}

@Composable
internal fun WeightDialog(
    initialWeightGrams: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(formatKg(initialWeightGrams)) }
    val parsed = parseKgToGrams(input)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weight_dialog_title)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.weight_label_kg)) },
                singleLine = true,
                isError = parsed == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
