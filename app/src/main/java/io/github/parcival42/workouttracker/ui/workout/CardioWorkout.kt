package io.github.parcival42.workouttracker.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.ui.components.DurationDialog
import io.github.parcival42.workouttracker.ui.components.MAX_DURATION_MINUTES
import io.github.parcival42.workouttracker.ui.components.cardioSummary
import io.github.parcival42.workouttracker.ui.components.formatDuration
import io.github.parcival42.workouttracker.ui.components.formatKm
import io.github.parcival42.workouttracker.ui.components.parseDuration
import io.github.parcival42.workouttracker.ui.components.parseKmToMeters
import kotlinx.coroutines.delay
import io.github.parcival42.workouttracker.ui.components.SECONDS_PER_MINUTE
import io.github.parcival42.workouttracker.ui.components.stepDurationDown
import io.github.parcival42.workouttracker.ui.components.stepDurationUp

@Composable
internal fun CardioCard(
    item: CardioItem,
    onEditNextSession: () -> Unit,
    onLogSession: () -> Unit,
    onUndo: () -> Unit,
    onSessionClick: (sessionNumber: Int, session: CardioLog) -> Unit,
    highlighted: Boolean = false,
    // Circuit plans log only through the timer, with the station time: the row shows that time, no log buttons.
    isCircuitPlan: Boolean = false,
) {
    val resources = LocalResources.current
    DeviceCard(
        deviceName = item.deviceName,
        isComplete = item.isComplete,
        // Several sessions are only planned in a circuit, one per round.
        progress = if (item.targetSessions > 1) {
            stringResource(R.string.workout_progress, item.todaySessions.size, item.targetSessions)
        } else {
            null
        },
        highlighted = highlighted,
        summary = item.todaySessions.joinToString("  ·  ") { resources.cardioSummary(it) },
    ) {
        if (item.todaySessions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.todaySessions.forEachIndexed { index, session ->
                    SuggestionChip(
                        onClick = { onSessionClick(index + 1, session) },
                        label = { Text(resources.cardioSummary(session)) },
                    )
                }
            }
        }

        // The duration is only editable in the sheet, so a stray tap on the card can't change it.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(
                    onClickLabel = stringResource(R.string.action_edit_next_session),
                    onClick = onEditNextSession,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    item.stationSeconds != null ->
                        stringResource(R.string.workout_station_time, formatDuration(item.stationSeconds))
                    item.distanceMeters != null -> stringResource(
                        R.string.workout_next_session_distance,
                        formatDuration(item.durationSeconds),
                        formatKm(item.distanceMeters),
                    )
                    else -> stringResource(R.string.workout_next_session, formatDuration(item.durationSeconds))
                },
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
            IconButton(onClick = onUndo, enabled = item.todaySessions.isNotEmpty()) {
                Icon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = stringResource(R.string.action_undo_session),
                )
            }
            // Keeps the button in the same grid slot as "Done" on strength cards, which have
            // a second button here.
            Spacer(modifier = Modifier.weight(1f))
            // Once done, a further session stays possible but is no longer the prominent action.
            if (isCircuitPlan) {
                Spacer(modifier = Modifier.weight(1f))
            } else if (item.isComplete) {
                OutlinedButton(onClick = onLogSession, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_extra_session), maxLines = 1)
                }
            } else {
                Button(onClick = onLogSession, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_device_done))
                }
            }
        }
    }
}

