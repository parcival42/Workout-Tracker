package io.github.parcival42.workouttracker.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.ui.history.SERIES_COLOR_COUNT
import io.github.parcival42.workouttracker.ui.history.SeriesColors

/**
 * Picks a device's history color: "Automatic" (null) or a palette index. An index outside the
 * palette shows as automatic.
 */
@Composable
fun SeriesColorPicker(selected: Int?, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val fixed = selected?.takeIf { it in 0 until SERIES_COLOR_COUNT }
    Column(modifier = modifier) {
        Text(text = stringResource(R.string.device_color_label), style = MaterialTheme.typography.bodyLarge)
        FlowRow(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = fixed == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.device_color_auto)) },
            )
            SeriesColors.forEachIndexed { index, color ->
                ColorSwatch(
                    color = color,
                    selected = fixed == index,
                    description = stringResource(R.string.device_color_option, index + 1),
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
