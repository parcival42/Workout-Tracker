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
    // Cardio: session duration. Bodyweight measured by time: duration per set (with target sets).
    // Sets/reps are used for strength and bodyweight devices.
    @ColumnInfo(name = "target_duration_seconds", defaultValue = "1200")
    val targetDurationSeconds: Int = DEFAULT_TARGET_DURATION_SECONDS,
    // Circuit plans only: own station duration; null uses the plan's station duration.
    @ColumnInfo(name = "station_seconds") val stationSeconds: Int? = null,
) {
    companion object {
        const val DEFAULT_TARGET_DURATION_SECONDS = 1_200
        // Prefill of a new bodyweight entry measured by time, e.g. a plank.
        const val DEFAULT_HOLD_SECONDS = 30
    }
}

data class PlanEntryItem(
    val id: Long,
    val deviceId: Long,
    val deviceName: String,
    val deviceType: DeviceType,
    val measure: BodyweightMeasure,
    val targetSets: Int,
    val targetReps: Int,
    val targetDurationSeconds: Int,
    val stationSeconds: Int?,
    val minWeightGrams: Int,
    val weightStepGrams: Int,
)

val PlanEntryItem.isTimedBodyweight: Boolean
    get() = deviceType == DeviceType.BODYWEIGHT && measure == BodyweightMeasure.TIME

/** A device that has logged training in a plan. */
data class DevicePlan(val deviceId: Long, val planId: Long)
