package io.github.parcival42.workouttracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_log",
    foreignKeys = [
        ForeignKey(
            entity = Device::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
        ),
        ForeignKey(
            entity = TrainingPlan::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("device_id"), Index("performed_at"), Index("plan_id")],
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "device_id") val deviceId: Long,
    @ColumnInfo(name = "performed_at") val performedAt: Long,
    // Stored as integer grams to avoid floating point rounding.
    @ColumnInfo(name = "weight_grams") val weightGrams: Int,
    val reps: Int,
    // Plan the set was logged in; null for sets without a known plan (deleted plan, old data).
    @ColumnInfo(name = "plan_id") val planId: Long? = null,
)
