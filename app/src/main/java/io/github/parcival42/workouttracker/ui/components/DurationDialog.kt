package io.github.parcival42.workouttracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.parcival42.workouttracker.R

/**
 * Exact duration input in seconds. By default a plain number means minutes and "m:ss" exact
 * seconds (see [parseDuration]); with [secondsOnly] the input is plain seconds.
 */
@Composable
fun DurationDialog(
    title: String,
    initialSeconds: Int,
    onConfirm: (seconds: Int) -> Unit,
    onDismiss: () -> Unit,
    range: IntRange = 1..MAX_DURATION_SECONDS,
    secondsOnly: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    var input by rememberSaveable {
        mutableStateOf(if (secondsOnly) initialSeconds.toString() else formatDuration(initialSeconds))
    }
    val parsed = if (secondsOnly) {
        input.trim().toIntOrNull()?.takeIf { it in range }
    } else {
        parseDuration(input, range)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = {
                    Text(stringResource(if (secondsOnly) R.string.seconds_input_label else R.string.duration_input_label))
                },
                singleLine = true,
                isError = parsed == null,
                // Number and phone keyboards have no colon on many devices.
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (secondsOnly) KeyboardType.Number else KeyboardType.Text,
                ),
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