// Edits stay local until "Apply"; dismissing the sheet discards them.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NextSessionSheet(
    item: CardioItem,
    onApply: (seconds: Int, distanceMeters: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var seconds by rememberSaveable { mutableIntStateOf(item.durationSeconds) }
    var distanceInput by rememberSaveable { mutableStateOf(item.distanceMeters?.let(::formatKm).orEmpty()) }
    var showDurationDialog by rememberSaveable { mutableStateOf(false) }
    // Empty means no distance; anything else must be a valid distance.
    val distanceMeters = parseKmToMeters(distanceInput)
    val distanceValid = distanceInput.isBlank() || distanceMeters != null

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
                    text = stringResource(
                        if (item.tracksDistance) R.string.next_session_sheet_subtitle_distance
                        else R.string.next_session_sheet_subtitle
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DurationStepper(seconds = seconds, onSecondsChange = { seconds = it }, onValueClick = { showDurationDialog = true })
            if (item.tracksDistance) {
                DistanceField(value = distanceInput, onValueChange = { distanceInput = it }, isError = !distanceValid)
            }
            Button(
                onClick = { onApply(seconds, distanceMeters) },
                enabled = distanceValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_apply))
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

/** Exact duration with +/- one minute; tapping the value opens exact input. */
@Composable
internal fun DurationStepper(seconds: Int, onSecondsChange: (Int) -> Unit, onValueClick: () -> Unit) {
    ValueStepper(
        label = stringResource(R.string.duration_label),
        value = stringResource(R.string.duration_exact, formatDuration(seconds)),
        onDecrease = { onSecondsChange(stepDurationDown(seconds)) },
        onIncrease = { onSecondsChange(stepDurationUp(seconds)) },
        decreaseEnabled = seconds > SECONDS_PER_MINUTE,
        onValueClick = onValueClick,
    )
}

/**
 * Optional distance in km; empty means no distance. Focusing the field selects its text, so a
 * prefilled value is replaced by typing instead of having to be deleted first.
 */
@Composable
internal fun DistanceField(value: String, onValueChange: (String) -> Unit, isError: Boolean, modifier: Modifier = Modifier) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }
    if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    // The tap that focuses the field places the cursor after the focus event, possibly in several
    // steps. For a short moment after focusing, cursor-only changes keep the text selected.
    var keepSelection by remember { mutableStateOf(false) }
    LaunchedEffect(keepSelection) {
        if (keepSelection) {
            delay(KEEP_SELECTION_MILLIS)
            keepSelection = false
        }
    }
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = field,
        onValueChange = { new ->
            if (keepSelection && new.text == field.text) return@OutlinedTextField
            keepSelection = false
            field = new
            if (new.text != value) onValueChange(new.text)
        },
        label = { Text(stringResource(R.string.distance_label)) },
        placeholder = { Text(stringResource(R.string.distance_placeholder)) },
        suffix = { Text(stringResource(R.string.unit_km)) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (state.isFocused && field.text.isNotEmpty()) {
                    field = field.copy(selection = TextRange(0, field.text.length))
                    keepSelection = true
                } else if (!state.isFocused) {
                    keepSelection = false
                }
            },
    )
}

private const val KEEP_SELECTION_MILLIS = 500L

/** Duration plus, if the device tracks distance or the session has one, an optional distance. */
@Composable
internal fun EditSessionDialog(
    title: String,
    session: CardioLog,
    onConfirm: (seconds: Int, distanceMeters: Int?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    tracksDistance: Boolean = false,
) {
    val showDistance = tracksDistance || session.distanceMeters != null
    var durationInput by rememberSaveable { mutableStateOf(formatDuration(session.durationSeconds)) }
    var distanceInput by rememberSaveable { mutableStateOf(session.distanceMeters?.let(::formatKm).orEmpty()) }
    val seconds = parseDuration(durationInput)
    val distanceMeters = parseKmToMeters(distanceInput)
    val distanceValid = distanceInput.isBlank() || distanceMeters != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = durationInput,
                    onValueChange = { durationInput = it },
                    label = { Text(stringResource(R.string.duration_input_label)) },
                    singleLine = true,
                    isError = seconds == null,
                    // Number and phone keyboards have no colon on many devices.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
                if (showDistance) {
                    DistanceField(value = distanceInput, onValueChange = { distanceInput = it }, isError = !distanceValid)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { seconds?.let { onConfirm(it, distanceMeters) } },
                enabled = seconds != null && distanceValid,
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
