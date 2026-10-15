package io.github.parcival42.workouttracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table (id is always [ID]); a missing row means all defaults. */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = ID,
    // Prefill for a device newly added to a plan; never applied to existing plan entries.
    @ColumnInfo(name = "default_target_sets") val defaultTargetSets: Int = DEFAULT_TARGET_SETS,
    @ColumnInfo(name = "default_target_reps") val defaultTargetReps: Int = DEFAULT_TARGET_REPS,
    @ColumnInfo(name = "default_target_duration_seconds")
    val defaultTargetDurationSeconds: Int = PlanEntry.DEFAULT_TARGET_DURATION_SECONDS,
) {
    companion object {
        const val ID = 1
        const val DEFAULT_TARGET_SETS = 3
        const val DEFAULT_TARGET_REPS = 15
    }
}
