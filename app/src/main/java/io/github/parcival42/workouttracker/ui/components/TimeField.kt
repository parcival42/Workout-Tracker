package io.github.parcival42.workouttracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R

/** A time value that opens a [DurationDialog] for direct input when tapped. */
@Composable
fun TimeField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    secondsOnly: Boolean = false,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = { showDialog = true }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Text(
            text = if (secondsOnly) {
                stringResource(R.string.duration_seconds, value)
            } else {
                stringResource(R.string.duration_exact, formatDuration(value))
            },
            style = MaterialTheme.typography.titleLarge,
        )
        Icon(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(20.dp),
        )
    }

    if (showDialog) {
        DurationDialog(
            title = label,
            initialSeconds = value,
            onConfirm = {
                onValueChange(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            range = range,
            secondsOnly = secondsOnly,
        )
    }
}
