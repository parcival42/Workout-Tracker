package io.github.parcival42.workouttracker.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.ui.components.StatTile
import io.github.parcival42.workouttracker.ui.components.formatDuration
import java.text.NumberFormat

private const val GRAMS_PER_KG = 1_000L

/** Slim bar under the top bar: how many devices of the plan are done. */
@Composable
internal fun PlanProgress(done: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.workout_plan_progress, done, total),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
    }
}

// No training duration: logging after the fact (after a few devices or the whole workout) is
// intended, so the log timestamps say nothing about how long the training took.
private data class PlanTotals(val sets: Int, val volumeKg: Long, val cardioSeconds: Int)

private fun totalsOf(items: List<WorkoutItem>): PlanTotals {
    val sets = items.filterIsInstance<StrengthItem>().flatMap { item -> item.todaySets.map { item to it } }
    val sessions = items.filterIsInstance<CardioItem>().flatMap { it.todaySessions }
    // Volume only counts sets with a weight and reps; bodyweight and timed sets have no volume.
    val volumeGrams = sets.sumOf { (item, set) -> if (item.isBodyweight) 0L else set.weightGrams.toLong() * (set.reps ?: 0) }
    return PlanTotals(
        sets = sets.size,
        volumeKg = (volumeGrams + GRAMS_PER_KG / 2) / GRAMS_PER_KG,
        cardioSeconds = sessions.sumOf { it.durationSeconds },
    )
}

/** Shown once every device of the plan is done: what was trained, plus restarting the plan. */
@Composable
internal fun PlanCompleteCard(items: List<WorkoutItem>, onRestart: () -> Unit) {
    val totals = remember(items) { totalsOf(items) }
    // Pops in once when the plan gets completed; Compose honors the system animation scale.
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(visibleState = visibleState, enter = fadeIn() + scaleIn(initialScale = 0.9f)) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = stringResource(R.string.workout_plan_complete),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .semantics { heading() },
                    )
                }
                val tiles = buildList {
                    if (totals.sets > 0) add(stringResource(R.string.target_sets) to totals.sets.toString())
                    if (totals.volumeKg > 0) {
                        add(
                            stringResource(R.string.workout_summary_volume) to
                                stringResource(R.string.weight_kg, NumberFormat.getIntegerInstance().format(totals.volumeKg))
                        )
                    }
                    if (totals.cardioSeconds > 0) {
                        add(
                            stringResource(R.string.device_type_cardio) to
                                stringResource(R.string.duration_exact, formatDuration(totals.cardioSeconds))
                        )
                    }
                }
                tiles.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (label, value) ->
                            StatTile(label = label, value = value, modifier = Modifier.weight(1f))
                        }
                        // Keeps a lone last tile at half width.
                        if (row.size == 1) Column(modifier = Modifier.weight(1f)) {}
                    }
                }
                OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
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
