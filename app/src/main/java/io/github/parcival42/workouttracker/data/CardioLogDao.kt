package io.github.parcival42.workouttracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardioLogDao {
    @Query("SELECT * FROM cardio_log WHERE performed_at >= :since ORDER BY performed_at, id")
    fun observeSince(since: Long): Flow<List<CardioLog>>

    @Query("SELECT * FROM cardio_log WHERE performed_at >= :since AND plan_id = :planId ORDER BY performed_at, id")
    fun observeSinceInPlan(since: Long, planId: Long): Flow<List<CardioLog>>

    // Ordered by time, not id: imported data does not guarantee ids in chronological order.
    @Query(
        """
        SELECT * FROM cardio_log c WHERE c.id = (
            SELECT c2.id FROM cardio_log c2 WHERE c2.device_id = c.device_id
            ORDER BY c2.performed_at DESC, c2.id DESC LIMIT 1
        )
        """
    )
    fun observeLastPerDevice(): Flow<List<CardioLog>>

    @Query(
        """
        SELECT * FROM cardio_log c WHERE c.plan_id = :planId AND c.id = (
            SELECT c2.id FROM cardio_log c2 WHERE c2.device_id = c.device_id AND c2.plan_id = :planId
            ORDER BY c2.performed_at DESC, c2.id DESC LIMIT 1
        )
        """
    )
    fun observeLastPerDeviceInPlan(planId: Long): Flow<List<CardioLog>>

    // Last session with a distance, so a session without one (e.g. a circuit station) keeps the prefill.
    @Query(
        """
        SELECT * FROM cardio_log c WHERE c.id = (
            SELECT c2.id FROM cardio_log c2 WHERE c2.device_id = c.device_id AND c2.distance_meters IS NOT NULL
            ORDER BY c2.performed_at DESC, c2.id DESC LIMIT 1
        )
        """
    )
    fun observeLastDistancePerDevice(): Flow<List<CardioLog>>

    @Query(
        """
        SELECT * FROM cardio_log c WHERE c.plan_id = :planId AND c.id = (
            SELECT c2.id FROM cardio_log c2
            WHERE c2.device_id = c.device_id AND c2.plan_id = :planId AND c2.distance_meters IS NOT NULL
            ORDER BY c2.performed_at DESC, c2.id DESC LIMIT 1
        )
        """
    )
    fun observeLastDistancePerDeviceInPlan(planId: Long): Flow<List<CardioLog>>

    @Insert
    suspend fun insert(session: CardioLog): Long

    @Insert
    suspend fun insertAll(sessions: List<CardioLog>)

    @Update
    suspend fun update(session: CardioLog)

    @Delete
    suspend fun delete(session: CardioLog)

    @Query("DELETE FROM cardio_log WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Newest first; by time, not id (imported ids are not chronological).
    @Query("SELECT * FROM cardio_log ORDER BY performed_at DESC, id DESC")
    fun observeAll(): Flow<List<CardioLog>>

    @Query("SELECT * FROM cardio_log ORDER BY id")
    suspend fun getAll(): List<CardioLog>

    @Query("DELETE FROM cardio_log")
    suspend fun deleteAll()

    @Query("DELETE FROM cardio_log WHERE device_id = :deviceId")
    suspend fun deleteForDevice(deviceId: Long)

    @Query(
        """
        DELETE FROM cardio_log WHERE id = (
            SELECT id FROM cardio_log WHERE device_id = :deviceId AND plan_id = :planId AND performed_at >= :since
            ORDER BY performed_at DESC, id DESC LIMIT 1
        )
        """
    )
    suspend fun deleteLastSinceInPlan(deviceId: Long, planId: Long, since: Long)
}
