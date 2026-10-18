package io.github.parcival42.workouttracker.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.parcival42.workouttracker.WorkoutTrackerApplication
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.CardioLogDao
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.PlanDao
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.SetLogDao
import io.github.parcival42.workouttracker.ui.components.MAX_DURATION_MINUTES
import io.github.parcival42.workouttracker.ui.components.SECONDS_PER_MINUTE
import io.github.parcival42.workouttracker.ui.components.secondsToMinutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    val items: List<WorkoutItem> = emptyList(),
) {
    val isComplete: Boolean get() = items.isNotEmpty() && items.all { it.isComplete }
}

/** Unsaved edits; null fields fall back to the last logged set or session (see [strengthLogs]). */
private data class Draft(val weightGrams: Int? = null, val reps: Int? = null, val minutes: Int? = null)

private data class LogsByDevice<T>(val today: Map<Long, List<T>>, val last: Map<Long, T>)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val planId: Long,
    private val planDao: PlanDao,
    private val setLogDao: SetLogDao,
    private val cardioLogDao: CardioLogDao,
) : ViewModel() {

    // The set counter resets per calendar day (or on a restart); "today" is fixed when the screen opens.
    private val startOfToday: Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val drafts = MutableStateFlow<Map<Long, Draft>>(emptyMap())

    private val plan = planDao.observePlan(planId)

    // Sets count from the start of the current session: midnight, or a later restart of the plan.
    private val sessionStart = plan
        .map { maxOf(startOfToday, it?.sessionStartedAt ?: 0L) }
        .distinctUntilChanged()

    // Session start before the last restart, for undoing the restart.
    private var previousSessionStartedAt: Long? = null

    // The session's logs count per plan. The prefill comes from the device's last log in this
    // plan, falling back to its last log in any plan (or without one).
    private val strengthLogs = combine(
        sessionStart.flatMapLatest { since -> setLogDao.observeSinceInPlan(since, planId) },
        setLogDao.observeLastSetPerDeviceInPlan(planId),
        setLogDao.observeLastSetPerDevice(),
    ) { today, lastInPlan, lastAnywhere ->
        LogsByDevice(
            today = today.groupBy { it.deviceId },
            last = lastAnywhere.associateBy { it.deviceId } + lastInPlan.associateBy { it.deviceId },
        )
    }

    private val cardioLogs = combine(
        sessionStart.flatMapLatest { since -> cardioLogDao.observeSinceInPlan(since, planId) },
        cardioLogDao.observeLastPerDeviceInPlan(planId),
        cardioLogDao.observeLastPerDevice(),
    ) { today, lastInPlan, lastAnywhere ->
        LogsByDevice(
            today = today.groupBy { it.deviceId },
            last = lastAnywhere.associateBy { it.deviceId } + lastInPlan.associateBy { it.deviceId },
        )
    }

    val uiState: StateFlow<WorkoutUiState> = combine(
        plan,
        planDao.observeEntries(planId),
        strengthLogs,
        cardioLogs,
        drafts,
    ) { plan, entries, strengthLogs, cardioLogs, drafts ->
        WorkoutUiState(
            planName = plan?.name.orEmpty(),
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
        viewModelScope.launch { setLogDao.deleteLastSinceInPlan(item.deviceId, planId, sessionStart.first()) }
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
            planId = planId,
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
        viewModelScope.launch { cardioLogDao.deleteLastSinceInPlan(item.deviceId, planId, sessionStart.first()) }
    }

    fun updateSession(session: CardioLog, minutes: Int) {
        val updated = session.copy(durationSeconds = minutes.coerceIn(1, MAX_DURATION_MINUTES) * SECONDS_PER_MINUTE)
        viewModelScope.launch { cardioLogDao.update(updated) }
    }

    fun deleteSession(session: CardioLog) {
        viewModelScope.launch { cardioLogDao.delete(session) }
    }

    /** Starts the plan over: only sets logged from now on count for today's progress. */
    fun restartPlan() {
        viewModelScope.launch {
            previousSessionStartedAt = plan.first()?.sessionStartedAt
            planDao.setSessionStart(planId, System.currentTimeMillis())
        }
    }

    fun undoRestartPlan() {
        viewModelScope.launch { planDao.setSessionStart(planId, previousSessionStartedAt) }
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
                planId = planId,
            )
        }
        viewModelScope.launch {
            setLogDao.insertAll(sets)
            // The logged values are now the device's last set, so the draft is no longer needed.
            drafts.update { it - item.deviceId }
        }
    }

    companion object {
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
