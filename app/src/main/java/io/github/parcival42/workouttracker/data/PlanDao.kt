package io.github.parcival42.workouttracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlanDao {
    @Query("SELECT * FROM training_plan ORDER BY name COLLATE NOCASE")
    abstract fun observePlans(): Flow<List<TrainingPlan>>

    @Query("SELECT * FROM training_plan WHERE id = :planId")
    abstract fun observePlan(planId: Long): Flow<TrainingPlan?>

    @Insert
    abstract suspend fun insertPlan(plan: TrainingPlan): Long

    @Update
    abstract suspend fun updatePlan(plan: TrainingPlan)

    @Delete
    abstract suspend fun deletePlan(plan: TrainingPlan)

    // Sets exactly this plan as default and clears the flag on all others.
    @Query("UPDATE training_plan SET is_default = (id = :planId)")
    abstract suspend fun setDefault(planId: Long)

    @Query("UPDATE training_plan SET session_started_at = :startedAt WHERE id = :planId")
    abstract suspend fun setSessionStart(planId: Long, startedAt: Long?)

    @Query("UPDATE training_plan SET is_default = 0")
    abstract suspend fun clearDefault()

    @Query(
        """
        SELECT pe.id, pe.device_id AS deviceId, d.name AS deviceName, d.type AS deviceType, d.measure AS measure,
               pe.target_sets AS targetSets, pe.target_reps AS targetReps,
               pe.target_duration_seconds AS targetDurationSeconds, pe.station_seconds AS stationSeconds,
               d.start_weight_grams AS minWeightGrams, d.weight_step_grams AS weightStepGrams,
               d.tracks_distance AS tracksDistance, pe.weight_grams AS weightGrams
        FROM plan_entry pe
        JOIN device d ON d.id = pe.device_id
        WHERE pe.plan_id = :planId AND d.archived = 0
        ORDER BY pe.sort_order
        """
    )
    abstract fun observeEntries(planId: Long): Flow<List<PlanEntryItem>>

    // Which device was trained in which plan, over all time; logs without a plan are left out.
    @Query(
        """
        SELECT device_id AS deviceId, plan_id AS planId FROM set_log WHERE plan_id IS NOT NULL
        UNION
        SELECT device_id AS deviceId, plan_id AS planId FROM cardio_log WHERE plan_id IS NOT NULL
        """
    )
    abstract fun observeLoggedDevicePlans(): Flow<List<DevicePlan>>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM plan_entry WHERE plan_id = :planId")
    protected abstract suspend fun nextSortOrder(planId: Long): Int

    @Insert
    protected abstract suspend fun insertEntry(entry: PlanEntry): Long

    /** Appends [entries] in the given order; their ids, plan and sort orders are ignored. */
    @Transaction
    open suspend fun addEntries(planId: Long, entries: List<PlanEntry>) {
        val first = nextSortOrder(planId)
        entries.forEachIndexed { index, entry ->
            insertEntry(entry.copy(id = 0, planId = planId, sortOrder = first + index))
        }
    }

    @Query("SELECT * FROM plan_entry WHERE id = :entryId")
    abstract suspend fun getEntry(entryId: Long): PlanEntry?

    /** Undoes a removal: puts the entry back with its id and restores the order from before. */
    @Transaction
    open suspend fun restoreEntry(entry: PlanEntry, orderedEntryIds: List<Long>) {
        insertEntry(entry)
        reorderEntries(orderedEntryIds)
    }

    @Query(
        """
        UPDATE plan_entry SET weight_grams = :weightGrams, target_sets = :targetSets, target_reps = :targetReps,
            target_duration_seconds = :targetDurationSeconds
        WHERE id = :entryId
        """
    )
    abstract suspend fun updateValues(
        entryId: Long,
        weightGrams: Int,
        targetSets: Int,
        targetReps: Int,
        targetDurationSeconds: Int,
    )

    @Query("UPDATE plan_entry SET weight_grams = :weightGrams WHERE id = :entryId")
    abstract suspend fun updateWeight(entryId: Long, weightGrams: Int)

    @Query("UPDATE plan_entry SET target_duration_seconds = :durationSeconds WHERE id = :entryId")
    abstract suspend fun updateDuration(entryId: Long, durationSeconds: Int)

    /** Sets the values of every entry once from the logs; see [DERIVE_PLAN_ENTRY_VALUES_SQL]. */
    @Query(DERIVE_PLAN_ENTRY_VALUES_SQL)
    abstract suspend fun deriveEntryValuesFromLogs()

    @Query("UPDATE plan_entry SET station_seconds = :stationSeconds WHERE id = :entryId")
    abstract suspend fun updateStationSeconds(entryId: Long, stationSeconds: Int?)

    @Query("DELETE FROM plan_entry WHERE id = :entryId")
    abstract suspend fun deleteEntry(entryId: Long)

    @Query("UPDATE plan_entry SET sort_order = :sortOrder WHERE id = :entryId")
    protected abstract suspend fun setSortOrder(entryId: Long, sortOrder: Int)

    @Query("SELECT * FROM training_plan ORDER BY id")
    abstract suspend fun getAllPlans(): List<TrainingPlan>

    @Query("SELECT * FROM plan_entry ORDER BY id")
    abstract suspend fun getAllEntries(): List<PlanEntry>

    @Insert
    abstract suspend fun insertPlans(plans: List<TrainingPlan>)

    @Insert
    abstract suspend fun insertEntries(entries: List<PlanEntry>)

    @Query("DELETE FROM plan_entry")
    abstract suspend fun deleteAllEntries()

    @Query("DELETE FROM plan_entry WHERE device_id = :deviceId")
    abstract suspend fun deleteEntriesForDevice(deviceId: Long)

    @Query("DELETE FROM training_plan")
    abstract suspend fun deleteAllPlans()

    @Transaction
    open suspend fun reorderEntries(orderedEntryIds: List<Long>) {
        orderedEntryIds.forEachIndexed { index, entryId -> setSortOrder(entryId, index) }
    }
}
