package io.github.parcival42.workouttracker.ui.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.parcival42.workouttracker.WorkoutTrackerApplication
import io.github.parcival42.workouttracker.data.BodyweightMeasure
import io.github.parcival42.workouttracker.data.CardioLog
import io.github.parcival42.workouttracker.data.CircuitSettings
import io.github.parcival42.workouttracker.data.CardioLogDao
import io.github.parcival42.workouttracker.data.DeviceType
import io.github.parcival42.workouttracker.data.PlanDao
import io.github.parcival42.workouttracker.data.PlanEntryItem
import io.github.parcival42.workouttracker.data.PlanType
import io.github.parcival42.workouttracker.data.SetLog
import io.github.parcival42.workouttracker.data.SetLogDao
import io.github.parcival42.workouttracker.data.circuitSettings
import io.github.parcival42.workouttracker.ui.circuit.CircuitController
import io.github.parcival42.workouttracker.ui.circuit.CircuitEvent
import io.github.parcival42.workouttracker.ui.circuit.CircuitPhase
import io.github.parcival42.workouttracker.ui.circuit.CircuitPreview
import io.github.parcival42.workouttracker.ui.circuit.CircuitStation
import io.github.parcival42.workouttracker.ui.circuit.CircuitUiState
import io.github.parcival42.workouttracker.ui.circuit.buildCircuitPhases
import io.github.parcival42.workouttracker.ui.components.MAX_DISTANCE_METERS
import io.github.parcival42.workouttracker.ui.components.MAX_DURATION_SECONDS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
    val hasSessionLogs: Boolean
    // Circuit plans only: the station time of this device; null in normal plans.
    val stationSeconds: Int?
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
    override val stationSeconds: Int? = null,
    // Bodyweight exercises: what counts; null for strength devices with a weight.
    val measure: BodyweightMeasure? = null,
    // Bodyweight measured by time: duration of the next set.
    val durationSeconds: Int = 0,
) : WorkoutItem {
    val isBodyweight: Boolean get() = measure != null
    // Circuit stations are timed: they log weight and duration, no reps.
    val isTimed: Boolean get() = stationSeconds != null
    val openSets: Int get() = (targetSets - todaySets.size).coerceAtLeast(0)
    override val isComplete: Boolean get() = openSets == 0
    override val hasSessionLogs: Boolean get() = todaySets.isNotEmpty()
}

data class CardioItem(
    override val deviceId: Long,
    override val deviceName: String,
    val todaySessions: List<CardioLog>,
    // Exact duration of the next session: the last one, or the plan target.
    val durationSeconds: Int,
    // One per round in a circuit plan, otherwise one.
    val targetSessions: Int = 1,
    override val stationSeconds: Int? = null,
    val tracksDistance: Boolean = false,
    // Distance of the next session: the last logged distance, or none. Always null without tracksDistance.
    val distanceMeters: Int? = null,
) : WorkoutItem {
    override val isComplete: Boolean get() = todaySessions.size >= targetSessions
    override val hasSessionLogs: Boolean get() = todaySessions.isNotEmpty()
}

data class WorkoutUiState(
    val planName: String = "",
    val items: List<WorkoutItem> = emptyList(),
    // Only set for circuit plans; normal plans show nothing of the circuit.
    val circuitPreview: CircuitPreview? = null,
    val circuit: CircuitUiState? = null,
) {
    val isComplete: Boolean get() = items.isNotEmpty() && items.all { it.isComplete }
}

/** Unsaved edits; null fields fall back to the last logged set or session (see [strengthLogs]). */
// A distance can be cleared on purpose, so [distanceSet] tells "cleared" from "not edited".
private data class Draft(
    val weightGrams: Int? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Int? = null,
    val distanceSet: Boolean = false,
)

