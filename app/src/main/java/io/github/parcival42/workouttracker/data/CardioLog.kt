package io.github.parcival42.workouttracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cardio_log",
    foreignKeys = [
        ForeignKey(
            entity = Device::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
        ),
    ],
    indices = [Index("device_id"), Index("performed_at")],
)
data class CardioLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "device_id") val deviceId: Long,
    @ColumnInfo(name = "performed_at") val performedAt: Long,
    // Seconds, although the UI works in whole minutes, so finer input needs no migration.
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int,
)
