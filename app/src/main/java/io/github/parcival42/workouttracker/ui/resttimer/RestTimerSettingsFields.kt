package io.github.parcival42.workouttracker.ui.resttimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.RestTimerSettings
import io.github.parcival42.workouttracker.ui.components.TimeField

val REST_SECONDS_RANGE = 5..3_600

/** Rest timer switch and times of a normal plan; the caller owns the edited state. */
@Composable
fun RestTimerSettingsFields(settings: RestTimerSettings, onSettingsChange: (RestTimerSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwitchRow(
            label = stringResource(R.string.rest_timer),
            checked = settings.enabled,
            onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) },
        )
        if (settings.enabled) {
            TimeField(
                label = stringResource(R.string.rest_set_time),
                value = settings.setSeconds,
                onValueChange = { onSettingsChange(settings.copy(setSeconds = it)) },
                range = REST_SECONDS_RANGE,
            )
            SwitchRow(
                label = stringResource(R.string.rest_exercise_own),
                checked = settings.exerciseEnabled,
                onCheckedChange = { onSettingsChange(settings.copy(exerciseEnabled = it)) },
            )
            if (settings.exerciseEnabled) {
                TimeField(
                    label = stringResource(R.string.rest_exercise_time),
                    value = settings.exerciseSeconds,
                    onValueChange = { onSettingsChange(settings.copy(exerciseSeconds = it)) },
                    range = REST_SECONDS_RANGE,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
