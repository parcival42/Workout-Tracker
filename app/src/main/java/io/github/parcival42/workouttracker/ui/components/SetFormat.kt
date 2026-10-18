package io.github.parcival42.workouttracker.ui.components

import android.content.res.Resources
import io.github.parcival42.workouttracker.R
import io.github.parcival42.workouttracker.data.SetLog

/** "17.5 kg × 10"; timed circuit sets show their duration: "17.5 kg · 2:00 min", "17.5 kg × 30 · 2 min". */
fun Resources.setSummary(weightGrams: Int, reps: Int?, durationSeconds: Int?): String = when {
    durationSeconds == null -> getString(R.string.workout_set_summary, formatKg(weightGrams), reps ?: 0)
    reps == null -> getString(R.string.set_summary_timed, formatKg(weightGrams), formatDuration(durationSeconds))
    else -> getString(R.string.set_summary_timed_reps, formatKg(weightGrams), reps, formatDuration(durationSeconds))
}

/** Bodyweight sets have no weight: "20 reps", "0:30 min" or "20 reps · 0:30 min". */
fun Resources.bodyweightSetSummary(reps: Int?, durationSeconds: Int?): String {
    val repsText = reps?.let { getQuantityString(R.plurals.reps_count, it, it) }
    val durationText = durationSeconds?.let { formatDuration(it) }
    return when {
        repsText != null && durationText != null -> getString(R.string.set_summary_bodyweight_both, repsText, durationText)
        durationText != null -> getString(R.string.duration_exact, durationText)
        else -> repsText.orEmpty()
    }
}

fun Resources.setSummary(set: SetLog, isBodyweight: Boolean = false): String =
    if (isBodyweight) {
        bodyweightSetSummary(set.reps, set.durationSeconds)
    } else {
        setSummary(set.weightGrams, set.reps, set.durationSeconds)
    }
