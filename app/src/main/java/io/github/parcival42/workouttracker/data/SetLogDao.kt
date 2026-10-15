package io.github.parcival42.workouttracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SetLogDao {
    @Query("SELECT * FROM set_log WHERE performed_at >= :since ORDER BY performed_at, id")
    fun observeSince(since: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_log WHERE performed_at >= :since AND plan_id = :planId ORDER BY performed_at, id")
    fun observeSinceInPlan(since: Long, planId: Long): Flow<List<SetLog>>

    // Ordered by time, not id: imported data does not guarantee ids in chronological order.
    @Query(
        """
        SELECT * FROM set_log s WHERE s.id = (
            SELECT s2.id FROM set_log s2 WHERE s2.device_id = s.device_id
            ORDER BY s2.performed_at DESC, s2.id DESC LIMIT 1
        )
        """
    )
    fun observeLastSetPerDevice(): Flow<List<SetLog>>

    @Query(
        """
        SELECT * FROM set_log s WHERE s.plan_id = :planId AND s.id = (
            SELECT s2.id FROM set_log s2 WHERE s2.device_id = s.device_id AND s2.plan_id = :planId
            ORDER BY s2.performed_at DESC, s2.id DESC LIMIT 1
        )
        """
    )
    fun observeLastSetPerDeviceInPlan(planId: Long): Flow<List<SetLog>>

    @Insert
    suspend fun insertAll(sets: List<SetLog>)

    @Update
    suspend fun update(set: SetLog)

    @Query("SELECT * FROM set_log ORDER BY id")
    suspend fun getAll(): List<SetLog>

    @Query("DELETE FROM set_log")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(set: SetLog)

    @Query(
        """
        DELETE FROM set_log WHERE id = (
            SELECT id FROM set_log WHERE device_id = :deviceId AND plan_id = :planId AND performed_at >= :since
            ORDER BY performed_at DESC, id DESC LIMIT 1
        )
        """
    )
    suspend fun deleteLastSinceInPlan(deviceId: Long, planId: Long, since: Long)
}
