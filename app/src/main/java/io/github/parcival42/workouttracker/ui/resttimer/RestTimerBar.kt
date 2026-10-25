package io.github.parcival42.workouttracker.ui.resttimer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.ui.components.formatMinutesSeconds

/** Height of the slot the rest bar shares with the plan progress, so switching moves nothing. */
val REST_SLOT_HEIGHT = 80.dp

/**
 * Running rest as a bar under the top bar, in the slot of the plan progress (see
 * [REST_SLOT_HEIGHT]), so the cards stay where they are.
 */
@Composable
fun RestTimerBar(remainingSeconds: Int, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.rest_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(16.dp))
            Text(
                text = formatMinutesSeconds(remainingSeconds),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSkip) { Text(stringResource(R.string.action_skip)) }
        }
    }
}
