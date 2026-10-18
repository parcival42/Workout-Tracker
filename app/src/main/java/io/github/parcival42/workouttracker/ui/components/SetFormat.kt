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

fun Resources.setSummary(set: SetLog): String = setSummary(set.weightGrams, set.reps, set.durationSeconds)
