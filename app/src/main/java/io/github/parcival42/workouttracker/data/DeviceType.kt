package io.github.parcival42.workouttracker.data

// Stored by name; never rename the constants without a migration.
enum class DeviceType {
    STRENGTH,
    CARDIO,
    // Exercises without a machine or weight (push-ups, plank); see BodyweightMeasure.
    BODYWEIGHT,
}

/**
 * Icon key a device of this type starts with: the type's own icon. Stored in the database and in
 * backups, so the keys never change (see DeviceIcons).
 */
val DeviceType.defaultIcon: String
    get() = when (this) {
        DeviceType.STRENGTH -> "strength"
        DeviceType.CARDIO -> "cardio"
        DeviceType.BODYWEIGHT -> "bodyweight"
    }

/** What counts for a bodyweight exercise. Stored by name; never rename the constants without a migration. */
enum class BodyweightMeasure {
    REPS,
    TIME,
}
