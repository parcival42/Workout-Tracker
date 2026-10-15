package io.github.parcival42.workouttracker.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.ui.components.EmptyHint
import io.github.parcival42.workouttracker.ui.components.formatKg
import io.github.parcival42.workouttracker.ui.components.parseKgToGrams
import io.github.parcival42.workouttracker.ui.components.stepWeightDown
import io.github.parcival42.workouttracker.ui.components.stepWeightUp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    planId: Long,
    onEditPlan: () -> Unit,
    onBack: (() -> Unit)? = null,
    onSwitchPlan: (() -> Unit)? = null,
    viewModel: WorkoutViewModel = viewModel(key = "workout-$planId", factory = WorkoutViewModel.factory(planId)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingSet by remember { mutableStateOf<Pair<Int, SetLog>?>(null) }
    var editingSession by remember { mutableStateOf<Pair<Int, CardioLog>?>(null) }
    var nextSetDeviceId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

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

    LaunchedEffect(state.planExists) {
        if (!state.planExists) onBack?.invoke()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.planName) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
                actions = {
                    if (onSwitchPlan != null) {
                        IconButton(onClick = onSwitchPlan) {
                            Icon(
                                painter = painterResource(R.drawable.ic_list),
                                contentDescription = stringResource(R.string.action_switch_plan),
                            )
                        }
                    }
                    if (state.items.isNotEmpty()) {
                        IconButton(onClick = restartPlan) {
                            Icon(
                                painter = painterResource(R.drawable.ic_restart),
                                contentDescription = stringResource(R.string.action_restart_plan),
                            )
                        }
                    }
                    IconButton(onClick = onEditPlan) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.action_edit_plan),
                        )
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
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items, key = { it.deviceId }) { item ->
                    when (item) {
                        is StrengthItem -> WorkoutCard(
                            item = item,
                            onEditNextSet = { nextSetDeviceId = item.deviceId },
                            onCompleteDevice = { viewModel.completeDevice(item) },
                            onCompleteOneSet = { viewModel.completeOneSet(item) },
                            onUndo = { viewModel.undoLastSet(item) },
                            onSetClick = { number, set -> editingSet = number to set },
                        )
                        is CardioItem -> CardioCard(
                            item = item,
                            onEditNextSession = { nextSetDeviceId = item.deviceId },
                            onLogSession = { viewModel.logSession(item) },
                            onUndo = { viewModel.undoLastSession(item) },
                            onSessionClick = { number, session -> editingSession = number to session },
                        )
                    }
                }
                if (state.isComplete) {
                    item(key = "plan-complete") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.workout_plan_complete),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            OutlinedButton(onClick = restartPlan) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_restart),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = stringResource(R.string.action_restart_plan),
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    when (val item = state.items.firstOrNull { it.deviceId == nextSetDeviceId }) {
        is StrengthItem -> NextSetSheet(
            item = item,
            onApply = { weightGrams, reps ->
                viewModel.setWeight(item.deviceId, weightGrams)
                viewModel.setReps(item.deviceId, reps)
                nextSetDeviceId = null
            },
            onDismiss = { nextSetDeviceId = null },
        )
        is CardioItem -> NextSessionSheet(
            item = item,
            onApply = { minutes ->
                viewModel.setMinutes(item.deviceId, minutes)
                nextSetDeviceId = null
            },
            onDismiss = { nextSetDeviceId = null },
        )
        null -> Unit
    }

    editingSession?.let { (number, session) ->
        EditSessionDialog(
            sessionNumber = number,
            session = session,
            onConfirm = { minutes ->
                viewModel.updateSession(session, minutes)
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
            setNumber = number,
            set = set,
            onConfirm = { weightGrams, reps ->
                viewModel.updateSet(set, weightGrams, reps)
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
private fun EditSetDialog(
    setNumber: Int,
    set: SetLog,
    onConfirm: (weightGrams: Int, reps: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var weightInput by rememberSaveable { mutableStateOf(formatKg(set.weightGrams)) }
    var repsInput by rememberSaveable { mutableStateOf(set.reps.toString()) }
    val weightGrams = parseKgToGrams(weightInput)
    val reps = repsInput.trim().toIntOrNull()?.takeIf { it in 1..999 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_set_title, setNumber)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text(stringResource(R.string.weight_label_kg)) },
                    singleLine = true,
                    isError = weightGrams == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text(stringResource(R.string.target_reps)) },
                    singleLine = true,
                    isError = reps == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (weightGrams != null && reps != null) onConfirm(weightGrams, reps) },
                enabled = weightGrams != null && reps != null,
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
) {
    val resources = LocalResources.current
    DeviceCard(
        deviceName = item.deviceName,
        isComplete = item.isComplete,
        progress = stringResource(R.string.workout_progress, item.todaySets.size, item.targetSets),
        summary = item.todaySets.joinToString("  ·  ") {
            resources.getString(R.string.workout_set_summary, formatKg(it.weightGrams), it.reps)
        },
    ) {
        if (item.todaySets.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.todaySets.forEachIndexed { index, set ->
                    SuggestionChip(
                        onClick = { onSetClick(index + 1, set) },
                        label = {
                            Text(stringResource(R.string.workout_set_summary, formatKg(set.weightGrams), set.reps))
                        },
                    )
                }
            }
        }

        // Values are only editable in the sheet, so a stray tap on the card can't change them.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(
                    onClickLabel = stringResource(R.string.action_edit_next_set),
                    onClick = onEditNextSet,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.workout_next_set, formatKg(item.weightGrams), item.reps),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onUndo, enabled = item.todaySets.isNotEmpty()) {
                Icon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = stringResource(R.string.action_undo_set),
                )
            }
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
            Button(
                onClick = onCompleteDevice,
                enabled = !item.isComplete,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_device_done))
            }
        }
    }
}

// Edits stay local until "Apply"; dismissing the sheet discards them.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextSetSheet(
    item: StrengthItem,
    onApply: (weightGrams: Int, reps: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var weightGrams by rememberSaveable { mutableIntStateOf(item.weightGrams) }
    var reps by rememberSaveable { mutableIntStateOf(item.reps) }
    var showWeightDialog by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(item.deviceName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.next_set_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ValueStepper(
                label = stringResource(R.string.weight_label),
                value = stringResource(R.string.weight_kg, formatKg(weightGrams)),
                onDecrease = { weightGrams = stepWeightDown(weightGrams, item.minWeightGrams, item.weightStepGrams) },
                onIncrease = { weightGrams = stepWeightUp(weightGrams, item.minWeightGrams, item.weightStepGrams) },
                decreaseEnabled = weightGrams > item.minWeightGrams,
                onValueClick = { showWeightDialog = true },
            )
            ValueStepper(
                label = stringResource(R.string.target_reps),
                value = reps.toString(),
                onDecrease = { reps = (reps - 1).coerceAtLeast(1) },
                onIncrease = { reps += 1 },
                decreaseEnabled = reps > 1,
            )
            Button(onClick = { onApply(weightGrams, reps) }, modifier = Modifier.fillMaxWidth()) {
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
private fun WeightDialog(
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
