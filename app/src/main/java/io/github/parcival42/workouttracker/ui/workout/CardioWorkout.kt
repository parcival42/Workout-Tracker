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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.ui.components.MAX_DURATION_MINUTES
import io.github.parcival42.workouttracker.ui.components.parseMinutes
import io.github.parcival42.workouttracker.ui.components.secondsToMinutes

@Composable
internal fun CardioCard(
    item: CardioItem,
    onEditNextSession: () -> Unit,
    onLogSession: () -> Unit,
    onUndo: () -> Unit,
    onSessionClick: (sessionNumber: Int, session: CardioLog) -> Unit,
) {
    val resources = LocalResources.current
    DeviceCard(
        deviceName = item.deviceName,
        isComplete = item.isComplete,
        progress = null,
        summary = item.todaySessions.joinToString("  ·  ") {
            resources.getString(R.string.duration_min, secondsToMinutes(it.durationSeconds))
        },
    ) {
        if (item.todaySessions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.todaySessions.forEachIndexed { index, session ->
                    SuggestionChip(
                        onClick = { onSessionClick(index + 1, session) },
                        label = {
                            Text(stringResource(R.string.duration_min, secondsToMinutes(session.durationSeconds)))
                        },
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
                text = stringResource(R.string.workout_next_session, item.minutes),
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
            if (item.isComplete) {
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
    onApply: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var minutes by rememberSaveable { mutableIntStateOf(item.minutes) }
    var showMinutesDialog by rememberSaveable { mutableStateOf(false) }

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
                    text = stringResource(R.string.next_session_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ValueStepper(
                label = stringResource(R.string.duration_label),
                value = stringResource(R.string.duration_min, minutes),
                onDecrease = { minutes = (minutes - 1).coerceAtLeast(1) },
                onIncrease = { minutes = (minutes + 1).coerceAtMost(MAX_DURATION_MINUTES) },
                decreaseEnabled = minutes > 1,
                onValueClick = { showMinutesDialog = true },
            )
            Button(onClick = { onApply(minutes) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_apply))
            }
        }
    }

    if (showMinutesDialog) {
        MinutesDialog(
            title = stringResource(R.string.duration_dialog_title),
            initialMinutes = minutes,
            onConfirm = {
                minutes = it
                showMinutesDialog = false
            },
            onDismiss = { showMinutesDialog = false },
        )
    }
}

@Composable
internal fun EditSessionDialog(
    title: String,
    session: CardioLog,
    onConfirm: (minutes: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    MinutesDialog(
        title = title,
        initialMinutes = secondsToMinutes(session.durationSeconds),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onDelete = onDelete,
    )
}

@Composable
internal fun MinutesDialog(
    title: String,
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var input by rememberSaveable { mutableStateOf(initialMinutes.toString()) }
    val parsed = parseMinutes(input)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.duration_label_min)) },
                singleLine = true,
                isError = parsed == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
