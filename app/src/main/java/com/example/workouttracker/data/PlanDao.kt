package com.example.workouttracker.data

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

    @Query("UPDATE training_plan SET is_default = 0")
    abstract suspend fun clearDefault()

    @Query(
        """
        SELECT pe.id, pe.device_id AS deviceId, d.name AS deviceName,
               pe.target_sets AS targetSets, pe.target_reps AS targetReps,
               d.start_weight_grams AS minWeightGrams, d.weight_step_grams AS weightStepGrams
        FROM plan_entry pe
        JOIN device d ON d.id = pe.device_id
        WHERE pe.plan_id = :planId AND d.archived = 0
        ORDER BY pe.sort_order
        """
    )
    abstract fun observeEntries(planId: Long): Flow<List<PlanEntryItem>>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM plan_entry WHERE plan_id = :planId")
    protected abstract suspend fun nextSortOrder(planId: Long): Int

    @Insert
    protected abstract suspend fun insertEntry(entry: PlanEntry): Long

    @Transaction
    open suspend fun addEntry(planId: Long, deviceId: Long, targetSets: Int, targetReps: Int) {
        insertEntry(
            PlanEntry(
                planId = planId,
                deviceId = deviceId,
                sortOrder = nextSortOrder(planId),
                targetSets = targetSets,
                targetReps = targetReps,
            )
        )
    }

    @Query("UPDATE plan_entry SET target_sets = :targetSets, target_reps = :targetReps WHERE id = :entryId")
    abstract suspend fun updateTargets(entryId: Long, targetSets: Int, targetReps: Int)

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

    @Query("DELETE FROM training_plan")
    abstract suspend fun deleteAllPlans()

    @Transaction
    open suspend fun reorderEntries(orderedEntryIds: List<Long>) {
        orderedEntryIds.forEachIndexed { index, entryId -> setSortOrder(entryId, index) }
    }
}
