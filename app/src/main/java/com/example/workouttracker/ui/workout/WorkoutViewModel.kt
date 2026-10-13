package com.example.workouttracker.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApplication
import com.example.workouttracker.data.PlanDao
import com.example.workouttracker.data.SetLog
import com.example.workouttracker.data.SetLogDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class WorkoutItem(
    val deviceId: Long,
    val deviceName: String,
    val targetSets: Int,
    val targetReps: Int,
    val todaySets: List<SetLog>,
    val weightGrams: Int,
    val minWeightGrams: Int,
    val weightStepGrams: Int,
    val reps: Int,
) {
    val openSets: Int get() = (targetSets - todaySets.size).coerceAtLeast(0)
    val isComplete: Boolean get() = openSets == 0
}

data class WorkoutUiState(
    val planName: String = "",
    val planExists: Boolean = true,
    val items: List<WorkoutItem> = emptyList(),
)

/** Unsaved weight/reps edits; null fields fall back to the device's last logged set. */
private data class Draft(val weightGrams: Int? = null, val reps: Int? = null)

class WorkoutViewModel(
    private val planId: Long,
    planDao: PlanDao,
    private val setLogDao: SetLogDao,
) : ViewModel() {

    // The set counter resets per calendar day; "today" is fixed when the screen opens.
    private val startOfToday: Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val drafts = MutableStateFlow<Map<Long, Draft>>(emptyMap())

    val uiState: StateFlow<WorkoutUiState> = combine(
        planDao.observePlan(planId),
        planDao.observeEntries(planId),
        setLogDao.observeSince(startOfToday),
        setLogDao.observeLastSetPerDevice(),
        drafts,
    ) { plan, entries, todaySets, lastSets, drafts ->
        val todayByDevice = todaySets.groupBy { it.deviceId }
        val lastByDevice = lastSets.associateBy { it.deviceId }
        WorkoutUiState(
            planName = plan?.name.orEmpty(),
            planExists = plan != null,
            items = entries.map { entry ->
                val draft = drafts[entry.deviceId]
                val last = lastByDevice[entry.deviceId]
                WorkoutItem(
                    deviceId = entry.deviceId,
                    deviceName = entry.deviceName,
                    targetSets = entry.targetSets,
                    targetReps = entry.targetReps,
                    todaySets = todayByDevice[entry.deviceId].orEmpty(),
                    weightGrams = draft?.weightGrams ?: last?.weightGrams ?: entry.minWeightGrams,
                    minWeightGrams = entry.minWeightGrams,
                    weightStepGrams = entry.weightStepGrams,
                    reps = draft?.reps ?: last?.reps ?: entry.targetReps,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    fun setWeight(deviceId: Long, weightGrams: Int) {
        val clamped = weightGrams.coerceIn(0, MAX_WEIGHT_GRAMS)
        drafts.update { it + (deviceId to (it[deviceId] ?: Draft()).copy(weightGrams = clamped)) }
    }

    fun setReps(deviceId: Long, reps: Int) {
        val clamped = reps.coerceIn(1, MAX_REPS)
        drafts.update { it + (deviceId to (it[deviceId] ?: Draft()).copy(reps = clamped)) }
    }

    fun completeDevice(item: WorkoutItem) = logSets(item, item.openSets)

    fun completeOneSet(item: WorkoutItem) = logSets(item, 1)

    fun undoLastSet(item: WorkoutItem) {
        // Pin the displayed values so removing the set does not change the selected weight.
        drafts.update { it + (item.deviceId to Draft(weightGrams = item.weightGrams, reps = item.reps)) }
        viewModelScope.launch { setLogDao.deleteLastSince(item.deviceId, startOfToday) }
    }

    fun updateSet(set: SetLog, weightGrams: Int, reps: Int) {
        val updated = set.copy(
            weightGrams = weightGrams.coerceIn(0, MAX_WEIGHT_GRAMS),
            reps = reps.coerceIn(1, MAX_REPS),
        )
        viewModelScope.launch { setLogDao.update(updated) }
    }

    fun deleteSet(set: SetLog) {
        viewModelScope.launch { setLogDao.delete(set) }
    }

    private fun logSets(item: WorkoutItem, count: Int) {
        if (count <= 0) return
        val now = System.currentTimeMillis()
        // Offset by 1 ms per set so sets logged in one tap keep a stable order.
        val sets = List(count) { index ->
            SetLog(
                deviceId = item.deviceId,
                performedAt = now + index,
                weightGrams = item.weightGrams,
                reps = item.reps,
            )
        }
        viewModelScope.launch {
            setLogDao.insertAll(sets)
            // The logged values are now the device's last set, so the draft is no longer needed.
            drafts.update { it - item.deviceId }
        }
    }

    companion object {
        const val PLAN_ID_ARG = "planId"
        private const val MAX_WEIGHT_GRAMS = 1_000_000
        private const val MAX_REPS = 999

        fun factory(planId: Long) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkoutTrackerApplication
                WorkoutViewModel(planId, app.database.planDao(), app.database.setLogDao())
            }
        }
    }
}
