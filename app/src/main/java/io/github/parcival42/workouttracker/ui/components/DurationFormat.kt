package io.github.parcival42.workouttracker.ui.components

// Durations are stored in seconds. Plan targets work in whole minutes; logged durations are
// shown exactly (whole minutes as "20", otherwise "2:30").

const val SECONDS_PER_MINUTE = 60
const val MAX_DURATION_MINUTES = 600
const val MAX_DURATION_SECONDS = MAX_DURATION_MINUTES * SECONDS_PER_MINUTE

/** Rounds to the nearest whole minute, at least 1. */
fun secondsToMinutes(seconds: Int): Int = ((seconds + SECONDS_PER_MINUTE / 2) / SECONDS_PER_MINUTE).coerceAtLeast(1)

/** One minute less, but not below one minute; a shorter duration stays as it is. */
fun stepDurationDown(seconds: Int): Int = if (seconds <= SECONDS_PER_MINUTE) seconds else (seconds - SECONDS_PER_MINUTE).coerceAtLeast(SECONDS_PER_MINUTE)

fun stepDurationUp(seconds: Int): Int = (seconds + SECONDS_PER_MINUTE).coerceAtMost(MAX_DURATION_SECONDS)

/** Formats seconds as "m:ss", e.g. 90 -> "1:30". */
fun formatMinutesSeconds(seconds: Int): String = "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)

/** Exact duration without unit: whole minutes as "20", anything else as "2:30" (shown with "min"). */
fun formatDuration(seconds: Int): String =
    if (seconds % SECONDS_PER_MINUTE == 0) (seconds / SECONDS_PER_MINUTE).toString() else formatMinutesSeconds(seconds)

/**
 * Parses a duration into seconds: a plain number means minutes ("20"), "m:ss" gives exact
 * seconds ("2:30", "0:45"). Null if malformed or outside [range].
 */
fun parseDuration(input: String, range: IntRange = 1..MAX_DURATION_SECONDS): Int? {
    val parts = input.trim().split(':')
    val seconds = when (parts.size) {
        1 -> parts[0].toIntOrNull()?.takeIf { it >= 0 }?.let { it * SECONDS_PER_MINUTE }
        2 -> {
            val minutes = parts[0].toIntOrNull()?.takeIf { it >= 0 }
            val rest = parts[1].takeIf { it.length == 2 }?.toIntOrNull()?.takeIf { it in 0 until SECONDS_PER_MINUTE }
            if (minutes != null && rest != null) minutes * SECONDS_PER_MINUTE + rest else null
        }
        else -> null
    }
    return seconds?.takeIf { it in range }
}
