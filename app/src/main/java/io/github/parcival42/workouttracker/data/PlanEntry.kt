package io.github.parcival42.workouttracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plan_entry",
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlan::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Device::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
        ),
    ],
    indices = [Index("plan_id"), Index("device_id")],
)
data class PlanEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plan_id") val planId: Long,
    @ColumnInfo(name = "device_id") val deviceId: Long,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "target_sets") val targetSets: Int,
    @ColumnInfo(name = "target_reps") val targetReps: Int,
    // Only used for cardio devices; sets/reps are only used for strength devices.
    @ColumnInfo(name = "target_duration_seconds", defaultValue = "1200")
    val targetDurationSeconds: Int = DEFAULT_TARGET_DURATION_SECONDS,
) {
    companion object {
        const val DEFAULT_TARGET_DURATION_SECONDS = 1_200
    }
}

data class PlanEntryItem(
    val id: Long,
    val deviceId: Long,
    val deviceName: String,
    val deviceType: DeviceType,
    val targetSets: Int,
    val targetReps: Int,
    val targetDurationSeconds: Int,
    val minWeightGrams: Int,
    val weightStepGrams: Int,
)
