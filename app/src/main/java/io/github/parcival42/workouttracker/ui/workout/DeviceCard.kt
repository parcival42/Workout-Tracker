package io.github.parcival42.workouttracker.ui.workout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R

private const val COMPLETED_CARD_ALPHA = 0.5f

/**
 * Card frame shared by strength and cardio devices. A completed device is greyed out and
 * collapses to its header plus [summary]; tapping expands it again (still greyed out) so
 * logged entries stay editable. It stays expanded until the user collapses it.
 */
@Composable
internal fun DeviceCard(
    deviceName: String,
    isComplete: Boolean,
    progress: String?,
    summary: String,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // Collapse again on the next completion if the device was reopened in between (e.g. undo).
    LaunchedEffect(isComplete) { if (!isComplete) expanded = false }
    val collapsed = isComplete && !expanded

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isComplete) Modifier.alpha(COMPLETED_CARD_ALPHA) else Modifier)
            .then(
                if (highlighted) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CardDefaults.elevatedShape)
                } else {
                    Modifier
                }
            )
            .animateContentSize(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (collapsed) 8.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (collapsed) 4.dp else 12.dp),
        ) {
            val toggleLabel = stringResource(if (expanded) R.string.action_hide_details else R.string.action_show_details)
            Row(
                modifier = if (isComplete) {
                    Modifier
                        .heightIn(min = 48.dp)
                        .clickable(onClickLabel = toggleLabel) { expanded = !expanded }
                } else {
                    Modifier
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = deviceName,
                    style = if (collapsed) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (isComplete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                if (progress != null) {
                    Text(text = progress, style = MaterialTheme.typography.titleMedium)
                }
                if (isComplete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .rotate(if (expanded) 180f else 0f),
                    )
                }
            }
            if (collapsed) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                content()
            }
        }
    }
}
