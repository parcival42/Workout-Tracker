package io.github.parcival42.workouttracker.ui.components

// Durations are stored in seconds; the UI works in whole minutes.

const val SECONDS_PER_MINUTE = 60
const val MAX_DURATION_MINUTES = 600

/** Rounds to the nearest whole minute, at least 1. */
fun secondsToMinutes(seconds: Int): Int = ((seconds + SECONDS_PER_MINUTE / 2) / SECONDS_PER_MINUTE).coerceAtLeast(1)

/** Parses whole minutes from 1 to [MAX_DURATION_MINUTES]; null if invalid. */
fun parseMinutes(input: String): Int? = input.trim().toIntOrNull()?.takeIf { it in 1..MAX_DURATION_MINUTES }
