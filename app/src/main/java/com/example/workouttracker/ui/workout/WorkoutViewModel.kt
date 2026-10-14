package com.example.workouttracker.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.workouttracker.WorkoutTrackerApplication
import com.example.workouttracker.data.CardioLog
import com.example.workouttracker.data.CardioLogDao
import com.example.workouttracker.data.DeviceType
import com.example.workouttracker.data.PlanDao
import com.example.workouttracker.data.SetLog
import com.example.workouttracker.data.SetLogDao
import com.example.workouttracker.ui.components.MAX_DURATION_MINUTES
import com.example.workouttracker.ui.components.SECONDS_PER_MINUTE
import com.example.workouttracker.ui.components.secondsToMinutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

sealed interface WorkoutItem {
    val deviceId: Long
    val deviceName: String
    val isComplete: Boolean
}

data class StrengthItem(
    override val deviceId: Long,
    override val deviceName: String,
    val targetSets: Int,
    val targetReps: Int,
    val todaySets: List<SetLog>,
    val weightGrams: Int,
    val minWeightGrams: Int,
    val weightStepGrams: Int,
    val reps: Int,
) : WorkoutItem {
    val openSets: Int get() = (targetSets - todaySets.size).coerceAtLeast(0)
    override val isComplete: Boolean get() = openSets == 0
}

data class CardioItem(
    override val deviceId: Long,
    override val deviceName: String,
    val todaySessions: List<CardioLog>,
    val minutes: Int,
) : WorkoutItem {
    override val isComplete: Boolean get() = todaySessions.isNotEmpty()
}

data class WorkoutUiState(
    val planName: String = "",
    val planExists: Boolean = true,
    val items: List<WorkoutItem> = emptyList(),
)

/** Unsaved edits; null fields fall back to the device's last logged set or session. */
private data class Draft(val weightGrams: Int? = null, val reps: Int? = null, val minutes: Int? = null)

private data class LogsByDevice<T>(val today: Map<Long, List<T>>, val last: Map<Long, T>)

class WorkoutViewModel(
    private val planId: Long,
    planDao: PlanDao,
    private val setLogDao: SetLogDao,
    private val cardioLogDao: CardioLogDao,
) : ViewModel() {

    // The set counter resets per calendar day; "today" is fixed when the screen opens.
    private val startOfToday: Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val drafts = MutableStateFlow<Map<Long, Draft>>(emptyMap())

    private val strengthLogs = combine(
        setLogDao.observeSince(startOfToday),
        setLogDao.observeLastSetPerDevice(),
    ) { today, last -> LogsByDevice(today.groupBy { it.deviceId }, last.associateBy { it.deviceId }) }

    private val cardioLogs = combine(
        cardioLogDao.observeSince(startOfToday),
        cardioLogDao.observeLastPerDevice(),
    ) { today, last -> LogsByDevice(today.groupBy { it.deviceId }, last.associateBy { it.deviceId }) }

    val uiState: StateFlow<WorkoutUiState> = combine(
        planDao.observePlan(planId),
        planDao.observeEntries(planId),
        strengthLogs,
        cardioLogs,
        drafts,
    ) { plan, entries, strengthLogs, cardioLogs, drafts ->
        WorkoutUiState(
            planName = plan?.name.orEmpty(),
            planExists = plan != null,
            items = entries.map { entry ->
                val draft = drafts[entry.deviceId]
                when (entry.deviceType) {
                    DeviceType.STRENGTH -> {
                        val last = strengthLogs.last[entry.deviceId]
                        StrengthItem(
                            deviceId = entry.deviceId,
                            deviceName = entry.deviceName,
                            targetSets = entry.targetSets,
                            targetReps = entry.targetReps,
                            todaySets = strengthLogs.today[entry.deviceId].orEmpty(),
                            weightGrams = draft?.weightGrams ?: last?.weightGrams ?: entry.minWeightGrams,
                            minWeightGrams = entry.minWeightGrams,
                            weightStepGrams = entry.weightStepGrams,
                            reps = draft?.reps ?: last?.reps ?: entry.targetReps,
                        )
                    }
                    DeviceType.CARDIO -> CardioItem(
                        deviceId = entry.deviceId,
                        deviceName = entry.deviceName,
                        todaySessions = cardioLogs.today[entry.deviceId].orEmpty(),
                        minutes = draft?.minutes
                            ?: secondsToMinutes(
                                cardioLogs.last[entry.deviceId]?.durationSeconds ?: entry.targetDurationSeconds
                            ),
                    )
                }
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

    fun setMinutes(deviceId: Long, minutes: Int) {
        val clamped = minutes.coerceIn(1, MAX_DURATION_MINUTES)
        drafts.update { it + (deviceId to (it[deviceId] ?: Draft()).copy(minutes = clamped)) }
    }

    fun completeDevice(item: StrengthItem) = logSets(item, item.openSets)

    fun completeOneSet(item: StrengthItem) = logSets(item, 1)

    fun undoLastSet(item: StrengthItem) {
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

    fun logSession(item: CardioItem) {
        val session = CardioLog(
            deviceId = item.deviceId,
            performedAt = System.currentTimeMillis(),
            durationSeconds = item.minutes * SECONDS_PER_MINUTE,
        )
        viewModelScope.launch {
            cardioLogDao.insert(session)
            // The logged value is now the device's last session, so the draft is no longer needed.
            drafts.update { it - item.deviceId }
        }
    }

    fun undoLastSession(item: CardioItem) {
        // Pin the displayed duration so removing the session does not change it.
        drafts.update { it + (item.deviceId to Draft(minutes = item.minutes)) }
        viewModelScope.launch { cardioLogDao.deleteLastSince(item.deviceId, startOfToday) }
    }

    fun updateSession(session: CardioLog, minutes: Int) {
        val updated = session.copy(durationSeconds = minutes.coerceIn(1, MAX_DURATION_MINUTES) * SECONDS_PER_MINUTE)
        viewModelScope.launch { cardioLogDao.update(updated) }
    }

    fun deleteSession(session: CardioLog) {
        viewModelScope.launch { cardioLogDao.delete(session) }
    }

    private fun logSets(item: StrengthItem, count: Int) {
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
                WorkoutViewModel(
                    planId,
                    app.database.planDao(),
                    app.database.setLogDao(),
                    app.database.cardioLogDao(),
                )
            }
        }
    }
}
