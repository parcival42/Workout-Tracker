package io.github.parcival42.workouttracker.ui.workout

import io.github.parcival42.workouttracker.data.RestTimerSettings

enum class RestKind { SET, EXERCISE }

/**
 * The rest after logging [loggedSets] sets of [item] (cardio: one session); [item] and [items]
 * are the state before the log. Sets left on this exercise get the set rest; a finished
 * exercise or an extra set (usually the last one) the exercise rest; a finished plan no rest.
 */
fun chooseRest(item: WorkoutItem, loggedSets: Int, items: List<WorkoutItem>): RestKind? {
    if (item is StrengthItem && item.openSets - loggedSets > 0) return RestKind.SET
    val otherOpen = items.any { it.entryId != item.entryId && !it.isComplete }
    return if (otherOpen) RestKind.EXERCISE else null
}

fun RestTimerSettings.secondsFor(kind: RestKind): Int = when (kind) {
    RestKind.SET -> setSeconds
    RestKind.EXERCISE -> if (exerciseEnabled) exerciseSeconds else setSeconds
}
