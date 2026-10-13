package com.example.workouttracker.data

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
    ],
    indices = [Index("device_id"), Index("performed_at")],
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "device_id") val deviceId: Long,
    @ColumnInfo(name = "performed_at") val performedAt: Long,
    // Stored as integer grams to avoid floating point rounding.
    @ColumnInfo(name = "weight_grams") val weightGrams: Int,
    val reps: Int,
)
