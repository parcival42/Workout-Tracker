package io.github.parcival42.workouttracker.ui.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.ui.components.DeviceTypeBadge
import kotlinx.coroutines.delay

private const val COMPLETED_CARD_ALPHA = 0.5f

private const val COLLAPSE_DELAY_MILLIS = 400L

// Above this many sets the segments get too narrow; a plain bar is used instead.
private const val MAX_PROGRESS_SEGMENTS = 10

/**
 * Card frame shared by strength and cardio devices. A completed device is greyed out and
 * collapses to its header plus [summary]; tapping expands it again (still greyed out) so
 * logged entries stay editable. It stays expanded until the user collapses it.
 * [isActive] marks the next device to train: its card stands out a bit from the others.
 * [done]/[target] show the logged and planned sets (or sessions) as segments; no target, no segments.
 * [onUndo] (null = nothing to undo) puts the undo icon in the header next to the counter it
 * changes, away from "Done" in the bottom right corner, so it is not hit by mistake.
 */
@Composable
internal fun DeviceCard(
    deviceName: String,
    type: DeviceType,
    isComplete: Boolean,
    done: Int,
    target: Int?,
    summary: String,
    isActive: Boolean = false,
    highlighted: Boolean = false,
    onUndo: (() -> Unit)? = null,
    undoDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // A card that is already complete when shown starts collapsed; one completed right now waits.
    var collapseDue by remember { mutableStateOf(isComplete) }
    LaunchedEffect(isComplete) {
        if (isComplete) {
            // Collapsing moves the cards below up under the finger: a quick second tap would hit
            // the next card's "Done". Meanwhile it lands on this card, which ignores taps (the
            // cardio card shows "Extra session" right where "Done" was).
            if (!collapseDue) delay(COLLAPSE_DELAY_MILLIS)
        } else {
            // Collapse again on the next completion if the device was reopened in between (e.g. undo).
            expanded = false
        }
        collapseDue = isComplete
    }
    val collapsed = isComplete && collapseDue && !expanded
    val containerColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "cardContainer",
    )

    Card(
        // Explicit content color: the animated container color matches no scheme role, so it
        // could not be derived from it.
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isComplete) Modifier.alpha(COMPLETED_CARD_ALPHA) else Modifier)
            .then(
                if (highlighted) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CardDefaults.shape)
                } else {
                    Modifier
                }
            )
            .animateContentSize(),
    ) {
        Column(
            modifier = Modifier
                .then(if (isComplete && !collapseDue) Modifier.swallowTaps() else Modifier)
                .padding(horizontal = 16.dp, vertical = if (collapsed) 8.dp else 16.dp),
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
                DeviceTypeBadge(type = type, size = if (collapsed) 28.dp else 36.dp)
                Text(
                    text = deviceName,
                    style = if (collapsed) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                if (isComplete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                if (target != null) {
                    Text(
                        text = stringResource(R.string.workout_progress, done, target),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onUndo != null && !collapsed) {
                    IconButton(onClick = onUndo) {
                        Icon(painter = painterResource(R.drawable.ic_undo), contentDescription = undoDescription)
                    }
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
                if (target != null) SetProgress(done = done, target = target)
                content()
            }
        }
    }
}

/** Consumes every pointer event before the children see it, so nothing inside reacts. */
private fun Modifier.swallowTaps(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

/** One segment per planned set, filled once logged; extra sets keep all segments filled. */
@Composable
private fun SetProgress(done: Int, target: Int) {
    // The header already says "1/3"; the segments are only its picture.
    val modifier = Modifier
        .fillMaxWidth()
        .clearAndSetSemantics {}
    if (target > MAX_PROGRESS_SEGMENTS) {
        LinearProgressIndicator(
            progress = { (done.toFloat() / target).coerceAtMost(1f) },
            strokeCap = StrokeCap.Round,
            modifier = modifier.height(6.dp),
        )
        return
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(target) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        color = if (index < done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

/** "Done": filled on the active card only, tonal elsewhere, so one main action stands out. */
@Composable
internal fun DoneButton(onClick: () -> Unit, prominent: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    if (prominent) {
        Button(onClick = onClick, enabled = enabled, modifier = modifier) {
            Text(stringResource(R.string.action_device_done), maxLines = 1)
        }
    } else {
        FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier) {
            Text(stringResource(R.string.action_device_done), maxLines = 1)
        }
    }
}

/**
 * The prefilled values of the next set or session, large enough to read at the machine: [value]
 * big, [detail] smaller next to it. Tapping opens the sheet; values are never edited on the card.
 */
@Composable
internal fun NextValueRow(
    label: String,
    value: String,
    detail: String?,
    onClickLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = onClickLabel, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.alignByBaseline(),
                )
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .alignByBaseline()
                            .padding(start = 8.dp),
                    )
                }
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
