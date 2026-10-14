package com.example.workouttracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM device WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<Device>>

    @Query("SELECT * FROM device WHERE archived = 1 ORDER BY name COLLATE NOCASE")
    fun observeArchived(): Flow<List<Device>>

    @Query("UPDATE device SET archived = :archived WHERE id = :deviceId")
    suspend fun setArchived(deviceId: Long, archived: Boolean)

    // Includes archived devices: their history stays viewable.
    @Query(
        """
        SELECT * FROM device d
        WHERE EXISTS (SELECT 1 FROM set_log s WHERE s.device_id = d.id)
           OR EXISTS (SELECT 1 FROM cardio_log c WHERE c.device_id = d.id)
        ORDER BY d.name COLLATE NOCASE
        """
    )
    fun observeWithHistory(): Flow<List<Device>>

    @Query("SELECT device_id FROM set_log UNION SELECT device_id FROM cardio_log")
    fun observeIdsWithLogs(): Flow<List<Long>>

    @Insert
    suspend fun insert(device: Device): Long

    @Update
    suspend fun update(device: Device)

    @Query("SELECT * FROM device ORDER BY id")
    suspend fun getAll(): List<Device>

    @Insert
    suspend fun insertAll(devices: List<Device>)

    @Query("DELETE FROM device")
    suspend fun deleteAll()
}
