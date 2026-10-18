package io.github.parcival42.workouttracker.data

// Stored by name; never rename the constants without a migration.
enum class DeviceType {
    STRENGTH,
    CARDIO,
    // Exercises without a machine or weight (push-ups, plank); see BodyweightMeasure.
    BODYWEIGHT,
}

/** What counts for a bodyweight exercise. Stored by name; never rename the constants without a migration. */
enum class BodyweightMeasure {
    REPS,
    TIME,
}