// [lastDistance]: cardio only, the distance of the last session that has one.
private data class LogsByDevice<T>(
    val today: Map<Long, List<T>>,
    val last: Map<Long, T>,
    val lastDistance: Map<Long, Int> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val planId: Long,
    private val planDao: PlanDao,
    private val setLogDao: SetLogDao,
    private val cardioLogDao: CardioLogDao,
    // Monotonic clock for the circuit timer.
    private val clock: () -> Long = SystemClock::elapsedRealtime,
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
        cardioLogDao.observeLastDistancePerDeviceInPlan(planId),
        cardioLogDao.observeLastDistancePerDevice(),
    ) { today, lastInPlan, lastAnywhere, lastDistanceInPlan, lastDistanceAnywhere ->
        LogsByDevice(
            today = today.groupBy { it.deviceId },
            last = lastAnywhere.associateBy { it.deviceId } + lastInPlan.associateBy { it.deviceId },
            lastDistance = (lastDistanceAnywhere + lastDistanceInPlan)
                .mapNotNull { session -> session.distanceMeters?.let { session.deviceId to it } }
                .toMap(),
        )
    }

    // Cold, so the circuit reads live values even while no screen collects [uiState].
    private val workoutState: Flow<WorkoutUiState> = combine(
        plan,
        planDao.observeEntries(planId),
        strengthLogs,
        cardioLogs,
        drafts,
    ) { plan, entries, strengthLogs, cardioLogs, drafts ->
        val circuitSettings = plan?.takeIf { it.type == PlanType.CIRCUIT }?.circuitSettings
        WorkoutUiState(
            planName = plan?.name.orEmpty(),
            circuitPreview = circuitSettings?.let { settings ->
                CircuitPreview(
                    stations = entries.size,
                    rounds = settings.rounds,
                    totalSeconds = circuitPhases(circuitStations(entries, settings), settings).sumOf { it.durationSeconds },
                )
            },
            items = entries.map { entry ->
                val draft = drafts[entry.deviceId]
                when (entry.deviceType) {
                    DeviceType.STRENGTH, DeviceType.BODYWEIGHT -> {
                        val last = strengthLogs.last[entry.deviceId]
                        val isBodyweight = entry.deviceType == DeviceType.BODYWEIGHT
                        StrengthItem(
                            deviceId = entry.deviceId,
                            deviceName = entry.deviceName,
                            targetSets = circuitSettings?.rounds ?: entry.targetSets,
                            targetReps = entry.targetReps,
                            todaySets = strengthLogs.today[entry.deviceId].orEmpty(),
                            weightGrams = if (isBodyweight) 0 else draft?.weightGrams ?: last?.weightGrams ?: entry.minWeightGrams,
                            minWeightGrams = entry.minWeightGrams,
                            weightStepGrams = entry.weightStepGrams,
                            reps = draft?.reps ?: last?.reps ?: entry.targetReps,
                            stationSeconds = circuitSettings?.let { entry.stationSeconds ?: it.stationSeconds },
                            measure = entry.measure.takeIf { isBodyweight },
                            durationSeconds = draft?.durationSeconds ?: last?.durationSeconds ?: entry.targetDurationSeconds,
                        )
                    }
                    DeviceType.CARDIO -> CardioItem(
                        deviceId = entry.deviceId,
                        deviceName = entry.deviceName,
                        todaySessions = cardioLogs.today[entry.deviceId].orEmpty(),
                        durationSeconds = draft?.durationSeconds
                            ?: cardioLogs.last[entry.deviceId]?.durationSeconds
                            ?: entry.targetDurationSeconds,
                        targetSessions = circuitSettings?.rounds ?: 1,
                        stationSeconds = circuitSettings?.let { entry.stationSeconds ?: it.stationSeconds },
                        tracksDistance = entry.tracksDistance,
                        distanceMeters = when {
                            !entry.tracksDistance -> null
                            draft?.distanceSet == true -> draft.distanceMeters
                            else -> cardioLogs.lastDistance[entry.deviceId]
                        },
                    )
                }
            },
        )
    }

    // Only started for circuit plans.
    private val circuit = CircuitController(viewModelScope, clock, onStationDone = ::logStation)
    private var circuitStarting = false

    val circuitEvents: SharedFlow<CircuitEvent> get() = circuit.events

    val uiState: StateFlow<WorkoutUiState> = combine(workoutState, circuit.state) { state, circuitState ->
        state.copy(circuit = circuitState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    fun setWeight(deviceId: Long, weightGrams: Int) {
        val clamped = weightGrams.coerceIn(0, MAX_WEIGHT_GRAMS)
        drafts.update { it + (deviceId to (it[deviceId] ?: Draft()).copy(weightGrams = clamped)) }
    }

    fun setReps(deviceId: Long, reps: Int) {
        val clamped = reps.coerceIn(1, MAX_REPS)
        drafts.update { it + (deviceId to (it[deviceId] ?: Draft()).copy(reps = clamped)) }
    }

    fun setDuration(deviceId: Long, seconds: Int) {
        val clamped = seconds.coerceIn(1, MAX_DURATION_SECONDS)
        drafts.update { it + (deviceId to (it[deviceId] ?: Draft()).copy(durationSeconds = clamped)) }
    }

    /** Null clears the distance, so the next session is logged without one. */
    fun setDistance(deviceId: Long, meters: Int?) {
        val clamped = meters?.coerceIn(1, MAX_DISTANCE_METERS)
        drafts.update { it + (deviceId to (it[deviceId] ?: Draft()).copy(distanceMeters = clamped, distanceSet = true)) }
    }

    fun completeDevice(item: StrengthItem) = logSets(item, item.openSets)

    fun completeOneSet(item: StrengthItem) = logSets(item, 1)

    fun undoLastSet(item: StrengthItem) {
        // Pin the displayed values so removing the set does not change the selected weight.
        drafts.update {
            it + (item.deviceId to Draft(weightGrams = item.weightGrams, reps = item.reps, durationSeconds = item.durationSeconds))
        }
        viewModelScope.launch { setLogDao.deleteLastSinceInPlan(item.deviceId, planId, sessionStart.first()) }
    }

    fun updateSet(set: SetLog, weightGrams: Int, reps: Int?, durationSeconds: Int?) {
        val updated = set.copy(
            weightGrams = weightGrams.coerceIn(0, MAX_WEIGHT_GRAMS),
            reps = reps?.coerceIn(1, MAX_REPS),
            durationSeconds = durationSeconds?.coerceIn(1, MAX_DURATION_SECONDS),
        )
        viewModelScope.launch { setLogDao.update(updated) }
    }

    fun deleteSet(set: SetLog) {
        viewModelScope.launch { setLogDao.delete(set) }
    }

    /** [onLogged] gets the stored session (with its id), e.g. to offer correcting the distance. */
    fun logSession(item: CardioItem, onLogged: (CardioLog) -> Unit = {}) {
        val session = CardioLog(
            deviceId = item.deviceId,
            performedAt = System.currentTimeMillis(),
            durationSeconds = item.durationSeconds,
            planId = planId,
            distanceMeters = item.distanceMeters.takeIf { item.tracksDistance },
        )
        viewModelScope.launch {
            val id = cardioLogDao.insert(session)
            // The logged value is now the device's last session, so the draft is no longer needed.
            drafts.update { it - item.deviceId }
            onLogged(session.copy(id = id))
        }
    }

    fun undoLastSession(item: CardioItem) {
        // Pin the displayed values so removing the session does not change them.
        drafts.update {
            it + (item.deviceId to Draft(durationSeconds = item.durationSeconds, distanceMeters = item.distanceMeters, distanceSet = true))
        }
        viewModelScope.launch { cardioLogDao.deleteLastSinceInPlan(item.deviceId, planId, sessionStart.first()) }
    }

    fun updateSession(session: CardioLog, seconds: Int, distanceMeters: Int?) {
        val updated = session.copy(
            durationSeconds = seconds.coerceIn(1, MAX_DURATION_SECONDS),
            distanceMeters = distanceMeters?.coerceIn(1, MAX_DISTANCE_METERS),
        )
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

    fun startCircuit() {
        if (circuit.isRunning || circuitStarting) return
        circuitStarting = true
        viewModelScope.launch {
            try {
                val settings = plan.first()?.takeIf { it.type == PlanType.CIRCUIT }?.circuitSettings ?: return@launch
                val stations = circuitStations(planDao.observeEntries(planId).first(), settings)
                if (stations.isEmpty()) return@launch
                // A further circuit on the same day starts a new session, so its progress counts from zero.
                if (workoutState.first().items.any { it.hasSessionLogs }) {
                    planDao.setSessionStart(planId, System.currentTimeMillis())
                }
                circuit.start(stations, settings.rounds, settings.restSeconds, settings.countdownSeconds)
            } finally {
                circuitStarting = false
            }
        }
    }

    /** Saves a station time in the plan; a running circuit uses it from the station's next pass. */
    fun setStationSeconds(deviceId: Long, seconds: Int) {
        viewModelScope.launch {
            val settings = plan.first()?.takeIf { it.type == PlanType.CIRCUIT }?.circuitSettings ?: return@launch
            val entry = planDao.observeEntries(planId).first().firstOrNull { it.deviceId == deviceId } ?: return@launch
            // The plan's time is stored as "no own time", so the station follows later plan changes.
            planDao.updateStationSeconds(entry.id, seconds.takeIf { it != settings.stationSeconds })
            circuit.updateStationSeconds(deviceId, seconds)
        }
    }

    fun pauseCircuit() = circuit.pause()

    fun resumeCircuit() = circuit.resume()

    /** Ends the current phase early; a skipped station is not logged. */
    fun skipCircuitPhase() = circuit.skip()

    /** Ends a running circuit or closes a finished one; logged stations stay. */
    fun stopCircuit() = circuit.stop()

    // Logs a finished station: the weight the card shows right now (last set, or the user's
    // edit) and the station time. Reps can be added later through the set chip.
    private suspend fun logStation(station: CircuitStation, seconds: Int) {
        when (val item = workoutState.first().items.firstOrNull { it.deviceId == station.deviceId }) {
            is StrengthItem -> {
                setLogDao.insertAll(
                    listOf(
                        SetLog(
                            deviceId = item.deviceId,
                            performedAt = System.currentTimeMillis(),
                            weightGrams = item.weightGrams,
                            reps = null,
                            planId = planId,
                            durationSeconds = seconds,
                        )
                    )
                )
                drafts.update { it - item.deviceId }
            }
            is CardioItem -> {
                cardioLogDao.insert(
                    CardioLog(
                        deviceId = item.deviceId,
                        performedAt = System.currentTimeMillis(),
                        durationSeconds = seconds,
                        planId = planId,
                    )
                )
                drafts.update { it - item.deviceId }
            }
            null -> Unit
        }
    }

    private fun logSets(item: StrengthItem, count: Int) {
        if (count <= 0) return
        val now = System.currentTimeMillis()
        // Offset by 1 ms per set so sets logged in one tap keep a stable order.
        val timed = item.measure == BodyweightMeasure.TIME
        val sets = List(count) { index ->
            SetLog(
                deviceId = item.deviceId,
                performedAt = now + index,
                weightGrams = item.weightGrams,
                reps = if (timed) null else item.reps,
                planId = planId,
                durationSeconds = if (timed) item.durationSeconds else null,
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

private fun circuitStations(entries: List<PlanEntryItem>, settings: CircuitSettings): List<CircuitStation> =
    entries.map { CircuitStation(it.deviceId, it.deviceName, it.stationSeconds ?: settings.stationSeconds) }

private fun circuitPhases(stations: List<CircuitStation>, settings: CircuitSettings): List<CircuitPhase> =
    buildCircuitPhases(
        stationSeconds = stations.map { it.seconds },
        rounds = settings.rounds,
        restSeconds = settings.restSeconds,
        countdownSeconds = settings.countdownSeconds,
    )
